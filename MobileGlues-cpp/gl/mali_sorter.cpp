// MobileGlues - gl/mali_sorter.cpp
// Phase 4: Mali GPU Front-to-Back Chunks Sorter and Foveated Culling
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#define DEBUG 0

#include "mali_sorter.h"
#include "v_sight_math.h"
#include "buffer.h"
#include "log.h"
#include "mg.h"
#include "../config/cJSON.h"
#include <vector>
#include <algorithm>
#include <cmath>
#include <unordered_map>
#include <string>

#ifndef M_PI
#define M_PI 3.14159265358979323846f
#endif


// Configuration settings
static float g_fovAngle = 180.0f;
static float g_fovealInnerV = 35.0f;
static float g_fovealDetailMultiplier = 1.0f;
static float g_peripheryDetailMultiplier = 0.4f;
static bool g_occlusionCulling = true;
static bool g_useDerivatives = true;
static float g_accelerationBias = 1.5f;

// Matrix tracking
struct ProgramState {
    GLint modelViewMatLoc = -1;
    float currentModelView[16] = {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
    };
    bool isChunkShader = false;
};

static std::unordered_map<GLuint, ProgramState> g_programStates;
static std::vector<DeferredDrawCall> g_deferredDraws;
static bool g_isBuffering = false;

// Helpers to load config
static void load_config() {
    const char* paths[] = {
        "/sdcard/MG/v_sight_logic.json",
        "/data/data/com.termux/files/home/MOBILEGLUEREBORN/v_sight_logic.json"
    };

    FILE* file = nullptr;
    for (const char* path : paths) {
        file = fopen(path, "r");
        if (file) {
            LOG_D("[MaliSorter] Loaded config from %s", path);
            break;
        }
    }

    if (!file) {
        LOG_W("[MaliSorter] Could not open v_sight_logic.json, using defaults.");
        return;
    }

    fseek(file, 0, SEEK_END);
    long size = ftell(file);
    fseek(file, 0, SEEK_SET);

    char* content = (char*)malloc(size + 1);
    if (content) {
        fread(content, 1, size, file);
        content[size] = '\0';
        cJSON* root = cJSON_Parse(content);
        if (root) {
            cJSON* vsight = cJSON_GetObjectItem(root, "v_sight");
            if (vsight) {
                cJSON* fov = cJSON_GetObjectItem(vsight, "fov_angle");
                if (fov) g_fovAngle = fov->valuedouble;
                cJSON* inner = cJSON_GetObjectItem(vsight, "foveal_inner_v");
                if (inner) g_fovealInnerV = inner->valuedouble;
                cJSON* fdet = cJSON_GetObjectItem(vsight, "foveal_detail_multiplier");
                if (fdet) g_fovealDetailMultiplier = fdet->valuedouble;
                cJSON* pdet = cJSON_GetObjectItem(vsight, "periphery_detail_multiplier");
                if (pdet) g_peripheryDetailMultiplier = pdet->valuedouble;
                cJSON* occ = cJSON_GetObjectItem(vsight, "occlusion_culling");
                if (occ) g_occlusionCulling = occ->valueint;
            }
            cJSON* pred = cJSON_GetObjectItem(root, "predictive_streaming");
            if (pred) {
                cJSON* der = cJSON_GetObjectItem(pred, "use_derivatives");
                if (der) g_useDerivatives = der->valueint;
                cJSON* bias = cJSON_GetObjectItem(pred, "acceleration_bias");
                if (bias) g_accelerationBias = bias->valuedouble;
            }
            cJSON_Delete(root);
        }
        free(content);
    }
    fclose(file);
}

void mali_sorter_init() {
    v_sight_math_init();
    load_config();
    g_deferredDraws.reserve(256);
    LOG_D("[MaliSorter] Initialized. FOV: %.1f, Periphery Multiplier: %.2f", g_fovAngle, g_peripheryDetailMultiplier);
}

