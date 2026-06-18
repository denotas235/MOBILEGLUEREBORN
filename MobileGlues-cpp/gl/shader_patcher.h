// MobileGlues - Shader Patcher Header para Mali-G52
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef MOBILEGLUES_SHADER_PATCHER_H
#define MOBILEGLUES_SHADER_PATCHER_H

#include <string>
#include <GL/gl.h>

// Função para aplicar patches em shaders
std::string patch_shader_source(const char* original_source, GLenum shader_type);

// Função wrapper para glShaderSource
void glass_glShaderSource(GLuint shader, GLsizei count, const GLchar* const* string, const GLint* length);

#endif // MOBILEGLUES_SHADER_PATCHER_H
