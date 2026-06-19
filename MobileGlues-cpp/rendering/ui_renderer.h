#pragma once
// MobileGlues - rendering/ui_renderer.h
// Batch UI rendering - all HUD elements in 2-3 draw calls
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <vector>
namespace MG {
struct UIVertex { float x,y,u,v; float r,g,b,a; };
class UIRenderer {
public:
    static constexpr int MAX_VERTS=8192;
    bool   init(int w,int h);
    void   destroy();
    void   beginFrame(int w,int h);
    void   drawQuad(float x,float y,float w,float h,
                    float u0,float v0,float u1,float v1,
                    float r,float g,float b,float a);
    void   flush(GLuint atlas);
    bool   isAvailable() const { return m_prog!=0; }
private:
    GLuint m_prog=0,m_vbo=0,m_vao=0;
    std::vector<UIVertex> m_verts;
    float  m_ortho[16]={};
    int    m_w=0,m_h=0;
};
UIRenderer& uiRenderer();
} // MG
#endif
