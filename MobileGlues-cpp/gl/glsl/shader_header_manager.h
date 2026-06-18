// MobileGlues - Shader Header Manager
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef MOBILEGLUES_SHADER_HEADER_MANAGER_H
#define MOBILEGLUES_SHADER_HEADER_MANAGER_H

#include <string>
#include <GL/gl.h>

// Normaliza o cabeçalho do shader para GLSL ES
// Garante que #version é a primeira linha não-comentada,
// seguida por #extension e precision.
std::string normalise_shader_header(std::string source, GLenum shader_type, unsigned int target_es_version = 320);

#endif // MOBILEGLUES_SHADER_HEADER_MANAGER_H
