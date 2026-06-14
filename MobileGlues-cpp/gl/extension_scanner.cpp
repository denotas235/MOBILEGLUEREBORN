#include "extension_scanner.h"
#include "../gles/gles.h"
#include "../config/settings.h"
#define DEBUG 0
#include "log.h"
#include "mg.h"
#include <fstream>
#include <set>
#include <regex>
#include <sstream>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <android/log.h>
#include <cstring>
#include <algorithm>

static GPUCapabilities g_gpu_caps;
static bool g_scanner_initialized = false;

// 1. RUNTIME: Obter extensões via GL API oficial
void scan_runtime_extensions() {
    LOG_I("=== RUNTIME EXTENSION SCAN ===");

    // GL Extensions (moderno)
    GLint num_extensions = 0;
    GLES.glGetIntegerv(GL_NUM_EXTENSIONS, &num_extensions);

    for (GLint i = 0; i < num_extensions; ++i) {
        const char* ext = (const char*)GLES.glGetStringi(GL_EXTENSIONS, i);
        if (ext) {
            g_gpu_caps.gl_extensions.insert(std::string(ext));
            LOG_I("  GL: %s", ext);
        }
    }

    // Fallback (antigo)
    if (num_extensions == 0) {
        const char* ext_str = (const char*)GLES.glGetString(GL_EXTENSIONS);
        if (ext_str) {
            std::string exts(ext_str);
            std::istringstream iss(exts);
            std::string ext;
            while (iss >> ext) {
                g_gpu_caps.gl_extensions.insert(ext);
                LOG_I("  GL (fallback): %s", ext.c_str());
            }
        }
    }
}

// 2. BINARY SCANNING: Ler extensões direto dos .so do driver
void scan_system_binaries() {
    LOG_I("=== BINARY SCANNING ===");

    const char* scan_paths[] = {
        "/vendor/lib64/egl",
        "/vendor/lib64/hw",
        "/system/lib64/egl",
        "/system/lib64",
        nullptr
    };

    std::regex ext_pattern("^(GL_|EGL_|VK_)[A-Za-z0-9_]+$");
    std::set<std::string> found_extensions;

    for (int p = 0; scan_paths[p]; ++p) {
        DIR* dir = opendir(scan_paths[p]);
        if (!dir) continue;

        struct dirent* entry;
        while ((entry = readdir(dir)) != nullptr) {
            if (strstr(entry->d_name, ".so")) {
                std::string so_path = std::string(scan_paths[p]) + "/" + entry->d_name;

                // Ler o arquivo binário como stream de bytes
                std::ifstream file(so_path, std::ios::binary);
                if (!file) continue;

                std::string buffer((std::istreambuf_iterator<char>(file)), std::istreambuf_iterator<char>());

                // Extrair strings ASCII (chars 32-126)
                std::string current_str;
                for (unsigned char c : buffer) {
                    if (c >= 32 && c <= 126) {
                        current_str += c;
                    } else {
                        if (!current_str.empty()) {
                            // Verificar se é uma extensão válida (rfind with pos=0 checks prefix)
                            if (current_str.size() > 4 && 
                                (current_str.rfind("GL_", 0) == 0 ||
                                 current_str.rfind("EGL_", 0) == 0 ||
                                 current_str.rfind("VK_", 0) == 0)) {
                                if (std::regex_match(current_str, ext_pattern)) {
                                    found_extensions.insert(current_str);
                                }
                            }
                            current_str.clear();
                        }
                    }
                }

                LOG_I("  Scanned: %s (%zu extensions found)", entry->d_name, found_extensions.size());
            }
        }
        closedir(dir);
    }

    // Mesclar com GL extensions
    for (const auto& ext : found_extensions) {
        if (ext.find("GL_") == 0) {
            g_gpu_caps.gl_extensions.insert(ext);
        } else if (ext.find("EGL_") == 0) {
            g_gpu_caps.egl_extensions.insert(ext);
        } else if (ext.find("VK_") == 0) {
            g_gpu_caps.vk_extensions.insert(ext);
        }
    }
}