void mali_sorter_on_link_program(GLuint program) {
    GLint numUniforms = 0;
    GLES.glGetProgramiv(program, GL_ACTIVE_UNIFORMS, &numUniforms);
    
    ProgramState state;
    for (GLint i = 0; i < numUniforms; ++i) {
        char name[256];
        GLsizei length = 0;
        GLint size = 0;
        GLenum type = 0;
        GLES.glGetActiveUniform(program, i, sizeof(name), &length, &size, &type, name);
        GLint loc = GLES.glGetUniformLocation(program, name);
        
        // Scan for model-view matrix uniforms commonly used in PojavLauncher/Sodium/Minecraft shaders
        if (strcmp(name, "ModelViewMat") == 0 || 
            strcmp(name, "u_ModelViewMatrix") == 0 || 
            strcmp(name, "matrix") == 0 || 
            strcmp(name, "u_modelView") == 0 ||
            strcmp(name, "u_modelViewMatrix") == 0) {
            state.modelViewMatLoc = loc;
            state.isChunkShader = true;
            LOG_D("[MaliSorter] Program %d detected as chunk shader. ModelViewMat location: %d", program, loc);
        }
    }
    
    g_programStates[program] = state;
}

void mali_sorter_on_use_program(GLuint program) {
    // If the active shader program changes, flush the previous program's sorted chunks first
    if (gl_state->current_program != program) {
        mali_sorter_flush();
    }
}

void mali_sorter_on_uniform_matrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value) {
    GLuint prog = gl_state->current_program;
    auto it = g_programStates.find(prog);
    if (it != g_programStates.end() && it->second.modelViewMatLoc == location) {
        // Cache the uploaded model-view matrix
        std::memcpy(it->second.currentModelView, value, 16 * sizeof(float));
    }
}

bool mali_sorter_on_draw_elements(GLenum mode, GLsizei count, GLenum type, const void* indices, GLint basevertex, bool hasBaseVertex) {
    GLuint prog = gl_state->current_program;
    auto it = g_programStates.find(prog);
    
    if (it == g_programStates.end() || !it->second.isChunkShader || count < 100) {
        return false; // Let GLES draw directly (not a chunk draw or small draw)
    }

    const float* m = it->second.currentModelView;
    
    // 1. Visão Foveada / V-Sight (180º Cone) in eye space
    // Center of a 16x16x16 chunk is at local (8, 8, 8). Transform to eye space:
    float ex = m[0] * 8.0f + m[4] * 8.0f + m[8] * 8.0f + m[12];
    float ey = m[1] * 8.0f + m[5] * 8.0f + m[9] * 8.0f + m[13];
    float ez = m[2] * 8.0f + m[6] * 8.0f + m[10] * 8.0f + m[14];
    
    float distanceSq = ex*ex + ey*ey + ez*ez;
    
    // Check if the chunk is behind the camera (ez > 0.0f)
    if (ez > 0.0f) {
        // Keep only if it's close to the player to avoid clipping artifacts
        if (distanceSq > 32.0f * 32.0f) {
            return true; // Culled (handled by dropping it)
        }
    }
    
    // 2. Variable Detail Level (Foveated detail reduction)
    float d = std::sqrt(distanceSq);
    if (d > 0.0f) {
        float cosTheta = -ez / d;
        float cosFoveal = std::cos((g_fovealInnerV * M_PI) / 180.0f);
        if (cosTheta < cosFoveal) {
            // Periphery zone: reduce index count to drop vertices
            count = static_cast<GLsizei>(count * g_peripheryDetailMultiplier);
        }
    }

    // 3. Buffer draw call for Front-to-Back sorting
    DeferredDrawCall draw;
    draw.vao = find_bound_array();
    draw.program = prog;
    std::memcpy(draw.modelView, m, 16 * sizeof(float));
    draw.mode = mode;
    draw.count = count;
    draw.type = type;
    draw.indices = indices;
    draw.basevertex = basevertex;
    draw.hasBaseVertex = hasBaseVertex;
    draw.distanceSq = distanceSq;
    
    g_deferredDraws.push_back(draw);
    g_isBuffering = true;
    
    return true; // Draw call buffered, do not execute immediately
}

