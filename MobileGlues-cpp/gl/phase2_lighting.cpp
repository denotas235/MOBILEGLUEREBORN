// MobileGlues - gl/phase2_lighting.cpp
// Phase 2: PLS + FBFetch Lighting & Shadow Optimization for Mali-G52 (TBDR)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#include "phase2_lighting.h"
#include "extension_scanner.h"
#include "log.h"
#include "mg.h"
#include "../config/settings.h"
#include <string>
#include <algorithm>

#define DEBUG 0

// ─────────────────────────────────────────────────────────────────────────────
// Estado interno
// ─────────────────────────────────────────────────────────────────────────────
static Phase2Status g_status;
static int  g_frame_draw_calls = 0;
static int  g_frame_fbo_changes = 0;

// ─────────────────────────────────────────────────────────────────────────────
// Helpers de detecção de padrões GLSL
// ─────────────────────────────────────────────────────────────────────────────

// Verifica se o shader faz cálculo de luz (Minecraft usa variáveis de luz bem conhecidas)
static bool shader_has_lighting(const std::string& src) {
    return src.find("TEXTURE_LIGHT") != std::string::npos ||
           src.find("lightmap")      != std::string::npos ||
           src.find("lightColor")    != std::string::npos ||
           src.find("LightColor")    != std::string::npos ||
           src.find("ambientLight")  != std::string::npos ||
           src.find("skyLight")      != std::string::npos ||
           src.find("blockLight")    != std::string::npos ||
           src.find("fogColor")      != std::string::npos ||
           src.find("u_lightmap")    != std::string::npos;
}

// Verifica se o shader faz shadow mapping
static bool shader_has_shadow(const std::string& src) {
    return src.find("shadow")       != std::string::npos ||
           src.find("Shadow")       != std::string::npos ||
           src.find("depthMap")     != std::string::npos ||
           src.find("shadowMap")    != std::string::npos ||
           src.find("cascadedShadow") != std::string::npos;
}

// Verifica se o shader faz blending/composição (candidato a FBFetch)
static bool shader_has_blending(const std::string& src) {
    return src.find("gl_FragColor")  != std::string::npos ||
           src.find("fragColor")     != std::string::npos ||
           src.find("outColor")      != std::string::npos ||
           src.find("FragColor")     != std::string::npos;
}

// Verifica se o shader já tem PLS/FBFetch (evita injeção dupla)
static bool shader_already_patched(const std::string& src) {
    return src.find("__pixel_local_inEXT")   != std::string::npos ||
           src.find("gl_LastFragData")        != std::string::npos ||
           src.find("GL_EXT_shader_pixel_local_storage") != std::string::npos;
}

// Extrai a diretiva #version do shader
static std::string extract_version_line(const std::string& src) {
    size_t nl = src.find('\n');
    if (src.substr(0, 8) == "#version") {
        return src.substr(0, nl == std::string::npos ? src.size() : nl + 1);
    }
    return "#version 300 es\n";
}

// ─────────────────────────────────────────────────────────────────────────────
// PLS Block: estrutura que vive na tile memory durante o frame
// ─────────────────────────────────────────────────────────────────────────────
//
// Layout do bloco PLS (32 bytes / pixel na tile memory):
//   vec4 pls_light_accum  (16 bytes) → acumula contribuições de luz
//   vec4 pls_shadow_data  (16 bytes) → armazena depth/shadow factor
//
// Estes dados ficam na SRAM da GPU durante todo o frame.
// Não há acesso à RAM principal enquanto a tile está sendo processada.
//
static const char* PLS_BLOCK_DECL = R"(
#extension GL_EXT_shader_pixel_local_storage : require

__pixel_local_inEXT MG_PLSLightBlock {
    layout(rgba8) highp vec4 pls_light_accum;
    layout(rgba8) highp vec4 pls_shadow_data;
} pls_in;

__pixel_local_outEXT MG_PLSLightBlockOut {
    layout(rgba8) highp vec4 pls_light_accum;
    layout(rgba8) highp vec4 pls_shadow_data;
} pls_out;

// Recupera luz acumulada da tile memory (sem custo de bandwidth)
vec4 mg_read_light() { return pls_in.pls_light_accum; }
vec4 mg_read_shadow() { return pls_in.pls_shadow_data; }

// Escreve resultado de volta na tile memory
void mg_write_light(vec4 light) { pls_out.pls_light_accum = light; }
void mg_write_shadow(vec4 shadow) { pls_out.pls_shadow_data = shadow; }
)";

// ─────────────────────────────────────────────────────────────────────────────
// FBFetch Block: lê o pixel atual do framebuffer sem sair da tile memory
// ─────────────────────────────────────────────────────────────────────────────
static const char* FBFETCH_BLOCK_DECL = R"(
#extension GL_EXT_shader_framebuffer_fetch : require

// Lê a cor atual do framebuffer (operação ZERO bandwidth no Mali TBDR)
vec4 mg_fetch_framebuffer() {
    return gl_LastFragData[0];
}

