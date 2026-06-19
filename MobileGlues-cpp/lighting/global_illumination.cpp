// MobileGlues - lighting/global_illumination.cpp
// Screen-space GI: blurred low-freq irradiance, multiplied into scene
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "global_illumination.h"
#include "log.h"
#include "../gles/loader.h"

namespace MG {
GlobalIllumination& gi(){ static GlobalIllumination s; return s; }

static const char* VERT = R"(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
out vec2 v_uv;
void main(){ v_uv=a_pos*0.5+0.5; gl_Position=vec4(a_pos,0.0,1.0); }
)";

// Simple 9-tap box blur downsampled to half res, used as diffuse irradiance
static const char* FRAG = R"(
#version 310 es
precision mediump float;
in vec2 v_uv;
uniform sampler2D u_scene;
uniform sampler2D u_ao;
uniform vec2      u_inv;
out vec4 fragColor;
void main(){
    vec3 acc=vec3(0.0);
    for(int x=-2;x<=2;x++) for(int y=-2;y<=2;y++){
        acc+=texture(u_scene,v_uv+vec2(x,y)*u_inv*3.0).rgb;
    }
    acc/=25.0;
    float aoV=texture(u_ao,v_uv).r;
    fragColor=vec4(acc*aoV*0.12,1.0); // 12% indirect bounce
}
)";

static GLuint mkShader(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("GI: %s",l);GLES.glDeleteShader(sh);return 0;}
    return sh;
}
static GLuint mkProg(GLuint v,GLuint f){
    if(!v||!f) return 0;
    GLuint p=GLES.glCreateProgram();
    GLES.glAttachShader(p,v); GLES.glAttachShader(p,f); GLES.glLinkProgram(p);
    GLint ok=0; GLES.glGetProgramiv(p,GL_LINK_STATUS,&ok);
    if(!ok){GLES.glDeleteProgram(p);return 0;}
    return p;
}

void GlobalIllumination::createResources(int w,int h){
    if(m_giTex){ GLES.glDeleteTextures(1,&m_giTex); m_giTex=0; }
    if(m_fbo)  { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    m_w=w/2; m_h=h/2; // Half res GI
    GLES.glGenTextures(1,&m_giTex);
    GLES.glBindTexture(GL_TEXTURE_2D,m_giTex);
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_RGB8,m_w,m_h,0,GL_RGB,GL_UNSIGNED_BYTE,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    GLES.glGenFramebuffers(1,&m_fbo);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_giTex,0);
    GLenum st=GLES.glCheckFramebufferStatus(GL_FRAMEBUFFER);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    if(st!=GL_FRAMEBUFFER_COMPLETE){ GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
}

bool GlobalIllumination::init(int w,int h){
    if(m_prog) return true;
    GLuint vs=mkShader(GL_VERTEX_SHADER,VERT);
    GLuint fs=mkShader(GL_FRAGMENT_SHADER,FRAG);
    m_prog=mkProg(vs,fs);
    if(vs) GLES.glDeleteShader(vs);
    if(fs) GLES.glDeleteShader(fs);
    if(!m_prog) return false;
    static const float Q[]={-1,-1,1,-1,-1,1,1,1};
    GLES.glGenVertexArrays(1,&m_qVAO); GLES.glBindVertexArray(m_qVAO);
    GLES.glGenBuffers(1,&m_qVBO); GLES.glBindBuffer(GL_ARRAY_BUFFER,m_qVBO);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(Q),Q,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);
    createResources(w,h);
    return m_fbo!=0;
}
void GlobalIllumination::resize(int w,int h){ if(w/2!=m_w||h/2!=m_h) createResources(w,h); }
void GlobalIllumination::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_giTex){ GLES.glDeleteTextures(1,&m_giTex); m_giTex=0; }
    if(m_fbo)  { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_qVBO) { GLES.glDeleteBuffers(1,&m_qVBO); m_qVBO=0; }
    if(m_qVAO) { GLES.glDeleteVertexArrays(1,&m_qVAO); m_qVAO=0; }
}
bool GlobalIllumination::run(GLuint sceneTex,GLuint aoTex){
    if(!m_prog||!m_fbo) return false;
    GLint savedFBO=0; GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&savedFBO);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glViewport(0,0,m_w,m_h);
    GLES.glUseProgram(m_prog);
    GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,sceneTex);
    GLES.glActiveTexture(GL_TEXTURE1); GLES.glBindTexture(GL_TEXTURE_2D,aoTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_scene"),0);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_ao"),1);
    GLES.glUniform2f(GLES.glGetUniformLocation(m_prog,"u_inv"),1.0f/m_w,1.0f/m_h);
    GLES.glDisable(GL_DEPTH_TEST); GLES.glDisable(GL_BLEND);
    GLES.glBindVertexArray(m_qVAO);
    GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    GLES.glBindVertexArray(0);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)savedFBO);
    return true;
}
} // MG
#endif
