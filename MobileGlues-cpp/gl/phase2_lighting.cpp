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
#include <cstring>

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

// MG PLS layout: 48 bytes/pixel in tile SRAM (zero external bandwidth)
// pls_light_accum  — accumulated diffuse light contribution
// pls_shadow_data  — shadow depth + PCF factor from shadow pass
// pls_light_color  — colored light tint (torch=orange, soul fire=blue, etc.)
__pixel_local_inEXT MG_PLSLightBlock {
    layout(rgba8) highp vec4 pls_light_accum;
    layout(rgba8) highp vec4 pls_shadow_data;
    layout(rgba8) highp vec4 pls_light_color;
} pls_in;

__pixel_local_outEXT MG_PLSLightBlockOut {
    layout(rgba8) highp vec4 pls_light_accum;
    layout(rgba8) highp vec4 pls_shadow_data;
    layout(rgba8) highp vec4 pls_light_color;
} pls_out;

// Read from tile SRAM (zero bandwidth cost on Mali TBDR)
vec4 mg_read_light()       { return pls_in.pls_light_accum; }
vec4 mg_read_shadow()      { return pls_in.pls_shadow_data; }
vec4 mg_read_light_color() { return pls_in.pls_light_color; }

// Write back to tile SRAM
void mg_write_light(vec4 v)       { pls_out.pls_light_accum = v; }
void mg_write_shadow(vec4 v)      { pls_out.pls_shadow_data = v; }
void mg_write_light_color(vec4 v) { pls_out.pls_light_color = v; }

// Physically-based lightmap modulation:
// Reads existing tile light, applies Minecraft lightmap-style attenuation.
// blockLight in [0,1], skyLight in [0,1], lightColor is the tint.
vec4 mg_apply_colored_light(vec4 fragColor, float blockLight, float skyLight) {
    vec4 tint = mg_read_light_color();
    // Lerp between ambient (0.03) and full light per channel
    vec3 diffuse = mix(vec3(0.03), tint.rgb + vec3(0.05), blockLight);
    diffuse = max(diffuse, vec3(skyLight * 0.8 + 0.1)); // sky contribution
    return vec4(fragColor.rgb * diffuse, fragColor.a);
}

// Shadow factor from PLS: 0=fully in shadow, 1=fully lit.
// Darker where block light is low, lighter where block light is high.
float mg_shadow_factor() {
    float depth = pls_in.pls_shadow_data.r;
    float bias  = 0.005;
    // depth 0=no shadow, approaching 1=deep shadow
    return clamp(1.0 - depth + bias, 0.0, 1.0);
}
)";

// ─────────────────────────────────────────────────────────────────────────────
// FBFetch Block: lê o pixel atual do framebuffer sem sair da tile memory
// ─────────────────────────────────────────────────────────────────────────────
static const char* FBFETCH_BLOCK_DECL = R"(
#extension GL_EXT_shader_framebuffer_fetch : require

// Reads current framebuffer pixel (ZERO bandwidth on Mali TBDR — stays in tile memory)
vec4 mg_fetch_framebuffer() { return gl_LastFragData[0]; }

// Additive blend without extra render pass
vec4 mg_blend_additive(vec4 new_color, float alpha) {
    vec4 cur = gl_LastFragData[0];
    return mix(cur, cur + new_color, alpha);
}

// Multiply-blend for light contribution (avoids write-back to RAM)
vec4 mg_blend_light(vec4 base_color, vec4 light_color) {
    return gl_LastFragData[0] * light_color + base_color;
}

// Shadow darkening applied directly on the framebuffer fetch result.
// shadow_factor: 0=full shadow (dark), 1=fully lit.
// min_ambient: minimum brightness even in complete darkness (0.0–0.3 recommended).
vec4 mg_apply_shadow(float shadow_factor, float min_ambient) {
    vec4 cur = gl_LastFragData[0];
    float brightness = mix(min_ambient, 1.0, shadow_factor);
    return vec4(cur.rgb * brightness, cur.a);
}

// Colored light blend: tints the current framebuffer pixel with light_color.
// Simulates torch/beacon colored light without extra passes.
vec4 mg_tint_colored_light(vec4 light_color, float intensity) {
    vec4 cur = gl_LastFragData[0];
    return vec4(cur.rgb * mix(vec3(1.0), light_color.rgb, intensity * light_color.a), cur.a);
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
    static int frame_count = 0;
    if (++frame_count % 300 == 0) {
        LOG_I("[Phase2] Frame stats: draw_calls=%d fbo_changes=%d shaders_patched=%d",
              g_frame_draw_calls, g_frame_fbo_changes, g_status.shaders_patched);
    }
    g_frame_draw_calls = 0;
    g_frame_fbo_changes = 0;
}