// Blending otimizado: mistura a cor nova com a cor atual do framebuffer
// sem precisar de uma segunda passagem de render
vec4 mg_blend_additive(vec4 new_color, float alpha) {
    vec4 current = gl_LastFragData[0];
    return mix(current, current + new_color, alpha);
}

vec4 mg_blend_light(vec4 base_color, vec4 light_color) {
    vec4 current = gl_LastFragData[0];
    return current * light_color + base_color;
}
)";

// ─────────────────────────────────────────────────────────────────────────────
// Implementação pública
// ─────────────────────────────────────────────────────────────────────────────

void phase2_init() {
    if (g_status.initialized) return;
    g_status.initialized = true;

    if (!global_settings.gpu_optimizations.phase2_lighting.enabled) {
        LOG_I("[Phase2] Disabled by settings.");
        return;
    }

    const GPUCapabilities& caps = get_gpu_capabilities();

    g_status.pls_supported = caps.has_pls ||
        has_extension("GL_EXT_shader_pixel_local_storage");

    g_status.fbfetch_supported = caps.has_fbfetch ||
        has_extension("GL_EXT_shader_framebuffer_fetch") ||
        has_extension("GL_ARM_shader_framebuffer_fetch");

    g_status.tbdr_mode_active = g_status.pls_supported || g_status.fbfetch_supported;

    LOG_I("[Phase2] PLS: %s | FBFetch: %s | TBDR mode: %s",
          g_status.pls_supported    ? "YES" : "NO",
          g_status.fbfetch_supported ? "YES" : "NO",
          g_status.tbdr_mode_active  ? "ACTIVE" : "INACTIVE");

    if (!g_status.tbdr_mode_active) {
        LOG_I("[Phase2] Neither PLS nor FBFetch available on this GPU. Phase 2 inactive.");
    }
}

bool phase2_is_active() {
    return g_status.initialized && g_status.tbdr_mode_active;
}

const Phase2Status& phase2_get_status() {
    return g_status;
}

std::string phase2_inject_pls(const std::string& glsl_src, bool is_fragment) {
    if (!phase2_is_active()) return glsl_src;
    if (!g_status.pls_supported) return glsl_src;
    if (!is_fragment) return glsl_src;
    if (shader_already_patched(glsl_src)) return glsl_src;

    // Só injeta em shaders que fazem cálculo de luz ou sombra
    if (!shader_has_lighting(glsl_src) && !shader_has_shadow(glsl_src)) {
        return glsl_src;
    }

    std::string version_line = extract_version_line(glsl_src);
    size_t insert_pos = glsl_src.find('\n');
    if (insert_pos == std::string::npos) return glsl_src;

    // Injeta o bloco PLS logo após o #version
    std::string patched = version_line + PLS_BLOCK_DECL +
                          glsl_src.substr(insert_pos + 1);

    g_status.shaders_patched++;
    LOG_D("[Phase2] PLS injected into lighting shader (total patched: %d)", g_status.shaders_patched);
    return patched;
}

std::string phase2_inject_fbfetch(const std::string& glsl_src) {
    if (!phase2_is_active()) return glsl_src;
    if (!g_status.fbfetch_supported) return glsl_src;
    if (shader_already_patched(glsl_src)) return glsl_src;

    // Só injeta em fragment shaders com blending/composição
    if (!shader_has_blending(glsl_src)) return glsl_src;

    // Não injeta se PLS já foi injetado (evita conflito de extensões)
    if (glsl_src.find("GL_EXT_shader_pixel_local_storage") != std::string::npos) {
        return glsl_src;
    }

    std::string version_line = extract_version_line(glsl_src);
    size_t insert_pos = glsl_src.find('\n');
    if (insert_pos == std::string::npos) return glsl_src;

    std::string patched = version_line + FBFETCH_BLOCK_DECL +
                          glsl_src.substr(insert_pos + 1);

    g_status.shaders_patched++;
    LOG_D("[Phase2] FBFetch injected into blending shader (total patched: %d)", g_status.shaders_patched);
    return patched;
}

void phase2_on_fbo_change(unsigned int new_fbo) {
    if (!phase2_is_active()) return;
    g_frame_fbo_changes++;
    LOG_D("[Phase2] FBO changed to %u (frame changes: %d)", new_fbo, g_frame_fbo_changes);
}

void phase2_on_draw_call() {
    if (!phase2_is_active()) return;
    g_frame_draw_calls++;
}

void phase2_on_frame_end() {
    if (!phase2_is_active()) return;

    // A cada 300 frames, imprime estatísticas
    static int frame_count = 0;
    if (++frame_count % 300 == 0) {
        LOG_I("[Phase2] Frame stats: draw_calls=%d fbo_changes=%d shaders_patched=%d",
              g_frame_draw_calls, g_frame_fbo_changes, g_status.shaders_patched);
    }
    g_frame_draw_calls = 0;
    g_frame_fbo_changes = 0;
}
