#pragma once
// MobileGlues - lighting/shadow_pipeline.h
// PCF Shadow Map pipeline for Mali TBDR
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {

class ShadowPipeline {
public:
    static constexpr int MAP_DEFAULT = 1024;
    bool  init(int mapSize=MAP_DEFAULT);
    void  resize(int mapSize);
    void  destroy();
    void  beginShadowPass();
    void  endShadowPass();
    void  bindShadowMap(int unit);
    bool  isAvailable() const { return m_fbo!=0; }
    GLuint shadowTex() const  { return m_tex; }
    float* lightMatrix()      { return m_lightMVP; }

private:
    void   createResources(int sz);
    GLuint m_fbo=0, m_tex=0;
    int    m_size=MAP_DEFAULT;
    GLint  m_savedFBO=0, m_savedVP[4]={};
    float  m_lightMVP[16]={};
};

ShadowPipeline& shadowPipeline();

} // MG
#endif