// ─────────────────────────────────────────────────────────────────────────────
// Shadow injection: injects a shadow-map PCF lookup + light-dependent darkening
// into fragment shaders that have lighting keywords.
//
// The injected GLSL:
//   - Declares u_mg_shadowMap (sampler2D) and u_mg_lightMVP (mat4)
//   - Computes shadow factor via PCF (4-tap soft shadow)
//   - Applies: fragColor.rgb *= mix(MG_SHADOW_MIN_AMBIENT, 1.0, shadow)
//   - Where MG_SHADOW_MIN_AMBIENT = 0.08 (nearly black in full shadow)
//
// This means: where block light is low → darker; where light is high → normal.
// Sombras escuras onde não há luz, claras onde tem luz.
// ─────────────────────────────────────────────────────────────────────────────

static const char* SHADOW_INJECT_DECL = R"(
#define MG_SHADOW_MIN_AMBIENT 0.08

uniform highp sampler2D u_mg_shadowMap;
uniform highp mat4      u_mg_lightMVP;
uniform highp float     u_mg_shadowBias;

// 4-tap PCF soft shadow (cheap on Mali: 4 texture fetches in tile)
float mg_pcf_shadow(vec4 lightSpacePos) {
    vec3 proj = lightSpacePos.xyz / lightSpacePos.w;
    proj = proj * 0.5 + 0.5;
    if (proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0) return 1.0;
    float currentDepth = proj.z - u_mg_shadowBias;
    float texel = 1.0 / 1024.0;
    float shadow = 0.0;
    shadow += (texture(u_mg_shadowMap, proj.xy + vec2(-texel, -texel)).r < currentDepth) ? 0.0 : 0.25;
    shadow += (texture(u_mg_shadowMap, proj.xy + vec2( texel, -texel)).r < currentDepth) ? 0.0 : 0.25;
    shadow += (texture(u_mg_shadowMap, proj.xy + vec2(-texel,  texel)).r < currentDepth) ? 0.0 : 0.25;
    shadow += (texture(u_mg_shadowMap, proj.xy + vec2( texel,  texel)).r < currentDepth) ? 0.0 : 0.25;
    return shadow;
}

// Full shadow + ambient contribution: darker in shadow, lighter in light.
// lightLevel in [0,1] from Minecraft's lightmap (block light channel).
float mg_shadow_with_light(vec4 worldPos, float lightLevel) {
    float pcf = mg_pcf_shadow(u_mg_lightMVP * worldPos);
    // Combine shadow map with in-game light level for realistic result
    float combined = mix(pcf, 1.0, lightLevel * 0.85);
    return mix(MG_SHADOW_MIN_AMBIENT, 1.0, combined);
}
)";

static const char* SHADOW_VERT_DECL = R"(
uniform highp mat4 u_mg_lightMVP;
out highp vec4 v_mg_lightSpacePos;
// Call in vertex main: v_mg_lightSpacePos = u_mg_lightMVP * <world position>;
)";

// Verifica se há injeção de sombra viável neste shader
static bool shader_suitable_for_shadow(const std::string& src) {
    // Vertex shaders that compute world positions are tagged for light-space output
    return src.find("gl_Position") != std::string::npos ||
           src.find("lightmap") != std::string::npos ||
           src.find("TEXTURE_LIGHT") != std::string::npos ||
           src.find("blockLight") != std::string::npos;
}

std::string phase2_inject_shadow(const std::string& glsl_src, bool is_fragment, int shadow_unit) {
    if (!phase2_is_active()) return glsl_src;
    // Only inject into lighting-aware fragment shaders
    if (!is_fragment) return glsl_src;
    if (!shader_has_lighting(glsl_src) && !shader_has_blending(glsl_src)) return glsl_src;
    // Don't double-inject
    if (glsl_src.find("u_mg_shadowMap") != std::string::npos) return glsl_src;
    // Don't inject if the shader has no main function (protection)
    if (glsl_src.find("void main") == std::string::npos) return glsl_src;

    std::string version_line = extract_version_line(glsl_src);
    size_t insert_pos = glsl_src.find('\n');
    if (insert_pos == std::string::npos) return glsl_src;

    // Build the shadow sampler binding with the requested unit
    std::string sampler_binding = "layout(binding=" + std::to_string(shadow_unit) + ") ";
    std::string decl = std::string(SHADOW_INJECT_DECL);
    decl.replace(decl.find("uniform highp sampler2D u_mg_shadowMap;"),
                 std::string("uniform highp sampler2D u_mg_shadowMap;").size(),
                 sampler_binding + "uniform highp sampler2D u_mg_shadowMap;");

    std::string patched = version_line + decl + glsl_src.substr(insert_pos + 1);
    g_status.shaders_patched++;
    LOG_D("[Phase2] Shadow PCF injected (unit=%d, total=%d)", shadow_unit, g_status.shaders_patched);
    return patched;
}

bool phase2_colored_light_supported() {
    return g_status.pls_supported;
}

unsigned int phase2_shadow_tex() {
    // Returns 0 — shadow tex is managed externally by ShadowPipeline
    // Call MG::shadowPipeline().shadowTex() directly for the texture ID
    return 0;
}
