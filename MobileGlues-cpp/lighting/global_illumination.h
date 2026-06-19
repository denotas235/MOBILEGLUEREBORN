#pragma once
// MobileGlues - lighting/global_illumination.h
// Simple screen-space GI via blurred irradiance accumulation
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
class GlobalIllumination {
public:
    bool   init(int w,int h);
    void   resize(int w,int h);
    void   destroy();
    bool   run(GLuint sceneTex,GLuint aoTex);
    bool   isAvailable() const { return m_prog!=0; }
    GLuint giTex() const { return m_giTex; }
private:
    void   createResources(int w,int h);
    GLuint m_prog=0,m_giTex=0,m_fbo=0,m_qVAO=0,m_qVBO=0;
    int    m_w=0,m_h=0;
};
GlobalIllumination& gi();
} // MG
#endif
