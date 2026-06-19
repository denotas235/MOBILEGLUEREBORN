#pragma once
// MobileGlues - core/gl_context.h
// GL State Cache - avoids redundant state changes (~20-30% CPU on render thread)
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {

struct GLStateCache {
    GLenum     blendSrcRGB   = GL_ONE;
    GLenum     blendDstRGB   = GL_ZERO;
    GLenum     blendSrcAlpha = GL_ONE;
    GLenum     blendDstAlpha = GL_ZERO;
    GLenum     depthFunc     = GL_LESS;
    GLboolean  depthMask     = GL_TRUE;
    bool       blendEnabled  = false;
    bool       depthTest     = false;
    bool       cullEnabled   = false;
    bool       scissorEnabled= false;
    GLenum     cullFace      = GL_BACK;
    GLuint     prog          = 0;
    GLuint     fbo           = 0;
    GLuint     vao           = 0;
    GLuint     tex2d[16]     = {};
    int        activeUnit    = 0;

    void setBlend(bool en);
    void setBlendFunc(GLenum sRGB,GLenum dRGB,GLenum sA,GLenum dA);
    void setDepthTest(bool en);
    void setDepthMask(GLboolean m);
    void setDepthFunc(GLenum f);
    void setCull(bool en,GLenum face=GL_BACK);
    void setScissor(bool en);
    void useProgram(GLuint p);
    void bindFBO(GLuint f);
    void bindVAO(GLuint v);
    void bindTex2D(int unit,GLuint t);
    void invalidate(); // Force next calls to re-apply
};

GLStateCache& glCache();

} // namespace MG
#endif
