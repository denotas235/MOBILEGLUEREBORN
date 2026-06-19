#pragma once
// MobileGlues - core/mali_caps.h
// Mali GPU Capability Detection - No GL context required for pre-context queries
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {

struct MaliCaps {
    int   maxTextureSize       = 4096;
    int   maxUniformBindings   = 72;
    float maxAnisotropy        = 1.0f;
    int   shaderCores          = 1;
    float maxShadowRes         = 1024.0f;
    bool  isDetected           = false;

    // Cached extension flags (queried via system binary scan - no GL context needed)
    bool  hasPLS               = false;
    bool  hasFBFetch           = false;
    bool  hasASTCLDR           = false;
    bool  hasBufferStorage     = false;
    bool  hasShadowSamplers    = false;
    bool  hasMultiDraw         = false;
    bool  hasComputeShader     = false;
    bool  hasGeometryShader    = false;
    bool  hasTessellation      = false;
    bool  hasColorFloat        = false;
    bool  hasMSAAExt           = false;
    bool  hasBlendAdvanced     = false;
    bool  hasFramebufferFetch  = false;
};

MaliCaps& getMaliCaps();
void detectMaliCaps(); // Safe to call with or without GL context

} // namespace MG
#endif
