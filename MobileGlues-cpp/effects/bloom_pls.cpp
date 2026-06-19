// MobileGlues - effects/bloom_pls.cpp
// Kawase bloom: downsample 4 levels, upsample with additive blend
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "bloom_pls.h"
#include "log.h"
#include "../gles/loader.h"
#include <algorithm>

namespace MG {
BloomPLS& bloom(){ static BloomPLS s; return s; }

static const char* VERT = R"(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
out vec2 v_uv;
void main(){ v_uv=a_pos*0.5+0.5; gl_Position=vec4(a_pos,0.0,1.0); }
)";

// Downsample with luminance threshold
static const char* FRAG_DOWN = R"(
#version 310 es
precision mediump float;
in vec2 v_uv;
uniform sampler2D u_src;
uniform vec2      u_inv;
uniform float     u_thresh;
out vec4 fragColor;
void main(){
    vec3 c = texture(u_src,v_uv).rgb;
    float lum=dot(c,vec3(0.2126,0.7152,0.0722));
    c*=max(0.0,lum-u_thresh)/(lum+0.001);
    // 4-tap Kawase
    vec3 s = c;
    s += texture(u_src,v_uv+vec2( u_inv.x, u_inv.y)).rgb;
    s += texture(u_src,v_uv+vec2(-u_inv.x, u_inv.y)).rgb;
    s += texture(u_src,v_uv+vec2( u_inv.x,-u_inv.y)).rgb;
    s += texture(u_src,v_uv+vec2(-u_inv.x,-u_inv.y)).rgb;
    fragColor = vec4(s*0.2,1.0);
}
)";

// Upsample: additive blur
static const char* FRAG_UP = R"(
#version 310 es
precision mediump float;
in vec2 v_uv;
uniform sampler2D u_src;
uniform vec2      u_inv;
uniform float     u_intensity;
out vec4 fragColor;
void main(){
    vec3 s = texture(u_src,v_uv).rgb;
    s += texture(u_src,v_uv+vec2( u_inv.x, 0.0)).rgb;
    s += texture(u_src,v_uv+vec2(-u_inv.x, 0.0)).rgb;
    s += texture(u_src,v_uv+vec2( 0.0, u_inv.y)).rgb;
    s += texture(u_src,v_uv+vec2( 0.0,-u_inv.y)).rgb;
    fragColor = vec4(s*0.2*u_intensity,1.0);
}
)";

static GLuint mkShader(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("Bloom: %s",l);GLES.glDeleteShader(sh);return 0;}
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

void BloomPLS::createResources(int w,int h){
    for(int i=0;i<BLOOM_PASSES;i++){
        if(m_chain[i]){ GLES.glDeleteTextures(1,&m_chain[i]); m_chain[i]=0; }
        if(m_fbos[i]) { GLES.glDeleteFramebuffers(1,&m_fbos[i]); m_fbos[i]=0; }
    }
    m_w=w; m_h=h;
    int cw=w,ch=h;
    for(int i=0;i<BLOOM_PASSES;i++){
        cw=std::max(1,cw/2); ch=std::max(1,ch/2);
        GLES.glGenTextures(1,&m_chain[i]);
        GLES.glBindTexture(GL_TEXTURE_2D,m_chain[i]);
        GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_RGB8,cw,ch,0,GL_RGB,GL_UNSIGNED_BYTE,nullptr);
        GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
        GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
        GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
        GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
        GLES.glGenFramebuffers(1,&m_fbos[i]);
        GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbos[i]);
        GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_chain[i],0);
        GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    }
}

