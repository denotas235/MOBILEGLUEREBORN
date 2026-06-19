// MobileGlues - core/mali_caps.cpp
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "mali_caps.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>

namespace MG {

MaliCaps& getMaliCaps() {
    static MaliCaps s;
    return s;
}

void detectMaliCaps() {
    auto& c = getMaliCaps();
    if (c.isDetected) return;
    c.isDetected = true;

    // --- Extension flags via binary/runtime scan (works pre-GL-context) ---
    c.hasPLS            = has_extension("GL_EXT_shader_pixel_local_storage");
    c.hasFBFetch        = has_extension("GL_EXT_shader_framebuffer_fetch") ||
                          has_extension("GL_ARM_shader_framebuffer_fetch");
    c.hasASTCLDR        = has_extension("GL_KHR_texture_compression_astc_ldr") ||
                          has_extension("GL_OES_texture_compression_astc");
    c.hasBufferStorage  = has_extension("GL_EXT_buffer_storage");
    c.hasShadowSamplers = has_extension("GL_EXT_shadow_samplers");
    c.hasMultiDraw      = has_extension("GL_EXT_multi_draw_indirect");
    c.hasComputeShader  = has_extension("GL_ANDROID_extension_pack_es31a");
    c.hasGeometryShader = has_extension("GL_EXT_geometry_shader");
    c.hasTessellation   = has_extension("GL_EXT_tessellation_shader");
    c.hasColorFloat     = has_extension("GL_EXT_color_buffer_float");
    c.hasMSAAExt        = has_extension("GL_EXT_multisampled_render_to_texture");
    c.hasBlendAdvanced  = has_extension("GL_KHR_blend_equation_advanced");
    c.hasFramebufferFetch = c.hasFBFetch;

    // --- GL limits (requires GL context — called after EGL init) ---
    if (GLES.glGetIntegerv) {
        GLES.glGetIntegerv(GL_MAX_TEXTURE_SIZE, &c.maxTextureSize);
        GLES.glGetIntegerv(GL_MAX_UNIFORM_BUFFER_BINDINGS, &c.maxUniformBindings);
        if (c.maxAnisotropy < 2.0f && has_extension("GL_EXT_texture_filter_anisotropic")) {
            GLES.glGetFloatv(0x84FFu, &c.maxAnisotropy); // GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT
        }
    }

    // --- Estimate shader cores from renderer string ---
    const char* renderer = GLES.glGetString ? (const char*)GLES.glGetString(GL_RENDERER) : nullptr;
    if (renderer) {
        if      (strstr(renderer,"G52") || strstr(renderer,"g52")) c.shaderCores = 1;
        else if (strstr(renderer,"G57") || strstr(renderer,"G68")) c.shaderCores = 2;
        else if (strstr(renderer,"G72") || strstr(renderer,"G76")) c.shaderCores = 4;
        else if (strstr(renderer,"G710") || strstr(renderer,"G715")) c.shaderCores = 8;
        else c.shaderCores = 1;
    }
    c.maxShadowRes = (c.shaderCores >= 2) ? 2048.0f : 1024.0f;

    MG_LOG_I("MaliCaps: maxTex=%d aniso=%.1f cores=%d PLS=%d ASTC=%d MDI=%d",
             c.maxTextureSize, c.maxAnisotropy, c.shaderCores,
             (int)c.hasPLS,(int)c.hasASTCLDR,(int)c.hasMultiDraw);
}

} // namespace MG
#endif
