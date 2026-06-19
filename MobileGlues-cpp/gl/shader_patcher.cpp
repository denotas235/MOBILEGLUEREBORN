// MobileGlues - gl/shader_patcher.cpp
// PURE PASS-THROUGH — does NOT modify shader source in any way.
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

// History of breakage this file caused:
//   - Included "shader_header_manager.h" (never existed in repo) → compile error
//   - normalise_shader_header() prepended \n before #version →
//       ERROR: 0:1: '' : invalid version directive
//       ERROR: 0:5: 'layout' : syntax error
//     on EVERY Minecraft core shader (gui, terrain, block, sky, clouds…)
//
// Decision: shader source passes through UNCHANGED.
// GLSLtoGLSLES (glsl_for_es.h) already handles all necessary conversions.
// Any further patching must be done inside glsl_for_es.h where the full
// GLSL AST is available — not here as a fragile string search.

#ifndef __APPLE__

#include "shader_patcher.h"
#include <string>
#include <cstring>

std::string patch_shader_source(const char* original_source, GLenum /*shader_type*/) {
    if (!original_source) return "";
    return std::string(original_source);
}

#endif // !__APPLE__
