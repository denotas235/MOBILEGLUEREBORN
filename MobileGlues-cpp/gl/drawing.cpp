// MobileGlues - gl/drawing.cpp
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only
// End of Source File Header

#include "drawing.h"
#include "buffer.h"
#include "framebuffer.h"
#include "mg.h"
#include "texture.h"
#include "phase2_lighting.h"
#include "mali_sorter.h"
#include "../lighting/shadow_pipeline.h"
#include <ankerl/unordered_dense.h>

#define DEBUG 0

GLuint bufSampelerProg;
GLuint bufSampelerLoc;
std::string bufSampelerName;

extern UnorderedMap<GLuint, bool> program_map_is_sampler_buffer_emulated;
extern UnorderedMap<GLuint, bool> program_map_is_atomic_counter_emulated;

UnorderedMap<GLuint, SamplerInfo> g_samplerCacheForSamplerBuffer;

void setupBufferTextureUniforms(GLuint program) {
    LOG_D("setupBufferTextureUniforms, program: %d", program);

    if (!program_map_is_sampler_buffer_emulated[program]) return;

    if (g_samplerCacheForSamplerBuffer.find(program) == g_samplerCacheForSamplerBuffer.end()) {
        auto& progSamplerInfo = g_samplerCacheForSamplerBuffer[program];
        GLint locWidth = GLES.glGetUniformLocation(program, "u_BufferTexWidth");
        GLint locHeight = GLES.glGetUniformLocation(program, "u_BufferTexHeight");
        if (locWidth == -1) {
            LOG_W("u_BufferTexWidth uniform not found in program %d", program);
            return;
        }

        progSamplerInfo.locHeight = locHeight;
        progSamplerInfo.locWidth = locWidth;
        progSamplerInfo.samplers.clear();

        GLint numUniforms = 0;
        GLES.glGetProgramiv(program, GL_ACTIVE_UNIFORMS, &numUniforms);
        LOG_D("Program %d has %d active uniforms", program, numUniforms);

        for (GLint i = 0; i < numUniforms; ++i) {
            const GLsizei bufSize = 256;
            GLchar name[bufSize];
            GLsizei length = 0;
            GLint size = 0;
            GLenum type = 0;
            GLES.glGetActiveUniform(program, i, bufSize, &length, &size, &type, name);

            if (type == GL_SAMPLER_2D || type == GL_INT_SAMPLER_2D) {
                GLint locSampler = GLES.glGetUniformLocation(program, name);
                progSamplerInfo.samplers.push_back(locSampler);
            }
        }
    }

    auto& progSamplerInfo = g_samplerCacheForSamplerBuffer[program];

    GLint locWidth = progSamplerInfo.locWidth;
    GLint locHeight = progSamplerInfo.locHeight;

    for (auto locSampler : progSamplerInfo.samplers) {
        if (locSampler < 0) {
            continue;
        }

        GLuint prev_unit = gl_state->current_tex_unit;
        const GLint unit = 15;

        GLES.glActiveTexture(GL_TEXTURE0 + unit);
        GLint texId = 0;
        GLES.glGetIntegerv(GL_TEXTURE_BINDING_2D, &texId);
        if (texId == 0) {
            GLES.glActiveTexture(GL_TEXTURE0 + prev_unit);
            continue;
        }

        auto texObject = mgGetTexObjectByID(texId);

        GLES.glUniform1i(locSampler, unit);
        GLES.glUniform1i(locWidth, texObject->width);
        GLES.glUniform1i(locHeight, texObject->height);

        GLES.glActiveTexture(GL_TEXTURE0 + prev_unit);
    }
}

