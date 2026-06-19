// MobileGlues - gl/shader_patcher.cpp
// Safe body-level patches for Minecraft 1.21 GLSL -> GLES 3.x
// Copyright (c) 2025-2026 Denocorp
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

// NOTE: This patcher intentionally does NOT touch the #version directive.
// #version normalization is already handled by GLSLtoGLSLES (glsl_for_es.h).
// Any modification to #version here would prepend a '\n' and cause:
//   ERROR: 0:1: '' : invalid version directive
//   ERROR: 0:5: 'layout' : syntax error
// which breaks every Minecraft core shader.

#ifndef __APPLE__

#include "shader_patcher.h"
#include "log.h"
#include "../gles/loader.h"
#include <string>
#include <cstring>
#include <cstddef>

#define DEBUG 0

// ---------------------------------------------------------------------------
// patch_shader_source
// Applies ONLY safe, body-level transformations to a GLSL shader string.
// Must never touch or relocate the #version directive.
// ---------------------------------------------------------------------------
std::string patch_shader_source(const char* original_source, GLenum shader_type) {
    if (!original_source) return "";
    std::string source(original_source);

    // Split at the end of the #version line so we never mutate it.
    // If there is no #version line, treat the whole string as the body.
    std::string header;
    std::string body;

    size_t ver_pos = source.find("#version");
    if (ver_pos != std::string::npos) {
        size_t eol = source.find('\n', ver_pos);
        if (eol != std::string::npos) {
            header = source.substr(0, eol + 1);
            body   = source.substr(eol + 1);
        } else {
            header = source;
        }
    } else {
        body = source;
    }

    // ------------------------------------------------------------------
    // Patch 1 (vertex shader only): replace deprecated GLSL 1.x "varying"
    // qualifier with the GLES 3.0 "out" qualifier.
    // Only replaces tokens at word-start (preceded by whitespace / newline).
    // ------------------------------------------------------------------
    if (shader_type == GL_VERTEX_SHADER) {
        std::string result;
        result.reserve(body.size());
        size_t pos = 0;
        const std::string token = "varying ";
        while (pos < body.size()) {
            size_t found = body.find(token, pos);
            if (found == std::string::npos) {
                result.append(body, pos, body.size() - pos);
                break;
            }
            // Ensure we only replace standalone "varying " (not inside an identifier)
            bool at_word_start = (found == 0) ||
                                 (body[found - 1] == '\n') ||
                                 (body[found - 1] == ' ')  ||
                                 (body[found - 1] == '\t');
            if (at_word_start) {
                result.append(body, pos, found - pos);
                result.append("out ");
                pos = found + token.size();
            } else {
                result.append(body, pos, found - pos + token.size());
                pos = found + token.size();
            }
        }
        body = std::move(result);
    }

    // ------------------------------------------------------------------
    // Patch 2: replace gl_ClipDistance[N] with the literal 1.0.
    // gl_ClipDistance is not available in base GLES 3.x without
    // GL_EXT_clip_cull_distance. Writing to it silently crashes some Mali
    // drivers; replacing with 1.0 is safe (plane always visible).
    // ------------------------------------------------------------------
    {
        size_t pos = 0;
        const std::string prefix = "gl_ClipDistance[";
        while ((pos = body.find(prefix, pos)) != std::string::npos) {
            size_t close = body.find(']', pos + prefix.size());
            if (close != std::string::npos) {
                body.replace(pos, close - pos + 1, "1.0");
                pos += 3; // len("1.0")
            } else {
                break;
            }
        }
    }

    return header + body;
}

#endif // !__APPLE__
