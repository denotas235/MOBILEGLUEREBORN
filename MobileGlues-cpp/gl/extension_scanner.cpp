// MobileGlues - gl/extension_scanner.cpp
// Varre extensões GL/EGL/VK de libs do sistema sem precisar de contexto GL.
// Também consulta o GL Context se disponível (runtime scan).
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

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

// ---------------------------------------------------------------------------
// Explicit library paths to scan — mirrors Sodium2ExtensionScanner.java
// Checked in order; missing paths are silently skipped.
// ---------------------------------------------------------------------------

static const char* GL_SYSTEM_LIBS[] = {
    "/vendor/lib64/egl/libGLES_mali.so",
    "/vendor/lib64/egl/libGLESv2.so",
    "/vendor/lib64/egl/libGLESv3.so",
    "/system/lib64/libGLESv2.so",
    "/system/lib64/libGLESv3.so",
    "/system/lib64/egl/libGLES_mali.so",
    "/system/lib64/egl/libGLESv2.so",
    "/system/lib64/egl/libGLESv3.so",
    nullptr
};

static const char* EGL_SYSTEM_LIBS[] = {
    "/vendor/lib64/egl/libEGL.so",
    "/system/lib64/libEGL.so",
    "/system/lib64/egl/libEGL.so",
    "/vendor/lib64/egl/libegl.so",
    "/system/lib64/egl/libegl.so",
    nullptr
};

static const char* VK_SYSTEM_LIBS[] = {
    "/vendor/lib64/hw/vulkan.mt6768.so",
    "/system/lib64/libvulkan.so",
    "/vendor/lib64/libvulkan.so",
    "/system/lib64/hw/vulkan.so",
    "/vendor/lib64/hw/vulkan.so",
    nullptr
};

// ---------------------------------------------------------------------------
// Helper: scan a single .so file for extension strings
// ---------------------------------------------------------------------------
static void scan_single_library(const char* path,
                                 std::set<std::string>& gl_out,
                                 std::set<std::string>& egl_out,
                                 std::set<std::string>& vk_out)
{
    struct stat st{};
    if (stat(path, &st) != 0) return; // file doesn't exist on this device

    std::ifstream file(path, std::ios::binary);
    if (!file) {
        LOG_W("extension_scanner: cannot open %s", path);
        return;
    }

    static const std::regex ext_pattern("^(GL_|EGL_|VK_)[A-Za-z0-9_]+$");
    size_t found_count = 0;

    std::string cur;
    cur.reserve(64);
    char c;
    while (file.get(c)) {
        unsigned char uc = static_cast<unsigned char>(c);
        if (uc >= 32 && uc <= 126) {
            cur += c;
        } else {
            if (cur.size() > 4) {
                if (cur[0] == 'G' && cur[1] == 'L' && cur[2] == '_') {
                    if (std::regex_match(cur, ext_pattern)) {
                        gl_out.insert(cur);
                        ++found_count;
                    }
                } else if (cur[0] == 'E' && cur[1] == 'G' && cur[2] == 'L' && cur[3] == '_') {
                    if (std::regex_match(cur, ext_pattern)) {
                        egl_out.insert(cur);
                        ++found_count;
                    }
                } else if (cur[0] == 'V' && cur[1] == 'K' && cur[2] == '_') {
                    if (std::regex_match(cur, ext_pattern)) {
                        vk_out.insert(cur);
                        ++found_count;
                    }
                }
            }
            cur.clear();
        }
    }

    LOG_I("  [BinScan] %s → %zu extensions", path, found_count);
}