bool BloomPLS::init(int w,int h){
    if(m_progDown) return true;
    GLuint vs=mkShader(GL_VERTEX_SHADER,VERT);
    GLuint fdw=mkShader(GL_FRAGMENT_SHADER,FRAG_DOWN);
    GLuint fup=mkShader(GL_FRAGMENT_SHADER,FRAG_UP);
    m_progDown=mkProg(vs,fdw);
    m_progUp  =mkProg(vs,fup);
    if(vs) GLES.glDeleteShader(vs);
    if(fdw) GLES.glDeleteShader(fdw);
    if(fup) GLES.glDeleteShader(fup);
    if(!m_progDown||!m_progUp){ MG_LOG_W("Bloom: prog creation failed"); return false; }
    static const float Q[]={-1,-1,1,-1,-1,1,1,1};
    GLES.glGenVertexArrays(1,&m_qVAO); GLES.glBindVertexArray(m_qVAO);
    GLES.glGenBuffers(1,&m_qVBO); GLES.glBindBuffer(GL_ARRAY_BUFFER,m_qVBO);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(Q),Q,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);
    createResources(w,h);
    MG_LOG_I("Bloom: %dx%d %d passes",w,h,BLOOM_PASSES);
    return true;
}
void BloomPLS::resize(int w,int h){ if(w!=m_w||h!=m_h) createResources(w,h); }
void BloomPLS::destroy(){
    if(m_progDown){ GLES.glDeleteProgram(m_progDown); m_progDown=0; }
    if(m_progUp)  { GLES.glDeleteProgram(m_progUp);   m_progUp=0;   }
    for(int i=0;i<BLOOM_PASSES;i++){
        if(m_chain[i]){ GLES.glDeleteTextures(1,&m_chain[i]); m_chain[i]=0; }
        if(m_fbos[i]) { GLES.glDeleteFramebuffers(1,&m_fbos[i]); m_fbos[i]=0; }
    }
    if(m_qVBO){ GLES.glDeleteBuffers(1,&m_qVBO); m_qVBO=0; }
    if(m_qVAO){ GLES.glDeleteVertexArrays(1,&m_qVAO); m_qVAO=0; }
}

bool BloomPLS::run(GLuint sceneTex,float threshold,float intensity){
    if(!m_progDown||!m_fbos[0]) return false;
    GLint savedFBO=0; GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&savedFBO);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glDisable(GL_BLEND);
    // Downsample pass
    GLES.glUseProgram(m_progDown);
    GLuint src=sceneTex;
    int cw=m_w,ch=m_h;
    for(int i=0;i<BLOOM_PASSES;i++){
        cw=std::max(1,cw/2); ch=std::max(1,ch/2);
        GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbos[i]);
        GLES.glViewport(0,0,cw,ch);
        GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,src);
        GLES.glUniform1i(GLES.glGetUniformLocation(m_progDown,"u_src"),0);
        GLES.glUniform2f(GLES.glGetUniformLocation(m_progDown,"u_inv"),1.0f/cw,1.0f/ch);
        GLES.glUniform1f(GLES.glGetUniformLocation(m_progDown,"u_thresh"),i==0?threshold:0.0f);
        GLES.glBindVertexArray(m_qVAO);
        GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        src=m_chain[i];
    }
    // Upsample pass (additive)
    GLES.glUseProgram(m_progUp);
    GLES.glEnable(GL_BLEND);
    GLES.glBlendFunc(GL_ONE,GL_ONE);
    cw=m_w/2; ch=m_h/2;
    for(int i=BLOOM_PASSES-1;i>=1;i--){
        cw=std::max(1,cw*2); ch=std::max(1,ch*2);
        // Upsample i into i-1
        GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbos[i-1]);
        GLES.glViewport(0,0,cw,ch);
        GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,m_chain[i]);
        GLES.glUniform1i(GLES.glGetUniformLocation(m_progUp,"u_src"),0);
        GLES.glUniform2f(GLES.glGetUniformLocation(m_progUp,"u_inv"),1.0f/cw,1.0f/ch);
        GLES.glUniform1f(GLES.glGetUniformLocation(m_progUp,"u_intensity"),intensity);
        GLES.glBindVertexArray(m_qVAO);
        GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    }
    GLES.glBindVertexArray(0);
    GLES.glDisable(GL_BLEND);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)savedFBO);
    return true;
}
} // MG
#endif