void prepareForDraw() {
    LOG_D("prepareForDraw...")
    if (hardware->emulate_texture_buffer) {
        setupBufferTextureUniforms(gl_state->current_program);
    }

    // ── ShadowPipeline: lazy init + per-draw uniform upload ───────────────────
    // Creates the 1024×1024 shadow depth FBO the first time it is called.
    // On every draw, uploads u_mg_lightMVP + binds the shadow map to unit 7
    // for any shader that received phase2_inject_shadow() injection.
    // Cost when no shadow uniforms present: one cached map lookup (~0 ns).
    static bool s_shadowInited = false;
    if (!s_shadowInited) {
        s_shadowInited = true;
        if (MG::shadowPipeline().init(1024)) {
            // Default sun direction: 45° above south horizon (good for mid-day)
            // Java mod (MaliWorld) will call mg_updateSunDirection() each tick
            // to rotate this with the actual day/night cycle.
            MG::shadowPipeline().updateLightDirection(0.5f, -0.7f, 0.5f);
            LOG_D("[Shadow] ShadowPipeline ready — default sun dir (0.5,-0.7,0.5)");
        } else {
            LOG_D("[Shadow] ShadowPipeline init failed (device may lack depth FBO support)");
        }
    }

    if (MG::shadowPipeline().isAvailable()) {
        GLuint prog = gl_state->current_program;
        if (prog) {
            bool uploaded = MG::shadowPipeline().uploadShadowUniforms(prog);
            if (uploaded) {
                // Restore active texture unit that shadow map binding may have changed
                GLES.glActiveTexture(GL_TEXTURE0 + gl_state->activeUnit);
            }
        }
    }
}

void glDrawElementsInstanced(GLenum mode, GLsizei count, GLenum type, const void* indices, GLsizei primcount) {
    LOG()
    LOG_D("glDrawElementsInstanced, mode: %d, count: %d, type: %d, indices: %p, primcount: %d", mode, count, type,
          indices, primcount)
    prepareForDraw();
    GLES.glDrawElementsInstanced(mode, count, type, indices, primcount);
    phase2_on_draw_call();
    CHECK_GL_ERROR
}

void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void* indices) {
    LOG()
    LOG_D("glDrawElements, mode: %d, count: %d, type: %d, indices: %p", mode, count, type, indices)
    if (mali_sorter_on_draw_elements(mode, count, type, indices, 0, false)) {
        return;
    }
    prepareForDraw();
    GLES.glDrawElements(mode, count, type, indices);
    phase2_on_draw_call();
    CHECK_GL_ERROR
}

void glDrawArrays(GLenum mode, GLint first, GLsizei count) {
    LOG()
    LOG_D("glDrawArrays, mode: %d, first: %d, count: %d", mode, first, count)
    prepareForDraw();
    GLES.glDrawArrays(mode, first, count);
    phase2_on_draw_call();
    CHECK_GL_ERROR
}

void glDrawArraysInstanced(GLenum mode, GLint first, GLsizei count, GLsizei instancecount) {
    LOG()
    LOG_D("glDrawArraysInstanced, mode: %d, first: %d, count: %d, instancecount: %d", mode, first, count, instancecount)
    prepareForDraw();
    GLES.glDrawArraysInstanced(mode, first, count, instancecount);
    phase2_on_draw_call();
    CHECK_GL_ERROR
}

void glBindImageTexture(GLuint unit, GLuint texture, GLint level, GLboolean layered, GLint layer, GLenum access,
                        GLenum format) {
    LOG()
    LOG_D("glBindImageTexture, unit: %d, texture: %d, level: %d, layered: %d, layer: %d, access: %d, format: %d", unit,
          texture, level, layered, layer, access, format)
    GLES.glBindImageTexture(unit, texture, level, layered, layer, access, format);
    CHECK_GL_ERROR
}

void glUniform1i(GLint location, GLint v0) {
    LOG()
    LOG_D("glUniform1i, location: %d, v0: %d", location, v0)
    GLES.glUniform1i(location, v0);
    CHECK_GL_ERROR
}

void bindAllAtomicCounterAsSSBO();
void glDispatchCompute(GLuint num_groups_x, GLuint num_groups_y, GLuint num_groups_z) {
    LOG()
    LOG_D("glDispatchCompute, num_groups_x: %d, num_groups_y: %d, num_groups_z: %d", num_groups_x, num_groups_y,
          num_groups_z)
    if (program_map_is_atomic_counter_emulated[gl_state->current_program]) {
        bindAllAtomicCounterAsSSBO();
        LOG_D("Atomic counters bound as SSBOs for program %d", gl_state->current_program);
    } else {
        LOG_D("No atomic counters bound as SSBOs for program %d", gl_state->current_program);
    }
    GLES.glDispatchCompute(num_groups_x, num_groups_y, num_groups_z);
    CHECK_GL_ERROR
}

