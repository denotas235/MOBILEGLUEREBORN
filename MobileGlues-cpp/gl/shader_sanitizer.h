// MobileGlues - gl/shader_sanitizer.h
// Phase 1: Mali GLES 3.0 Shader Sanitizer
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef MOBILEGLUES_SHADER_SANITIZER_H
#define MOBILEGLUES_SHADER_SANITIZER_H

#include <string>

// Sanitizes raw GLSL desktop shader source for Mali GLES 3.0.
//
// Guarantees (in order):
//   1. #version 300 es is the ABSOLUTE FIRST character of the output.
//   2. precision highp float/int/sampler2D is injected right after #version.
//   3. GL_EXT_texture_compression_astc (and other unsupported extensions that
//      previously pushed #version to line 2) are silently dropped.
//   4. texture2D() legacy calls are replaced with texture().
//
// This function is safe to call before any other shader transformation.
// It operates line-by-line and does NOT use std::regex for robustness.
std::string sanitizeForMaliGLES(const std::string& source);

#endif // MOBILEGLUES_SHADER_SANITIZER_H
