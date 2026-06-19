#pragma once
// MobileGlues - gl/earlyZ.h
// Early-Z Depth Pre-Pass — eliminates overdraw on Mali-G52
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {

class EarlyZPass {
public:
    bool init(int w,int h);
    void resize(int w,int h);
    void destroy();
    void beginPrePass();
    void endPrePass();
    void bindDepthTex(int unit);
    bool isAvailable() const { return m_fbo!=0; }
    GLuint depthTex()  const { return m_tex; }
private:
    GLuint m_fbo=0,m_tex=0;
    int    m_w=0,m_h=0;
    GLint  m_savedFBO=0;
    void   create(int w,int h);
};

EarlyZPass& earlyZ();
} // MG
#endif