// ---------------------------------------------------------------------------
// 1. BINARY SCAN: scan explicit lib paths + fallback directory walk
// Does NOT require a GL context — safe to call before EGL/GLES init.
// ---------------------------------------------------------------------------
static void scan_system_binaries() {
    LOG_I("=== BINARY SCAN: system GL/EGL/VK libraries ===");

    std::set<std::string> gl_found, egl_found, vk_found;

    // Scan explicit GL libs
    for (int i = 0; GL_SYSTEM_LIBS[i]; ++i)
        scan_single_library(GL_SYSTEM_LIBS[i], gl_found, egl_found, vk_found);

    // Scan explicit EGL libs
    for (int i = 0; EGL_SYSTEM_LIBS[i]; ++i)
        scan_single_library(EGL_SYSTEM_LIBS[i], gl_found, egl_found, vk_found);

    // Scan explicit Vulkan libs
    for (int i = 0; VK_SYSTEM_LIBS[i]; ++i)
        scan_single_library(VK_SYSTEM_LIBS[i], gl_found, egl_found, vk_found);

    // Fallback: also walk the standard directories to catch OEM-specific libs
    // (e.g. Adreno ships as libGLES_adreno.so under a non-standard name)
    if (gl_found.empty()) {
        LOG_I("  [BinScan] Explicit paths yielded 0 GL exts — falling back to directory walk");
        const char* fallback_dirs[] = {
            "/vendor/lib64/egl",
            "/vendor/lib64/hw",
            "/system/lib64/egl",
            "/system/lib64",
            nullptr
        };
        for (int d = 0; fallback_dirs[d]; ++d) {
            DIR* dir = opendir(fallback_dirs[d]);
            if (!dir) continue;
            struct dirent* entry;
            while ((entry = readdir(dir)) != nullptr) {
                if (strstr(entry->d_name, ".so")) {
                    std::string p = std::string(fallback_dirs[d]) + "/" + entry->d_name;
                    scan_single_library(p.c_str(), gl_found, egl_found, vk_found);
                }
            }
            closedir(dir);
        }
    }

    // Merge into global caps
    for (const auto& e : gl_found)  g_gpu_caps.gl_extensions.insert(e);
    for (const auto& e : egl_found) g_gpu_caps.egl_extensions.insert(e);
    for (const auto& e : vk_found)  g_gpu_caps.vk_extensions.insert(e);

    LOG_I("  [BinScan] Total: %zu GL, %zu EGL, %zu VK extensions found",
          gl_found.size(), egl_found.size(), vk_found.size());
}

// ---------------------------------------------------------------------------
// 2. RUNTIME SCAN: query the live GL context (only valid after eglMakeCurrent)
// Supplements binary scan with extensions the driver exposes at runtime.
// ---------------------------------------------------------------------------
static void scan_runtime_extensions() {
    LOG_I("=== RUNTIME GL EXTENSION SCAN ===");

    GLint num_extensions = 0;
    GLES.glGetIntegerv(GL_NUM_EXTENSIONS, &num_extensions);

    if (num_extensions > 0) {
        for (GLint i = 0; i < num_extensions; ++i) {
            const char* ext = reinterpret_cast<const char*>(
                GLES.glGetStringi(GL_EXTENSIONS, i));
            if (ext && ext[0]) {
                g_gpu_caps.gl_extensions.insert(ext);
            }
        }
        LOG_I("  [Runtime] %d extensions via glGetStringi", num_extensions);
    } else {
        // Fallback: old-style glGetString(GL_EXTENSIONS)
        const char* ext_str = reinterpret_cast<const char*>(
            GLES.glGetString(GL_EXTENSIONS));
        if (ext_str) {
            std::istringstream iss(ext_str);
            std::string ext;
            while (iss >> ext) {
                g_gpu_caps.gl_extensions.insert(ext);
            }
            LOG_I("  [Runtime] extensions via glGetString (legacy fallback)");
        }
    }
}

