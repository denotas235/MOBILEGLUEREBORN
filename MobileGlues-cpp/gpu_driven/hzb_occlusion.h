#pragma once
// MobileGlues - gpu_driven/hzb_occlusion.h
// Hierarchical Z-Buffer Occlusion Culling
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {

class HZBOcclusion {
public:
    static constexpr int HZB_LEVELS = 8;

    bool  init(int w, int h);
    void  resize(int w, int h);
    void  destroy();
    void  buildHZB(GLuint depthTex);        // Build mip pyramid from depth
    bool  testAABB(float minX,float minY,float minZ,
                   float maxX,float maxY,float maxZ,
                   const float* viewProj);   // true = visible
    bool  isAvailable() const { return m_prog!=0; }
    GLuint hzbTex() const { return m_hzb; }

private:
    void   createResources(int w,int h);
    GLuint m_prog=0, m_hzb=0, m_fbo=0;
    GLuint m_quadVAO=0, m_quadVBO=0;
    int    m_w=0, m_h=0;
};

HZBOcclusion& hzb();

} // MG
#endif
