// MobileGlues - gl/shader.cpp
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only
// End of Source File Header

#include <cctype>
#include "shader.h"
#include "shader_patcher.h"

#include <GL/gl.h>
#include "log.h"
#include "program.h"
#include "../gles/loader.h"
#include "../includes.h"
#include "glsl/glsl_for_es.h"
#include "../config/settings.h"
#include "FSR1/FSR1.h"
#include "phase2_lighting.h"
#include "mali_sorter.h"

#define DEBUG 0

#include <unordered_map>
extern std::unordered_map<GLuint, std::string> g_shader_sources;

struct shader_t shaderInfo;

UnorderedMap<GLuint, bool> shader_map_is_sampler_buffer_emulated;
UnorderedMap<GLuint, bool> shader_map_is_atomic_counter_emulated;

bool can_run_essl3(unsigned int esversion, const char* glsl) {
    if (strncmp(glsl, "#version 100", 12) == 0) {
        return true;
    }

    unsigned int glsl_version = 0;
    if (strncmp(glsl, "#version 300 es", 15) == 0) {
        glsl_version = 300;
    } else if (strncmp(glsl, "#version 310 es", 15) == 0) {
        glsl_version = 310;
    } else if (strncmp(glsl, "#version 320 es", 15) == 0) {
        glsl_version = 320;
    } else {
        return false;
    }
    return esversion >= glsl_version;
}

bool is_direct_shader(const char* glsl) {
    bool es3_ability = can_run_essl3(hardware->es_version, glsl);
    return es3_ability;
}

bool check_if_sampler_buffer_used(std::string str) {
    return str.find("samplerBuffer") != std::string::npos;
}

void glShaderSource(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length) {
    LOG()
    shaderInfo.id = 0;
    shaderInfo.converted = "";
    shaderInfo.frag_data_changed = 0;
    size_t l = 0;
    for (int i = 0; i < count; i++)
        l += (length && length[i] >= 0) ? length[i] : strlen(string[i]);
    std::string glsl_src, essl_src;
    glsl_src.reserve(l + 1);
    if (length) {
        for (int i = 0; i < count; i++) {
            if (length[i] >= 0)
                glsl_src += std::string_view(string[i], length[i]);
            else
                glsl_src += string[i];
        }
    } else {
        for (int i = 0; i < count; i++) {
            glsl_src += string[i];
        }
    }

    // --- Aplicar patch direto no shader antes de qualquer conversão ---
    GLint shaderType;
    GLES.glGetShaderiv(shader, GL_SHADER_TYPE, &shaderType);
    std::string patched_src = patch_shader_source(glsl_src.c_str(), shaderType);
    // ----------------------------------------------------------------------

    bool is_sampler_buffer_emulated = hardware->emulate_texture_buffer && check_if_sampler_buffer_used(patched_src);

    if (is_direct_shader(patched_src.c_str())) {
        LOG_D("[INFO] [Shader] Direct shader source: ")
        LOG_D("%s", patched_src.c_str())
        essl_src = patched_src;
    } else {
        int glsl_version = getGLSLVersion(patched_src.c_str());
        LOG_D("[INFO] [Shader] Shader source: ")
        LOG_D("%s", patched_src.c_str())
        int return_code = 0;
        essl_src = GLSLtoGLSLES(patched_src.c_str(), shaderType, hardware->es_version, glsl_version, return_code);
        if (return_code == 1) { // atomicCounterEmulated
            shader_map_is_atomic_counter_emulated[shader] = true;
            LOG_D("[INFO] [Shader] Atomic counter emulated in shader %d", shader)
        }

        if (essl_src.empty()) {
            LOG_E("Failed to convert shader %d.", shader)
            return;
        }
        LOG_D("\n[INFO] [Shader] Converted Shader source: \n%s", essl_src.c_str())
    }
    if (!essl_src.empty()) {
        GLint shaderType = 0;
        GLES.glGetShaderiv(shader, GL_SHADER_TYPE, &shaderType);
        bool is_fragment = (shaderType == GL_FRAGMENT_SHADER);

        // Injeta otimizações da Fase 2 (PLS e Framebuffer Fetch)
        essl_src = phase2_inject_pls(essl_src, is_fragment);
        if (is_fragment) {
            essl_src = phase2_inject_fbfetch(essl_src);
        }
        
        // Injeta otimização da Fase 4 (AtmosV-Alpha Fog)
        essl_src = mali_sorter_inject_fog(essl_src, is_fragment);

        // --- Lexical Shader Transpiler (Fase 5 - Engine Fix) ---
        if (is_fragment) {
            // Sempre injeta bloco completo de precisões logo após o #version
            // O GLSL 3.30+ do Minecraft não declara precisões, obrigatório no ESSL 3.0
            static const std::string precision_block =
                "precision highp float;\n"
                "precision highp int;\n"
                "precision mediump sampler2D;\n"
                "precision mediump sampler3D;\n"
                "precision mediump samplerCube;\n"
                "precision mediump sampler2DShadow;\n"
                "precision mediump sampler2DArray;\n";

            size_t ver_pos = essl_src.find("#version");
            size_t inject_pos = std::string::npos;
            if (ver_pos != std::string::npos) {
                inject_pos = essl_src.find('\n', ver_pos);
                if (inject_pos != std::string::npos) inject_pos += 1;
            }

            // Remove precisões duplicadas antigas se existirem antes de reinjetar
            size_t old_prec = essl_src.find("precision mediump float;");
            if (old_prec != std::string::npos) {
                size_t old_end = essl_src.find('\n', old_prec);
                if (old_end != std::string::npos) essl_src.erase(old_prec, old_end - old_prec + 1);
                // Recalcula inject_pos após remoção
                ver_pos = essl_src.find("#version");
                inject_pos = std::string::npos;
                if (ver_pos != std::string::npos) {
                    inject_pos = essl_src.find('\n', ver_pos);
                    if (inject_pos != std::string::npos) inject_pos += 1;
                }
            }

            if (inject_pos != std::string::npos) {
                essl_src.insert(inject_pos, precision_block);
            } else {
                essl_src = precision_block + essl_src;
            }

            // Substitui gl_FragColor obsoleto (banido no ESSL 3.0)
            if (essl_src.find("gl_FragColor") != std::string::npos) {
                static const std::string frag_out_decl =
                    "layout(location = 0) out vec4 _mg_FragColor;\n"
                    "#define gl_FragColor _mg_FragColor\n";
                ver_pos = essl_src.find("#version");
                if (ver_pos != std::string::npos) {
                    size_t end_line = essl_src.find('\n', ver_pos);
                    if (end_line != std::string::npos) essl_src.insert(end_line + 1, frag_out_decl);
                } else {
                    essl_src = frag_out_decl + essl_src;
                }
            }
        }

        // Armazena a fonte do shader para a Fase 3 (Shader Binary Cache)
        g_shader_sources[shader] = essl_src;

        shaderInfo.id = shader;
        shaderInfo.converted = essl_src;
        const char* s[] = {essl_src.c_str()};
        GLES.glShaderSource(shader, count, s, nullptr);
        if (hardware->emulate_texture_buffer)
            shader_map_is_sampler_buffer_emulated[shader] = is_sampler_buffer_emulated;
    } else
        LOG_E("Failed to convert glsl.")
    CHECK_GL_ERROR
}