void mali_sorter_flush() {
    if (!g_isBuffering || g_deferredDraws.empty()) {
        return;
    }
    
    // Sort front-to-back (ascending order of distanceSq)
    std::sort(g_deferredDraws.begin(), g_deferredDraws.end(), [](const DeferredDrawCall& a, const DeferredDrawCall& b) {
        return a.distanceSq < b.distanceSq;
    });
    
    // Save previous state to restore later
    GLint prevVao = 0;
    GLES.glGetIntegerv(GL_VERTEX_ARRAY_BINDING, &prevVao);
    
    // Draw all buffered chunk calls
    for (const auto& draw : g_deferredDraws) {
        // Bind the virtual VAO (wrapped glBindVertexArray will translate to real VAO)
        glBindVertexArray(draw.vao);
        
        // Re-upload the cached ModelView matrix
        auto it = g_programStates.find(draw.program);
        if (it != g_programStates.end()) {
            GLES.glUniformMatrix4fv(it->second.modelViewMatLoc, 1, GL_FALSE, draw.modelView);
        }
        
        // Execute draw
        if (draw.hasBaseVertex) {
            GLES.glDrawElementsBaseVertex(draw.mode, draw.count, draw.type, draw.indices, draw.basevertex);
        } else {
            GLES.glDrawElements(draw.mode, draw.count, draw.type, draw.indices);
        }
    }
    
    // Restore GLES VAO
    GLES.glBindVertexArray(prevVao);
    
    // Reset buffer
    g_deferredDraws.clear();
    g_isBuffering = false;
}

void mali_sorter_on_framebuffer_change() {
    mali_sorter_flush();
}

void mali_sorter_on_frame_end() {
    mali_sorter_flush();
}

std::string mali_sorter_inject_fog(const std::string& glsl_src, bool is_fragment) {
    if (!is_fragment) return glsl_src;
    
    std::string modified = glsl_src;
    std::string declarations = "";
    
    size_t version_pos = modified.find("#version");
    if (version_pos == std::string::npos) {
        declarations += "#extension GL_OES_standard_derivatives : enable\n";
    }
    
    if (modified.find("u_fogDensity") == std::string::npos) {
        declarations += "uniform float u_fogDensity;\n";
    }
    if (modified.find("u_viewPos") == std::string::npos) {
        declarations += "uniform vec3 u_viewPos;\n";
    }
    if (modified.find("u_fogColor") == std::string::npos) {
        declarations += "uniform vec4 u_fogColor;\n";
    }
    
    size_t insert_pos = 0;
    if (version_pos != std::string::npos) {
        insert_pos = modified.find("\n", version_pos) + 1;
    }
    
    modified.insert(insert_pos, declarations);
    
    size_t main_pos = modified.find("void main");
    if (main_pos != std::string::npos) {
        size_t last_brace = modified.rfind("}");
        if (last_brace != std::string::npos && last_brace > main_pos) {
            std::string fog_calc = R"glsl(
    // AtmosV-Alpha Fog Injection
    #if defined(GL_OES_standard_derivatives) || __VERSION__ >= 300
    float dx = dFdx(gl_FragColor.r);
    float dy = dFdy(gl_FragColor.g);
    if (abs(dx) + abs(dy) < 0.01) {
        gl_FragColor = mix(u_fogColor, gl_FragColor, 0.9);
    }
    #endif
)glsl";
            
            if (modified.find("outColor") != std::string::npos) {
                fog_calc = R"glsl(
    // AtmosV-Alpha Fog Injection
    #if defined(GL_OES_standard_derivatives) || __VERSION__ >= 300
    float dx = dFdx(outColor0.r);
    float dy = dFdy(outColor0.g);
    if (abs(dx) + abs(dy) < 0.01) {
        outColor0 = mix(u_fogColor, outColor0, 0.9);
    }
    #endif
)glsl";
            }
            
            modified.insert(last_brace, fog_calc);
        }
    }
    
    return modified;
}

