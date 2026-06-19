#pragma once
// MobileGlues - effects/reflections_ssr.h
// Screen-Space Reflections (SSR) for water and glass
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
class SSReflections {
public:
    bool   init(int w,int h);
    void   resize(int w,int h);
    void   destroy();
    bool   run(GLuint sceneTex,GLuint depthTex,const float* projMat,const float* invViewMat);
    bool   isAvailable() const { return m_prog!=0; }
    GLuint ssrTex() const { return m_ssrTex; }
private:
    void   createResources(int w,int h);
    GLuint m_prog=0,m_ssrTex=0,m_fbo=0,m_qVAO=0,m_qVBO=0;
    int    m_w=0,m_h=0;
};
SSReflections& ssr();
} // MG
#endif