void glGetShaderiv(GLuint shader, GLenum pname, GLint* params) {
    LOG()
    GLES.glGetShaderiv(shader, pname, params);
    if (global_settings.ignore_error >= IgnoreErrorLevel::Partial && pname == GL_COMPILE_STATUS && !*params) {
        GLchar infoLog[512];
        GLES.glGetShaderInfoLog(shader, 512, nullptr, infoLog);
        LOG_W_FORCE("Shader %d compilation failed: \n%s", shader, infoLog)
        LOG_W_FORCE("Now try to cheat.")
        *params = GL_TRUE;
    }
    CHECK_GL_ERROR
}

GLuint glCreateShader(GLenum shaderType) {
    if (global_settings.fsr1_setting != FSR1_Quality_Preset::Disabled && !fsrInitialized) {
        InitFSRResources();
    }

    LOG()
    LOG_D("glCreateShader(%s)", glEnumToString(shaderType))
    GLuint shader = GLES.glCreateShader(shaderType);
    if (shader != 0 && hardware->emulate_texture_buffer) shader_map_is_sampler_buffer_emulated[shader] = false;
    CHECK_GL_ERROR
    return shader;
}

void glDeleteShader(GLuint shader) {
    LOG()
    LOG_D("glDeleteShader(%d)", shader)
    g_shader_sources.erase(shader);
    GLES.glDeleteShader(shader);
    CHECK_GL_ERROR
}

void glMaxShaderCompilerThreadsKHR(GLuint count) {
    LOG()
    LOG_D("glMaxShaderCompilerThreadsKHR(%u)", count)
    if (GLES.glMaxShaderCompilerThreadsKHR) {
        GLES.glMaxShaderCompilerThreadsKHR(count);
    } else {
        LOG_W("Driver does not support glMaxShaderCompilerThreadsKHR. Ignoring call.")
    }
}

void glMaxShaderCompilerThreadsARB(GLuint count) {
    LOG()
    LOG_D("glMaxShaderCompilerThreadsARB(%u)", count)
    if (GLES.glMaxShaderCompilerThreadsKHR) {
        GLES.glMaxShaderCompilerThreadsKHR(count);
    }
}