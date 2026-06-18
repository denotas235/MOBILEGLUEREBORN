// MobileGlues - Shader Patcher para Mali-G52
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#include <string>
#include <vector>
#include <regex>
#include <cstring>
#include <GL/gl.h>
#include "log.h"

// Função para aplicar patches em shaders
std::string patch_shader_source(const char* original_source, GLenum shader_type) {
    std::string source(original_source);

    // 1. Adiciona precisão para Fragment Shaders (Mali-G52 suporta highp em fragment shaders)
    if (shader_type == GL_FRAGMENT_SHADER) {
        if (source.find("precision ") == std::string::npos) {
            size_t version_pos = source.find("#version");
            if (version_pos != std::string::npos) {
                size_t line_end = source.find('\n', version_pos);
                if (line_end != std::string::npos) {
                    // Injeta precisão alta para floats e texturas
                    source.insert(line_end + 1,
                        "\nprecision highp float;\n"
                        "precision highp int;\n"
                        "precision highp sampler2D;\n"
                        "precision highp sampler2DArray;\n"
                        "#extension GL_EXT_shader_framebuffer_fetch : enable\n"  // Habilita FBFetch para Mali
                    );
                }
            } else {
                source.insert(0,
                    "precision highp float;\n"
                    "precision highp int;\n"
                    "precision highp sampler2D;\n"
                    "precision highp sampler2DArray;\n"
                    "#extension GL_EXT_shader_framebuffer_fetch : enable\n"
                );
            }
        }
    }

    // 2. Correção para Vertex Shaders (matrizes e uniforms comuns no Minecraft)
    if (shader_type == GL_VERTEX_SHADER) {
        // Verifica se o shader usa ProjectionMatrix (comum no Minecraft)
        if (source.find("ProjectionMatrix") != std::string::npos &&
            source.find("uniform mat4 ProjectionMatrix;") == std::string::npos) {
            size_t version_pos = source.find("#version");
            if (version_pos != std::string::npos) {
                size_t line_end = source.find('\n', version_pos);
                source.insert(line_end + 1,
                    "\nprecision highp float;\n"
                    "uniform mat4 ProjectionMatrix;\n"
                    "uniform mat4 ModelViewMatrix;\n"
                    "uniform mat4 SpriteMatrix;\n"
                    "uniform float UPadding;\n"
                    "uniform float VPadding;\n"
                );
            }
        }

        // Remove "varying" (depreciado em GLES 3.0+) e substitui por "out"
        source = std::regex_replace(source, std::regex("varying\\s+([a-zA-Z0-9_]+)"), "out $1");
    }

    // 3. Remove instruções não suportadas em GLES 3.2 (ex.: gl_ClipDistance)
    if (shader_type == GL_VERTEX_SHADER || shader_type == GL_FRAGMENT_SHADER) {
        source = std::regex_replace(source, std::regex("gl_ClipDistance\\[[0-9]+\\]"), "1.0");
    }

    // 4. Adiciona extensões específicas para Mali-G52
    if (source.find("#extension GL_EXT_texture_compression_astc") == std::string::npos) {
        source.insert(0, "#extension GL_EXT_texture_compression_astc : enable\n");
    }

    return source;
}

// Função wrapper para glShaderSource
void glass_glShaderSource(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length) {
    if (count <= 0 || !string) {
        return; // Evita crashes
    }

    // Obtém o tipo do shader
    GLint shader_type;
    glGetShaderiv(shader, GL_SHADER_TYPE, &shader_type);

    // Aplica o patch no primeiro string (assumindo que o shader é uma string única)
    std::string patched = patch_shader_source(string[0], shader_type);
    const char* patched_ptr = patched.c_str();
    GLint patched_length = static_cast<GLint>(patched.length());

    // Chama a função nativa do GLES (ou do driver da Mali)
    glShaderSource(shader, 1, &patched_ptr, &patched_length);
}