// 3. Detectar capabilities críticas
void detect_critical_capabilities() {
    LOG_I("=== CRITICAL CAPABILITY DETECTION ===");

    // ASTC
    g_gpu_caps.has_astc = (
        g_gpu_caps.gl_extensions.count("GL_KHR_texture_compression_astc_ldr") ||
        g_gpu_caps.gl_extensions.count("GL_OES_texture_compression_astc")
    );
    LOG_I("  ASTC Support: %s", g_gpu_caps.has_astc ? "YES" : "NO");

    // PLS (Pixel Local Storage)
    g_gpu_caps.has_pls = (
        g_gpu_caps.gl_extensions.count("GL_EXT_shader_pixel_local_storage")
    );
    LOG_I("  PLS Support: %s", g_gpu_caps.has_pls ? "YES" : "NO");

    // Framebuffer Fetch
    g_gpu_caps.has_fbfetch = (
        g_gpu_caps.gl_extensions.count("GL_ARM_shader_framebuffer_fetch") ||
        g_gpu_caps.gl_extensions.count("GL_ARM_shader_framebuffer_fetch_depth_stencil")
    );
    LOG_I("  Framebuffer Fetch: %s", g_gpu_caps.has_fbfetch ? "YES" : "NO");

    // Shader Binary
    g_gpu_caps.has_shader_binary = (
        g_gpu_caps.gl_extensions.count("GL_ARM_mali_program_binary") ||
        g_gpu_caps.gl_extensions.count("GL_ARB_get_program_binary")
    );
    LOG_I("  Shader Binary Cache: %s", g_gpu_caps.has_shader_binary ? "YES" : "NO");
}

// 4. Salvar resultado em cache
void save_extensions_cache() {
    if (!global_settings.gpu_optimizations.extension_scanner.cache_result) {
        return;
    }

    std::string cache_path = global_settings.gpu_optimizations.extension_scanner.cache_path;
    std::ofstream cache_file(cache_path);
    if (!cache_file) {
        LOG_E("Failed to open extension cache path: %s", cache_path.c_str());
        return;
    }

    cache_file << "{\n";
    cache_file << "  \"timestamp\": " << time(nullptr) << ",\n";
    cache_file << "  \"gl_extensions\": [\n";

    bool first = true;
    for (const auto& ext : g_gpu_caps.gl_extensions) {
        if (!first) cache_file << ",\n";
        cache_file << "    \"" << ext << "\"";
        first = false;
    }

    cache_file << "\n  ],\n";
    cache_file << "  \"capabilities\": {\n";
    cache_file << "    \"astc\": " << (g_gpu_caps.has_astc ? "true" : "false") << ",\n";
    cache_file << "    \"pls\": " << (g_gpu_caps.has_pls ? "true" : "false") << ",\n";
    cache_file << "    \"fbfetch\": " << (g_gpu_caps.has_fbfetch ? "true" : "false") << ",\n";
    cache_file << "    \"shader_binary\": " << (g_gpu_caps.has_shader_binary ? "true" : "false") << "\n";
    cache_file << "  }\n";
    cache_file << "}\n";

    LOG_I("Extensions cache saved to %s", cache_path.c_str());
}

// MAIN INIT
void init_extension_scanner() {
    if (g_scanner_initialized) return;
    g_scanner_initialized = true;

    if (!global_settings.gpu_optimizations.extension_scanner.enabled) {
        return;
    }

    LOG_I("Starting GPU Extension Scanner...");

    scan_runtime_extensions();

    if (global_settings.gpu_optimizations.extension_scanner.scan_system_libs) {
        scan_system_binaries();
    }

    detect_critical_capabilities();
    save_extensions_cache();

    LOG_I("Extension scanner complete. Found %zu GL extensions",
          g_gpu_caps.gl_extensions.size());
}

// GETTERS
const GPUCapabilities& get_gpu_capabilities() {
    if (!g_scanner_initialized) {
        init_extension_scanner();
    }
    return g_gpu_caps;
}

bool has_extension(const char* ext_name) {
    if (!g_scanner_initialized) {
        init_extension_scanner();
    }
    return g_gpu_caps.gl_extensions.count(ext_name) > 0;
}
