// MobileGlues - lighting/ambient_occlusion.cpp
// HBAO-Lite: 8-sample hemisphere AO, stored in R8 texture
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "ambient_occlusion.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"
#include <cmath>
#include <algorithm>

namespace MG {
AmbientOcclusion& ao(){ static AmbientOcclusion s; return s; }

static const char* VERT = R"(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
out vec2 v_uv;
void main(){ v_uv=a_pos*0.5+0.5; gl_Position=vec4(a_pos,0.0,1.0); }
)";

static const char* FRAG = R"(
#version 310 es
precision highp float;
in vec2 v_uv;
uniform sampler2D u_depth;
uniform vec2      u_invScreen;
uniform vec2      u_projScale; // (1/tanHalfFovX, 1/tanHalfFovY)
out vec4 fragColor;

// 8 rotated hemisphere samples (HBAO pattern)
const vec2 KERNEL[8] = vec2[8](
    vec2( 1.0, 0.0), vec2(-1.0, 0.0), vec2( 0.0, 1.0), vec2( 0.0,-1.0),
    vec2( 0.707, 0.707), vec2(-0.707, 0.707),
    vec2( 0.707,-0.707), vec2(-0.707,-0.707)
);
const float RADIUS = 0.3;
const int   SAMPLES = 8;

float getDepth(vec2 uv){ return texture(u_depth, clamp(uv,0.0,1.0)).r; }

void main(){
    float depth = getDepth(v_uv);
    if(depth >= 0.9999){ fragColor=vec4(1.0); return; }

    float ao = 0.0;
    for(int i=0;i<SAMPLES;i++){
        vec2 off = KERNEL[i] * RADIUS * u_invScreen * 8.0;
        float sd = getDepth(v_uv + off);
        float delta = depth - sd;
        // Contribution is high if sample is above horizon
        ao += clamp(delta * 10.0, 0.0, 1.0) * (1.0 - clamp(abs(delta)*5.0,0.0,1.0));
    }
    ao = 1.0 - clamp(ao / float(SAMPLES) * 1.5, 0.0, 0.85);
    fragColor = vec4(ao, ao, ao, 1.0);
}
)";

static GLuint compShader(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("AO shader: %s",l);GLES.glDeleteShader(sh);return 0;}
    return sh;
}
static GLuint linkProg(GLuint v,GLuint f){
    if(!v||!f) return 0;
    GLuint p=GLES.glCreateProgram();
    GLES.glAttachShader(p,v); GLES.glAttachShader(p,f); GLES.glLinkProgram(p);
    GLint ok=0; GLES.glGetProgramiv(p,GL_LINK_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetProgramInfoLog(p,256,nullptr,l);MG_LOG_W("AO prog: %s",l);GLES.glDeleteProgram(p);return 0;}
    return p;
}

void AmbientOcclusion::createResources(int w,int h){
    if(m_aoTex){ GLES.glDeleteTextures(1,&m_aoTex); m_aoTex=0; }
    if(m_fbo)  { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    m_w=w; m_h=h;
    GLES.glGenTextures(1,&m_aoTex);
    GLES.glBindTexture(GL_TEXTURE_2D,m_aoTex);
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_R8,w,h,0,GL_RED,GL_UNSIGNED_BYTE,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    GLES.glGenFramebuffers(1,&m_fbo);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_aoTex,0);
    GLenum st=GLES.glCheckFramebufferStatus(GL_FRAMEBUFFER);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    if(st!=GL_FRAMEBUFFER_COMPLETE){ MG_LOG_W("AO FBO incomplete"); GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
}

bool AmbientOcclusion::init(int w,int h){
    if(m_prog) return true;
    GLuint vs=compShader(GL_VERTEX_SHADER,VERT);
    GLuint fs=compShader(GL_FRAGMENT_SHADER,FRAG);
    m_prog=linkProg(vs,fs);
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
    MG_LOG_I("AO: %dx%d prog=%u",w,h,m_prog);
    return m_fbo!=0;
}
void AmbientOcclusion::resize(int w,int h){ if(w!=m_w||h!=m_h) createResources(w,h); }
void AmbientOcclusion::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_aoTex){ GLES.glDeleteTextures(1,&m_aoTex); m_aoTex=0; }
    if(m_fbo)  { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_qVBO) { GLES.glDeleteBuffers(1,&m_qVBO); m_qVBO=0; }
    if(m_qVAO) { GLES.glDeleteVertexArrays(1,&m_qVAO); m_qVAO=0; }
}
bool AmbientOcclusion::run(GLuint depthTex,GLuint /*normalTex*/,float /*projX*/,float /*projY*/){
    if(!m_prog||!m_fbo) return false;
    GLint savedFBO=0; GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&savedFBO);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glUseProgram(m_prog);
    GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,depthTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_depth"),0);
    GLES.glUniform2f(GLES.glGetUniformLocation(m_prog,"u_invScreen"),1.0f/m_w,1.0f/m_h);
    GLES.glUniform2f(GLES.glGetUniformLocation(m_prog,"u_projScale"),1.0f,1.0f);
    GLES.glDisable(GL_DEPTH_TEST); GLES.glDisable(GL_BLEND);
    GLES.glBindVertexArray(m_qVAO);
    GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    GLES.glBindVertexArray(0);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)savedFBO);
    return true;
}
} // MG
#endif
