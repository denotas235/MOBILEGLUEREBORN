#pragma once
// MobileGlues - effects/bloom_pls.h
// Dual-pass Bloom: downsample + upsample kawase blur, uses PLS if available
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
class BloomPLS {
public:
    static constexpr int BLOOM_PASSES = 4;
    bool   init(int w,int h);
    void   resize(int w,int h);
    void   destroy();
    bool   run(GLuint sceneTex,float threshold=0.8f,float intensity=0.4f);
    bool   isAvailable() const { return m_progDown!=0; }
    GLuint bloomTex() const    { return m_chain[0]; }
private:
    void   createResources(int w,int h);
    GLuint m_progDown=0,m_progUp=0;
    GLuint m_chain[BLOOM_PASSES]={};
    GLuint m_fbos[BLOOM_PASSES]={};
    GLuint m_qVAO=0,m_qVBO=0;
    int    m_w=0,m_h=0;
};
BloomPLS& bloom();
} // MG
#endif