// ---------------------------------------------------------------------------
// 3. Detect critical capabilities from the merged extension set
// ---------------------------------------------------------------------------
static void detect_critical_capabilities() {
    LOG_I("=== CRITICAL CAPABILITY DETECTION ===");

    g_gpu_caps.has_astc = (
        g_gpu_caps.gl_extensions.count("GL_KHR_texture_compression_astc_ldr") ||
        g_gpu_caps.gl_extensions.count("GL_OES_texture_compression_astc")
    );
    LOG_I("  ASTC:           %s", g_gpu_caps.has_astc ? "YES" : "NO");

    g_gpu_caps.has_pls = (
        g_gpu_caps.gl_extensions.count("GL_EXT_shader_pixel_local_storage")
    );
    LOG_I("  PLS:            %s", g_gpu_caps.has_pls ? "YES" : "NO");

    g_gpu_caps.has_fbfetch = (
        g_gpu_caps.gl_extensions.count("GL_ARM_shader_framebuffer_fetch") ||
        g_gpu_caps.gl_extensions.count("GL_ARM_shader_framebuffer_fetch_depth_stencil")
    );
    LOG_I("  Framebuffer Fetch: %s", g_gpu_caps.has_fbfetch ? "YES" : "NO");

    g_gpu_caps.has_shader_binary = (
        g_gpu_caps.gl_extensions.count("GL_ARM_mali_program_binary") ||
        g_gpu_caps.gl_extensions.count("GL_ARB_get_program_binary")
    );
    LOG_I("  Shader Binary:  %s", g_gpu_caps.has_shader_binary ? "YES" : "NO");
}

// ---------------------------------------------------------------------------
// 4. Persist results to JSON cache
// ---------------------------------------------------------------------------
static void save_extensions_cache() {
    if (!global_settings.gpu_optimizations.extension_scanner.cache_result) return;

    const std::string& cache_path =
        global_settings.gpu_optimizations.extension_scanner.cache_path;
    std::ofstream f(cache_path);
    if (!f) {
        LOG_E("extension_scanner: cannot write cache to %s", cache_path.c_str());
        return;
    }

    f << "{\n  \"timestamp\": " << time(nullptr) << ",\n  \"gl_extensions\": [\n";
    bool first = true;
    for (const auto& e : g_gpu_caps.gl_extensions) {
        if (!first) f << ",\n";
        f << "    \"" << e << "\"";
        first = false;
    }
    f << "\n  ],\n  \"egl_extensions\": [\n";
    first = true;
    for (const auto& e : g_gpu_caps.egl_extensions) {
        if (!first) f << ",\n";
        f << "    \"" << e << "\"";
        first = false;
    }
    f << "\n  ],\n  \"vk_extensions\": [\n";
    first = true;
    for (const auto& e : g_gpu_caps.vk_extensions) {
        if (!first) f << ",\n";
        f << "    \"" << e << "\"";
        first = false;
    }
    f << "\n  ],\n  \"capabilities\": {\n";
    f << "    \"astc\": "          << (g_gpu_caps.has_astc          ? "true" : "false") << ",\n";
    f << "    \"pls\": "           << (g_gpu_caps.has_pls           ? "true" : "false") << ",\n";
    f << "    \"fbfetch\": "       << (g_gpu_caps.has_fbfetch       ? "true" : "false") << ",\n";
    f << "    \"shader_binary\": " << (g_gpu_caps.has_shader_binary ? "true" : "false") << "\n";
    f << "  }\n}\n";

    LOG_I("extension_scanner: cache saved → %s", cache_path.c_str());
}

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

void init_extension_scanner() {
    if (g_scanner_initialized) return;
    g_scanner_initialized = true;

    if (!global_settings.gpu_optimizations.extension_scanner.enabled) {
        LOG_I("extension_scanner: disabled in settings, skipping.");
        return;
    }

    LOG_I("=== MobileGlues Extension Scanner START ===");

    // Binary scan first — works without a GL context
    scan_system_binaries();

    // Runtime scan supplements binary results (requires active EGL context)
    if (global_settings.gpu_optimizations.extension_scanner.scan_system_libs) {
        scan_runtime_extensions();
    }

    detect_critical_capabilities();
    save_extensions_cache();

    LOG_I("=== Extension Scanner DONE: %zu GL | %zu EGL | %zu VK ===",
          g_gpu_caps.gl_extensions.size(),
          g_gpu_caps.egl_extensions.size(),
          g_gpu_caps.vk_extensions.size());
}

const GPUCapabilities& get_gpu_capabilities() {
    if (!g_scanner_initialized) init_extension_scanner();
    return g_gpu_caps;
}

bool has_extension(const char* ext_name) {
    if (!g_scanner_initialized) init_extension_scanner();
    return g_gpu_caps.gl_extensions.count(ext_name) > 0;
}
