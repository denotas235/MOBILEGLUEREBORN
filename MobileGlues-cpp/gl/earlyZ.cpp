// MobileGlues - gl/earlyZ.cpp
// Early-Z Depth Pre-Pass
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "earlyZ.h"
#include "../gles/loader.h"
#include "log.h"

namespace MG {

EarlyZPass& earlyZ(){ static EarlyZPass s; return s; }

void EarlyZPass::create(int w,int h){
    if(m_fbo){GLES.glDeleteFramebuffers(1,&m_fbo);m_fbo=0;GLES.glDeleteTextures(1,&m_tex);m_tex=0;}
    m_w=w; m_h=h;
    GLES.glGenTextures(1,&m_tex);
    GLES.glBindTexture(GL_TEXTURE_2D,m_tex);
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT24,w,h,0,GL_DEPTH_COMPONENT,GL_UNSIGNED_INT,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    GLES.glGenFramebuffers(1,&m_fbo);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,m_tex,0);
    GLenum st=GLES.glCheckFramebufferStatus(GL_FRAMEBUFFER);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    if(st!=GL_FRAMEBUFFER_COMPLETE){
        MG_LOG_E("EarlyZ FBO incomplete 0x%x",st);
        GLES.glDeleteFramebuffers(1,&m_fbo);m_fbo=0;
        GLES.glDeleteTextures(1,&m_tex);m_tex=0;
        return;
    }
    MG_LOG_I("EarlyZ: %dx%d ready",w,h);
}

bool EarlyZPass::init(int w,int h){ if(w>0&&h>0)create(w,h); return isAvailable(); }
void EarlyZPass::resize(int w,int h){ if(w!=m_w||h!=m_h)create(w,h); }
void EarlyZPass::destroy(){
    if(m_fbo){GLES.glDeleteFramebuffers(1,&m_fbo);m_fbo=0;}
    if(m_tex){GLES.glDeleteTextures(1,&m_tex);m_tex=0;}
}

void EarlyZPass::beginPrePass(){
    if(!m_fbo)return;
    GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&m_savedFBO);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glClear(GL_DEPTH_BUFFER_BIT);
    GLES.glColorMask(GL_FALSE,GL_FALSE,GL_FALSE,GL_FALSE);
    GLES.glDepthMask(GL_TRUE);
    GLES.glEnable(GL_DEPTH_TEST);
    GLES.glDepthFunc(GL_LESS);
}

void EarlyZPass::endPrePass(){
    if(!m_fbo)return;
    GLES.glColorMask(GL_TRUE,GL_TRUE,GL_TRUE,GL_TRUE);
    GLES.glDepthMask(GL_FALSE);
    GLES.glDepthFunc(GL_LEQUAL);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)m_savedFBO);
}

void EarlyZPass::bindDepthTex(int unit){
    if(!m_tex)return;
    GLES.glActiveTexture(GL_TEXTURE0+unit);
    GLES.glBindTexture(GL_TEXTURE_2D,m_tex);
}

} // MG
#endif
