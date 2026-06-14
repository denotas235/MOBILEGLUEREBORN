// MobileGlues - gl/shader_binary_cache.cpp
// Phase 3: Shader Program Binary Cache to eliminate compilation stutters (stuttering)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_binary_cache.h"
#include "../gles/loader.h"
#include "log.h"
#include "../config/settings.h"
#include <unordered_map>
#include <vector>
#include <fstream>
#include <sstream>
#include <sys/stat.h>
#include <sys/types.h>
#include <errno.h>

std::unordered_map<GLuint, std::string> g_shader_sources;

// 64-bit FNV-1a hash
static uint64_t fnv1a_64(const std::string& str) {
    uint64_t hash = 0xcbf29ce484222325ULL;
    for (char c : str) {
        hash ^= static_cast<uint64_t>(static_cast<unsigned char>(c));
        hash *= 0x00000100000001B3ULL;
    }
    return hash;
}

static std::string get_cache_file_path(GLuint program) {
    if (!global_settings.gpu_optimizations.shader_binary_cache.enabled) return "";

    // Obter shaders anexados ao program
    GLint count = 0;
    GLES.glGetProgramiv(program, GL_ATTACHED_SHADERS, &count);
    if (count <= 0) return "";

    std::vector<GLuint> shaders(count);
    GLES.glGetAttachedShaders(program, count, nullptr, shaders.data());

    std::string vs_source, fs_source;
    for (GLuint shader : shaders) {
        GLint type = 0;
        GLES.glGetShaderiv(shader, GL_SHADER_TYPE, &type);
        auto it = g_shader_sources.find(shader);
        if (it != g_shader_sources.end()) {
            if (type == GL_VERTEX_SHADER) {
                vs_source = it->second;
            } else if (type == GL_FRAGMENT_SHADER) {
                fs_source = it->second;
            }
        }
    }

    if (vs_source.empty() || fs_source.empty()) {
        return "";
    }

    std::string program_key = vs_source + "|" + fs_source;
    uint64_t hash = fnv1a_64(program_key);

    std::ostringstream ss;
    ss << global_settings.gpu_optimizations.shader_binary_cache.cache_path << "/";
    ss << std::hex << hash << ".bin";
    return ss.str();
}

void shader_binary_cache_init() {
    if (!global_settings.gpu_optimizations.shader_binary_cache.enabled) return;

    std::string cache_dir = global_settings.gpu_optimizations.shader_binary_cache.cache_path;

    // Garantir criação recursiva da pasta de cache
    mkdir("/sdcard/MG", 0755);
    mkdir(cache_dir.c_str(), 0755);
    LOG_I("[ShaderCache] Initialized at: %s", cache_dir.c_str());
}

bool shader_binary_cache_load(GLuint program) {
    if (!global_settings.gpu_optimizations.shader_binary_cache.enabled) return false;

    std::string path = get_cache_file_path(program);
    if (path.empty()) return false;

    std::ifstream file(path, std::ios::binary);
    if (!file) return false;

    GLenum format = 0;
    GLsizei length = 0;

    file.read(reinterpret_cast<char*>(&format), sizeof(format));
    file.read(reinterpret_cast<char*>(&length), sizeof(length));

    if (length <= 0) return false;

    std::vector<char> buffer(length);
    file.read(buffer.data(), length);

    LOG_I("[ShaderCache] Loading binary program from cache: %s (size: %d bytes)", path.c_str(), length);
    GLES.glProgramBinary(program, format, buffer.data(), length);

    GLint link_status = 0;
    GLES.glGetProgramiv(program, GL_LINK_STATUS, &link_status);
    if (link_status == GL_TRUE) {
        LOG_I("[ShaderCache] Loaded program binary successfully!");
        return true;
    }

    LOG_W("[ShaderCache] Failed to load program binary (incompatible format/driver update). File discarded.");
    file.close();
    remove(path.c_str());
    return false;
}

void shader_binary_cache_save(GLuint program) {
    if (!global_settings.gpu_optimizations.shader_binary_cache.enabled) return;

    GLint link_status = 0;
    GLES.glGetProgramiv(program, GL_LINK_STATUS, &link_status);
    if (link_status != GL_TRUE) return;

    std::string path = get_cache_file_path(program);
    if (path.empty()) return;

    // Se já existe, não precisa salvar novamente
    struct stat st;
    if (stat(path.c_str(), &st) == 0) return;

    GLint length = 0;
    GLES.glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH, &length);
    if (length <= 0) return;

    std::vector<char> buffer(length);
    GLenum format = 0;
    GLsizei written = 0;
    GLES.glGetProgramBinary(program, length, &written, &format, buffer.data());

    std::ofstream file(path, std::ios::binary);
    if (!file) {
        LOG_E("[ShaderCache] Failed to open cache file for writing: %s", path.c_str());
        return;
    }

    file.write(reinterpret_cast<const char*>(&format), sizeof(format));
    file.write(reinterpret_cast<const char*>(&written), sizeof(written));
    file.write(buffer.data(), written);

    LOG_I("[ShaderCache] Saved program binary to cache: %s (size: %d bytes)", path.c_str(), written);
}