void glMemoryBarrier(GLbitfield barriers) {
    LOG()
    LOG_D("glMemoryBarrier, barriers: %d", barriers)
    if (program_map_is_atomic_counter_emulated[gl_state->current_program]) {
        barriers |= GL_ATOMIC_COUNTER_BARRIER_BIT;
        barriers |= GL_SHADER_STORAGE_BARRIER_BIT;
    }
    GLES.glMemoryBarrier(barriers);
    CHECK_GL_ERROR
}

void glDrawElementsBaseVertex(GLenum mode, GLsizei count, GLenum type, const void* indices, GLint basevertex) {
    LOG()
    LOG_D("glDrawElementsBaseVertex, mode: %d, count: %d, type: %d, indices: %p, basevertex: %d", mode, count, type,
          indices, basevertex);
    if (mali_sorter_on_draw_elements(mode, count, type, indices, basevertex, true)) {
        return;
    }
    prepareForDraw();
    if (hardware->es_version < 320 && !g_gles_caps.GL_EXT_draw_elements_base_vertex &&
        !g_gles_caps.GL_OES_draw_elements_base_vertex) {
        // TODO: use indirect drawing for GLES 3.1
        LOG_D("Emulating glDrawElementsBaseVertex")
        GLint prevElementBuffer;
        GLES.glGetIntegerv(GL_ELEMENT_ARRAY_BUFFER_BINDING, &prevElementBuffer);

        if (basevertex == 0) {
            GLES.glDrawElements(mode, count, type, indices);
            return;
        }

        size_t indexSize;
        switch (type) {
        case GL_UNSIGNED_INT:
            indexSize = sizeof(GLuint);
            break;
        case GL_UNSIGNED_SHORT:
            indexSize = sizeof(GLushort);
            break;
        case GL_UNSIGNED_BYTE:
            indexSize = sizeof(GLubyte);
            break;
        default:
            return;
        }

        void* tempIndices = malloc(count * indexSize);
        if (!tempIndices) {
            return;
        }

        if (prevElementBuffer != 0) {
            GLES.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, prevElementBuffer);
            void* srcData =
                GLES.glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, (GLintptr)indices, count * indexSize, GL_MAP_READ_BIT);

            if (srcData) {
                memcpy(tempIndices, srcData, count * indexSize);
                GLES.glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);
            } else {
                free(tempIndices);
                return;
            }
        } else {
            memcpy(tempIndices, indices, count * indexSize);
        }

        switch (type) {
        case GL_UNSIGNED_INT:
            for (int j = 0; j < count; ++j) {
                ((GLuint*)tempIndices)[j] += basevertex;
            }
            break;
        case GL_UNSIGNED_SHORT:
            for (int j = 0; j < count; ++j) {
                ((GLushort*)tempIndices)[j] += basevertex;
            }
            break;
        case GL_UNSIGNED_BYTE:
            for (int j = 0; j < count; ++j) {
                ((GLubyte*)tempIndices)[j] += basevertex;
            }
            break;
        }

        GLuint tempBuffer;
        GLES.glGenBuffers(1, &tempBuffer);
        GLES.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, tempBuffer);
        GLES.glBufferData(GL_ELEMENT_ARRAY_BUFFER, count * indexSize, tempIndices, GL_STREAM_DRAW);
        free(tempIndices);

        GLES.glDrawElements(mode, count, type, 0);

        GLES.glDeleteBuffers(1, &tempBuffer);
        GLES.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, prevElementBuffer);

        CHECK_GL_ERROR
    } else {
        GLES.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
    }
    phase2_on_draw_call();
    CHECK_GL_ERROR
}

void glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat *value) {
    LOG()
    LOG_D("glUniformMatrix4fv, location: %d, count: %d, transpose: %d", location, count, transpose)
    mali_sorter_on_uniform_matrix4fv(location, count, transpose, value);
    GLES.glUniformMatrix4fv(location, count, transpose, value);
    CHECK_GL_ERROR
}

