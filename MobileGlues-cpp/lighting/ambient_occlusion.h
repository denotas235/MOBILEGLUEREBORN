#pragma once
// MobileGlues - lighting/ambient_occlusion.h
// HBAO-Lite Ambient Occlusion (fullscreen PLS pass)
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
class AmbientOcclusion {
public:
    bool  init(int w,int h);
    void  resize(int w,int h);
    void  destroy();
    bool  run(GLuint depthTex,GLuint normalTex,float projX,float projY);
    bool  isAvailable() const { return m_prog!=0; }
    GLuint aoTex() const { return m_aoTex; }
private:
    void   createResources(int w,int h);
    GLuint m_prog=0,m_aoTex=0,m_fbo=0,m_qVAO=0,m_qVBO=0;
    int    m_w=0,m_h=0;
};
AmbientOcclusion& ao();
} // MG
#endif
