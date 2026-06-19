// MobileGlues - gpu_driven/hzb_occlusion.cpp
// Builds a hi-z mip pyramid; tests chunk AABBs without stalling the GPU
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "hzb_occlusion.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>
#include <cmath>
#include <algorithm>

namespace MG {

HZBOcclusion& hzb(){ static HZBOcclusion s; return s; }

// Minify depth: max of 2x2 region → conservative occlusion
static const char* VERT_QUAD = R"(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
out vec2 v_uv;
void main(){ v_uv=a_pos*0.5+0.5; gl_Position=vec4(a_pos,0.0,1.0); }
)";

static const char* FRAG_HZB = R"(
#version 310 es
precision highp float;
in vec2 v_uv;
uniform sampler2D u_depth;
uniform vec2 u_invSize;
out vec4 fragColor;
void main(){
    vec2 tc = v_uv;
    float d0 = texture(u_depth, tc + vec2(-u_invSize.x,-u_invSize.y)*0.5).r;
    float d1 = texture(u_depth, tc + vec2( u_invSize.x,-u_invSize.y)*0.5).r;
    float d2 = texture(u_depth, tc + vec2(-u_invSize.x, u_invSize.y)*0.5).r;
    float d3 = texture(u_depth, tc + vec2( u_invSize.x, u_invSize.y)*0.5).r;
    fragColor = vec4(max(max(d0,d1),max(d2,d3)));
}
)";

static GLuint compileShader(GLenum t,const char* src){
    GLuint s=GLES.glCreateShader(t); if(!s) return 0;
    GLES.glShaderSource(s,1,&src,nullptr); GLES.glCompileShader(s);
    GLint ok=0; GLES.glGetShaderiv(s,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(s,256,nullptr,l);MG_LOG_W("HZB shader: %s",l);GLES.glDeleteShader(s);return 0;}
    return s;
}
static GLuint linkProg(GLuint v,GLuint f){
    if(!v||!f) return 0;
    GLuint p=GLES.glCreateProgram();
    GLES.glAttachShader(p,v); GLES.glAttachShader(p,f); GLES.glLinkProgram(p);
    GLint ok=0; GLES.glGetProgramiv(p,GL_LINK_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetProgramInfoLog(p,256,nullptr,l);MG_LOG_W("HZB prog: %s",l);GLES.glDeleteProgram(p);return 0;}
    return p;
}

bool HZBOcclusion::init(int w,int h){
    if(m_prog) return true;
    GLuint vs=compileShader(GL_VERTEX_SHADER,VERT_QUAD);
    GLuint fs=compileShader(GL_FRAGMENT_SHADER,FRAG_HZB);
    m_prog=linkProg(vs,fs);
    if(vs) GLES.glDeleteShader(vs);
    if(fs) GLES.glDeleteShader(fs);
    if(!m_prog) return false;

    static const float Q[]={-1,-1,1,-1,-1,1,1,1};
    GLES.glGenVertexArrays(1,&m_quadVAO); GLES.glBindVertexArray(m_quadVAO);
    GLES.glGenBuffers(1,&m_quadVBO); GLES.glBindBuffer(GL_ARRAY_BUFFER,m_quadVBO);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(Q),Q,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);

    createResources(w,h);
    MG_LOG_I("HZB: %dx%d levels=%d prog=%u",w,h,HZB_LEVELS,m_prog);
    return true;
}

void HZBOcclusion::createResources(int w,int h){
    if(m_hzb){ GLES.glDeleteTextures(1,&m_hzb); m_hzb=0; }
    if(m_fbo){ GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    m_w=w; m_h=h;
    // HZB texture: R32F mip chain
    GLES.glGenTextures(1,&m_hzb);
    GLES.glBindTexture(GL_TEXTURE_2D,m_hzb);
    GLES.glTexStorage2D(GL_TEXTURE_2D,HZB_LEVELS,GL_R32F,w,h);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST_MIPMAP_NEAREST);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    GLES.glBindTexture(GL_TEXTURE_2D,0);
    GLES.glGenFramebuffers(1,&m_fbo);
}

void HZBOcclusion::resize(int w,int h){ if(w!=m_w||h!=m_h) createResources(w,h); }

