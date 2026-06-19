// MobileGlues - effects/reflections_ssr.cpp
// SSR: ray-march in screen space, works best on water/glass surfaces
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "reflections_ssr.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>

namespace MG {
SSReflections& ssr(){ static SSReflections s; return s; }

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
uniform sampler2D u_scene;
uniform sampler2D u_depth;
uniform mat4      u_proj;
uniform mat4      u_invView;
uniform vec2      u_screen;
out vec4 fragColor;

vec3 reconstructPos(vec2 uv,float d){
    vec4 clip=vec4(uv*2.0-1.0,d*2.0-1.0,1.0);
    vec4 view=inverse(u_proj)*clip;
    view.xyz/=view.w;
    return (u_invView*vec4(view.xyz,0.0)).xyz;
}

void main(){
    float depth=texture(u_depth,v_uv).r;
    if(depth>0.999){ fragColor=vec4(0.0); return; }

    // Simplified SSR: reflect up vector, march 16 steps
    vec3 pos=reconstructPos(v_uv,depth);
    vec3 N=vec3(0.0,1.0,0.0); // flat normal (water/glass approximation)
    vec3 V=normalize(-pos);
    vec3 R=reflect(-V,N);

    vec2 hit=v_uv;
    vec3 col=vec3(0.0);
    float alpha=0.0;
    const int STEPS=16;
    for(int i=1;i<=STEPS;i++){
        vec3 samplePos=pos+R*float(i)*0.3;
        // Project sample back to screen
        vec4 proj=u_proj*vec4(samplePos,1.0);
        if(proj.w<=0.0) break;
        proj.xyz/=proj.w;
        vec2 uv2=proj.xy*0.5+0.5;
        if(uv2.x<0.0||uv2.x>1.0||uv2.y<0.0||uv2.y>1.0) break;
        float sd=texture(u_depth,uv2).r;
        if(proj.z*0.5+0.5>sd+0.001){
            col=texture(u_scene,uv2).rgb;
            alpha=1.0-float(i)/float(STEPS);
            // Fade at screen edges
            vec2 e=abs(uv2-0.5)*2.0;
            alpha*=1.0-smoothstep(0.7,1.0,max(e.x,e.y));
            break;
        }
    }
    fragColor=vec4(col,alpha*0.5);
}
)";

static GLuint mkS(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("SSR: %s",l);GLES.glDeleteShader(sh);return 0;}
    return sh;
}
static GLuint mkP(GLuint v,GLuint f){
    if(!v||!f) return 0;
    GLuint p=GLES.glCreateProgram();
    GLES.glAttachShader(p,v); GLES.glAttachShader(p,f); GLES.glLinkProgram(p);
    GLint ok=0; GLES.glGetProgramiv(p,GL_LINK_STATUS,&ok);
    if(!ok){GLES.glDeleteProgram(p);return 0;}
    return p;
}

void SSReflections::createResources(int w,int h){
    if(m_ssrTex){ GLES.glDeleteTextures(1,&m_ssrTex); m_ssrTex=0; }
    if(m_fbo)   { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    m_w=w/2; m_h=h/2; // Half-res SSR
    GLES.glGenTextures(1,&m_ssrTex);
    GLES.glBindTexture(GL_TEXTURE_2D,m_ssrTex);
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,m_w,m_h,0,GL_RGBA,GL_UNSIGNED_BYTE,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    GLES.glGenFramebuffers(1,&m_fbo);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_ssrTex,0);
    GLenum st=GLES.glCheckFramebufferStatus(GL_FRAMEBUFFER);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    if(st!=GL_FRAMEBUFFER_COMPLETE){ GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
}

bool SSReflections::init(int w,int h){
    if(m_prog) return true;
    GLuint vs=mkS(GL_VERTEX_SHADER,VERT);
    GLuint fs=mkS(GL_FRAGMENT_SHADER,FRAG);
    m_prog=mkP(vs,fs);
    if(vs) GLES.glDeleteShader(vs);
    if(fs) GLES.glDeleteShader(fs);
    if(!m_prog){ MG_LOG_W("SSR prog failed"); return false; }
    static const float Q[]={-1,-1,1,-1,-1,1,1,1};
    GLES.glGenVertexArrays(1,&m_qVAO); GLES.glBindVertexArray(m_qVAO);
    GLES.glGenBuffers(1,&m_qVBO); GLES.glBindBuffer(GL_ARRAY_BUFFER,m_qVBO);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(Q),Q,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);
    createResources(w,h);
    MG_LOG_I("SSR: %dx%d",m_w,m_h);
    return m_fbo!=0;
}
void SSReflections::resize(int w,int h){ if(w/2!=m_w||h/2!=m_h) createResources(w,h); }
void SSReflections::destroy(){
    if(m_prog)  { GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_ssrTex){ GLES.glDeleteTextures(1,&m_ssrTex); m_ssrTex=0; }
    if(m_fbo)   { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_qVBO)  { GLES.glDeleteBuffers(1,&m_qVBO); m_qVBO=0; }
    if(m_qVAO)  { GLES.glDeleteVertexArrays(1,&m_qVAO); m_qVAO=0; }
}
bool SSReflections::run(GLuint sceneTex,GLuint depthTex,const float* projMat,const float* invViewMat){
    if(!m_prog||!m_fbo) return false;
    GLint savedFBO=0; GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&savedFBO);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glViewport(0,0,m_w,m_h);
    GLES.glUseProgram(m_prog);
    GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,sceneTex);
    GLES.glActiveTexture(GL_TEXTURE1); GLES.glBindTexture(GL_TEXTURE_2D,depthTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_scene"),0);
    GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_depth"),1);
    if(projMat)    GLES.glUniformMatrix4fv(GLES.glGetUniformLocation(m_prog,"u_proj"),1,GL_FALSE,projMat);
    if(invViewMat) GLES.glUniformMatrix4fv(GLES.glGetUniformLocation(m_prog,"u_invView"),1,GL_FALSE,invViewMat);
    GLES.glUniform2f(GLES.glGetUniformLocation(m_prog,"u_screen"),(float)m_w,(float)m_h);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glBindVertexArray(m_qVAO);
    GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
    GLES.glBindVertexArray(0);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)savedFBO);
    return true;
}
} // MG
#endif
