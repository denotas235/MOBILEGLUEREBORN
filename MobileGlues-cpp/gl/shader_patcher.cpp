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
#include "shader_header_manager.h"

// Função para aplicar patches NO CORPO do shader (NÃO no cabeçalho)
std::string patch_shader_source(const char* original_source, GLenum shader_type) {
    std::string source(original_source);

    // 1. Normaliza o cabeçalho primeiro (garante que #version é a primeira linha)
    source = normalise_shader_header(source, shader_type, 320); // ES 3.2 para Mali-G52

    // 2. Aplica patches NO CORPO do shader (após o cabeçalho)
    size_t header_end = source.find("\n\n"); // Fim do cabeçalho
    if (header_end == std::string::npos) {
        header_end = source.find('\n');
        if (header_end != std::string::npos) {
            header_end = source.find('\n', header_end + 1);
        }
    }

    if (header_end != std::string::npos) {
        std::string header = source.substr(0, header_end + 1);
        std::string body = source.substr(header_end + 1);

        // 2.1. Vertex Shaders: Adiciona uniforms ausentes (apenas no corpo)
        if (shader_type == GL_VERTEX_SHADER) {
            if (body.find("ProjectionMatrix") != std::string::npos &&
                body.find("uniform mat4 ProjectionMatrix;") == std::string::npos) {
                body = "uniform mat4 ProjectionMatrix;\n" +
                       "uniform mat4 ModelViewMatrix;\n" +
                       "uniform mat4 SpriteMatrix;\n" +
                       "uniform float UPadding;\n" +
                       "uniform float VPadding;\n" + body;
            }

            // Substitui "varying" por "out" (apenas no corpo)
            body = std::regex_replace(body, std::regex("varying\\s+([a-zA-Z0-9_]+)"), "out $1");
        }

        // 2.2. Remove gl_ClipDistance (apenas no corpo)
        if (shader_type == GL_VERTEX_SHADER || shader_type == GL_FRAGMENT_SHADER) {
            body = std::regex_replace(body, std::regex("gl_ClipDistance\\[[0-9]+\\]"), "1.0");
        }

        // 2.3. Reconstroi o shader
        source = header + body;
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
