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

// Funcao para aplicar patches NO CORPO do shader (NAO no cabecalho)
std::string patch_shader_source(const char* original_source, GLenum shader_type) {
    std::string source(original_source);

    // 1. Normaliza o cabecalho primeiro (garante que #version e a primeira linha)
    source = normalise_shader_header(source, shader_type, 320); // ES 3.2 para Mali-G52

    // 2. Aplica patches NO CORPO do shader (apos o cabecalho)
    size_t header_end = source.find("\n\n"); // Fim do cabecalho
    if (header_end == std::string::npos) {
        header_end = source.find('\n');
        if (header_end != std::string::npos) {
            header_end = source.find('\n', header_end + 1);
        }
    }

    if (header_end != std::string::npos) {
        std::string header = source.substr(0, header_end + 1);
        std::string body = source.substr(header_end + 1);

        // 2.1. Vertex Shaders: apenas a substituicao varying→out
        // NOTA: A injecao de uniforms (ProjectionMatrix, SpriteMatrix, etc.) foi
        // removida porque no MC 1.21.11 estes campos existem dentro de UBOs
        // (Uniform Buffer Objects / interface blocks). Injetar como uniforms
        // globais causava "redefinition of an interface block member name".
        if (shader_type == GL_VERTEX_SHADER) {
            // Substitui "varying" por "out" (obsoleto em GLES 3.0+)
            body = std::regex_replace(body, std::regex("varying\\s+([a-zA-Z0-9_]+)"), "out $1");
        }

        // 2.2. Remove gl_ClipDistance (nao suportado no GLES base 3.x sem extensao)
        if (shader_type == GL_VERTEX_SHADER || shader_type == GL_FRAGMENT_SHADER) {
            body = std::regex_replace(body, std::regex("gl_ClipDistance\\[[0-9]+\\]"), "1.0");
        }

        // 2.3. Reconstroi o shader
        source = header + body;
    }

    return source;
}

// Funcao wrapper para glShaderSource
void glass_glShaderSource(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length) {
    if (count <= 0 || !string) {
        return;
    }

    GLint shader_type;
    glGetShaderiv(shader, GL_SHADER_TYPE, &shader_type);

    std::string patched = patch_shader_source(string[0], shader_type);
    const char* patched_ptr = patched.c_str();
    GLint patched_length = static_cast<GLint>(patched.length());

    glShaderSource(shader, 1, &patched_ptr, &patched_length);
}