void HZBOcclusion::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_hzb) { GLES.glDeleteTextures(1,&m_hzb); m_hzb=0; }
    if(m_fbo) { GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_quadVBO){ GLES.glDeleteBuffers(1,&m_quadVBO); m_quadVBO=0; }
    if(m_quadVAO){ GLES.glDeleteVertexArrays(1,&m_quadVAO); m_quadVAO=0; }
}

void HZBOcclusion::buildHZB(GLuint depthTex){
    if(!m_prog||!m_hzb||!m_fbo) return;
    GLES.glUseProgram(m_prog);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glColorMask(GL_TRUE,GL_TRUE,GL_TRUE,GL_TRUE);

    int w=m_w, h=m_h;
    GLuint srcTex=depthTex;
    for(int level=0;level<HZB_LEVELS;level++){
        w=std::max(1,w/2); h=std::max(1,h/2);
        GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,m_hzb,level);
        GLES.glViewport(0,0,w,h);
        GLES.glActiveTexture(GL_TEXTURE0);
        GLES.glBindTexture(GL_TEXTURE_2D,srcTex);
        GLES.glUniform1i(GLES.glGetUniformLocation(m_prog,"u_depth"),0);
        GLES.glUniform2f(GLES.glGetUniformLocation(m_prog,"u_invSize"),1.0f/w,1.0f/h);
        GLES.glBindVertexArray(m_quadVAO);
        GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4);
        srcTex=m_hzb; // Next level reads from hzb
    }
    GLES.glBindVertexArray(0);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
}

bool HZBOcclusion::testAABB(float minX,float minY,float minZ,
                              float maxX,float maxY,float maxZ,
                              const float* vp){
    if(!m_prog) return true; // conservative: always visible
    // Transform 8 corners of AABB through view-proj, find screen min/max
    float corners[8][3]={
        {minX,minY,minZ},{maxX,minY,minZ},{minX,maxY,minZ},{maxX,maxY,minZ},
        {minX,minY,maxZ},{maxX,minY,maxZ},{minX,maxY,maxZ},{maxX,maxY,maxZ}
    };
    float sMinX=1e9f,sMinY=1e9f,sMaxX=-1e9f,sMaxY=-1e9f,sMinZ=1e9f;
    bool allBehind=true;
    for(auto& c:corners){
        float cx=vp[0]*c[0]+vp[4]*c[1]+vp[8] *c[2]+vp[12];
        float cy=vp[1]*c[0]+vp[5]*c[1]+vp[9] *c[2]+vp[13];
        float cz=vp[2]*c[0]+vp[6]*c[1]+vp[10]*c[2]+vp[14];
        float cw=vp[3]*c[0]+vp[7]*c[1]+vp[11]*c[2]+vp[15];
        if(cw<=0.0f) continue;
        allBehind=false;
        float ndx=cx/cw, ndy=cy/cw, ndz=cz/cw;
        if(ndx<sMinX) sMinX=ndx; if(ndx>sMaxX) sMaxX=ndx;
        if(ndy<sMinY) sMinY=ndy; if(ndy>sMaxY) sMaxY=ndy;
        if(ndz<sMinZ) sMinZ=ndz;
    }
    if(allBehind) return false;
    // Clamp to screen
    sMinX=std::max(-1.0f,sMinX); sMinY=std::max(-1.0f,sMinY);
    sMaxX=std::min( 1.0f,sMaxX); sMaxY=std::min( 1.0f,sMaxY);
    if(sMaxX<-1.0f||sMaxY<-1.0f||sMinX>1.0f||sMinY>1.0f) return false;
    // Choose mip level based on projected size
    float pw=(sMaxX-sMinX)*m_w*0.5f;
    float ph=(sMaxY-sMinY)*m_h*0.5f;
    float diag=std::sqrt(pw*pw+ph*ph);
    int mip=std::min((int)std::log2(std::max(diag,1.0f)),HZB_LEVELS-1);
    // Sample HZB at chosen mip
    float uvx=(sMinX+sMaxX)*0.5f*0.5f+0.5f;
    float uvy=(sMinY+sMaxY)*0.5f*0.5f+0.5f;
    // CPU-side HZB read would need PBO — for now: conservative pass
    (void)mip;(void)uvx;(void)uvy;
    return sMinZ < 1.0f; // not clipped to far plane
}

} // MG
#endif
