// MobileGlues - rendering/ui_renderer.cpp
// Batches UI quads, flushes with single glDrawArrays
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "ui_renderer.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>

namespace MG {
UIRenderer& uiRenderer(){ static UIRenderer s; return s; }

static const char* VERT = R"(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
layout(location=1) in vec2 a_uv;
layout(location=2) in vec4 a_color;
uniform mat4 u_ortho;
out vec2 v_uv;
out vec4 v_color;
void main(){ v_uv=a_uv; v_color=a_color; gl_Position=u_ortho*vec4(a_pos,0.0,1.0); }
)";
static const char* FRAG = R"(
#version 310 es
precision mediump float;
in vec2 v_uv;
in vec4 v_color;
uniform sampler2D u_atlas;
out vec4 fragColor;
void main(){ fragColor=texture(u_atlas,v_uv)*v_color; }
)";

bool UIRenderer::init(int w,int h){
    m_w=w;m_h=h;
    GLuint vs,fs;
    vs=GLES.glCreateShader(GL_VERTEX_SHADER); fs=GLES.glCreateShader(GL_FRAGMENT_SHADER);
    GLES.glShaderSource(vs,1,&VERT,nullptr); GLES.glCompileShader(vs);
    GLES.glShaderSource(fs,1,&FRAG,nullptr); GLES.glCompileShader(fs);
    m_prog=GLES.glCreateProgram();
    GLES.glAttachShader(m_prog,vs); GLES.glAttachShader(m_prog,fs); GLES.glLinkProgram(m_prog);
    GLES.glDeleteShader(vs); GLES.glDeleteShader(fs);
    GLint ok=0; GLES.glGetProgramiv(m_prog,GL_LINK_STATUS,&ok);
    if(!ok){ MG_LOG_W("UIRenderer prog link failed"); GLES.glDeleteProgram(m_prog); m_prog=0; return false; }
    GLES.glGenBuffers(1,&m_vbo); GLES.glGenVertexArrays(1,&m_vao);
    GLES.glBindVertexArray(m_vao);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferData(GL_ARRAY_BUFFER,MAX_VERTS*(GLsizeiptr)sizeof(UIVertex),nullptr,GL_DYNAMIC_DRAW);
    GLsizei stride=(GLsizei)sizeof(UIVertex);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,stride,(void*)0);
    GLES.glEnableVertexAttribArray(1); GLES.glVertexAttribPointer(1,2,GL_FLOAT,GL_FALSE,stride,(void*)8);
    GLES.glEnableVertexAttribArray(2); GLES.glVertexAttribPointer(2,4,GL_FLOAT,GL_FALSE,stride,(void*)16);
    GLES.glBindVertexArray(0);
    m_verts.reserve(MAX_VERTS);
    MG_LOG_I("UIRenderer: prog=%u",m_prog);
    return true;
}

void UIRenderer::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_vbo) { GLES.glDeleteBuffers(1,&m_vbo); m_vbo=0; }
    if(m_vao) { GLES.glDeleteVertexArrays(1,&m_vao); m_vao=0; }
    m_verts.clear();
}

void UIRenderer::beginFrame(int w,int h){
    m_w=w;m_h=h;
    m_verts.clear();
    // Ortho matrix [0,w] x [0,h]
    float L=0,R=(float)w,T=0,B=(float)h;
    float* m=m_ortho;
    memset(m,0,64);
    m[0]=2.0f/(R-L); m[5]=2.0f/(T-B); m[10]=-1.0f;
    m[12]=-(R+L)/(R-L); m[13]=-(T+B)/(T-B); m[15]=1.0f;
}

void UIRenderer::drawQuad(float x,float y,float w,float h,
                           float u0,float v0,float u1,float v1,
                           float r,float g,float b,float a){
    if((int)m_verts.size()+6>MAX_VERTS) return;
    UIVertex verts[6]={
        {x,   y,   u0,v0,r,g,b,a},
        {x+w, y,   u1,v0,r,g,b,a},
        {x,   y+h, u0,v1,r,g,b,a},
        {x+w, y,   u1,v0,r,g,b,a},
        {x+w, y+h, u1,v1,r,g,b,a},
        {x,   y+h, u0,v1,r,g,b,a}
    };
    for(auto& v:verts) m_verts.push_back(v);
}

void UIRenderer::flush(GLuint atlas){
    if(!m_prog||m_verts.empty()) return;
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferSubData(GL_ARRAY_BUFFER,0,(GLsizeiptr)(m_verts.size()*sizeof(UIVertex)),m_verts.data());
    GLES.glUseProgram(m_prog);
    GLES.glUniformMatrix4fv(GLES.glGetUniformLocation(m_prog,"u_ortho"),1,GL_FALSE,m_ortho);
    GLES.glActiveTexture(GL_TEXTURE0);
    GLES.glBindTexture(GL_TEXTURE_2D,atlas);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_atlas"),0);
    GLES.glEnable(GL_BLEND); GLES.glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glBindVertexArray(m_vao);
    GLES.glDrawArrays(GL_TRIANGLES,0,(GLsizei)m_verts.size());
    GLES.glBindVertexArray(0);
    GLES.glDisable(GL_BLEND);
    m_verts.clear();
}
} // MG
#endif
