// MobileGlues - gl/mali_sorter.h
// Phase 4: Mali GPU Front-to-Back Chunks Sorter and Foveated Culling
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef MALI_SORTER_H
#define MALI_SORTER_H

#include <GL/gl.h>
#include <string>

struct DeferredDrawCall {
    GLuint vao;
    GLuint program;
    float modelView[16];
    GLenum mode;
    GLsizei count;
    GLenum type;
    const void* indices;
    GLint basevertex;
    bool hasBaseVertex;
    float distanceSq;
};

// Initializes the Mali sorter system and loads settings from v_sight_logic.json
void mali_sorter_init();

// Called when a program is compiled/linked to scan matrix uniform locations
void mali_sorter_on_link_program(GLuint program);

// Called when the active shader program changes
void mali_sorter_on_use_program(GLuint program);

// Intercepts model-view matrices uploaded by the game
void mali_sorter_on_uniform_matrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);

// Intercepts draw calls to either buffer them for sorting or drop them via culling.
// Returns true if the draw call was buffered/handled, false if it should be executed immediately.
bool mali_sorter_on_draw_elements(GLenum mode, GLsizei count, GLenum type, const void* indices, GLint basevertex, bool hasBaseVertex);

// Sorts and flushes all buffered draw calls
void mali_sorter_flush();

// Flushes buffered draws before changing framebuffer
void mali_sorter_on_framebuffer_change();

// Flushes buffered draws at frame end (eglSwapBuffers)
void mali_sorter_on_frame_end();

// Injects AtmosV-Alpha fog shader effect
std::string mali_sorter_inject_fog(const std::string& glsl_src, bool is_fragment);

#endif // MALI_SORTER_H
