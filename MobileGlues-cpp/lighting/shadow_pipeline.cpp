// MobileGlues - lighting/shadow_pipeline.cpp
// PCF Shadow Map - Depth-only FBO, sampled with GL_EXT_shadow_samplers
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "shadow_pipeline.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>

namespace MG {

ShadowPipeline& shadowPipeline(){ static ShadowPipeline s; return s; }

void ShadowPipeline::createResources(int sz){
    if(m_fbo){ GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_tex){ GLES.glDeleteTextures(1,&m_tex); m_tex=0; }
    m_size=sz;

    GLES.glGenTextures(1,&m_tex);
    GLES.glBindTexture(GL_TEXTURE_2D,m_tex);
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_DEPTH_COMPONENT24,sz,sz,0,
                      GL_DEPTH_COMPONENT,GL_UNSIGNED_INT,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    // PCF compare mode (GL_EXT_shadow_samplers)
    if(has_extension("GL_EXT_shadow_samplers")){
        GLES.glTexParameteri(GL_TEXTURE_2D,0x884Cu,GL_COMPARE_REF_TO_TEXTURE); // TEXTURE_COMPARE_MODE
        GLES.glTexParameteri(GL_TEXTURE_2D,0x884Du,GL_LEQUAL);                 // TEXTURE_COMPARE_FUNC
    }
    GLES.glBindTexture(GL_TEXTURE_2D,0);

    GLES.glGenFramebuffers(1,&m_fbo);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glFramebufferTexture2D(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_TEXTURE_2D,m_tex,0);
    // No color attachment needed — depth-only pass
    GLenum drawBuf=GL_NONE;
    // glDrawBuffers is GLES3 but no color attachment is default
    GLenum st=GLES.glCheckFramebufferStatus(GL_FRAMEBUFFER);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,0);
    if(st!=GL_FRAMEBUFFER_COMPLETE){
        MG_LOG_W("ShadowPipeline FBO incomplete 0x%x (sz=%d), disabling shadows",st,sz);
        GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0;
        GLES.glDeleteTextures(1,&m_tex); m_tex=0;
        return;
    }
    MG_LOG_I("ShadowPipeline: %dx%d depth FBO OK, PCF=%d",sz,sz,
             (int)has_extension("GL_EXT_shadow_samplers"));
}

bool ShadowPipeline::init(int sz){
    createResources(sz);
    // Identity light MVP
    memset(m_lightMVP,0,sizeof(m_lightMVP));
    m_lightMVP[0]=m_lightMVP[5]=m_lightMVP[10]=m_lightMVP[15]=1.0f;
    return m_fbo!=0;
}
void ShadowPipeline::resize(int sz){ if(sz!=m_size) createResources(sz); }
void ShadowPipeline::destroy(){
    if(m_fbo){ GLES.glDeleteFramebuffers(1,&m_fbo); m_fbo=0; }
    if(m_tex){ GLES.glDeleteTextures(1,&m_tex); m_tex=0; }
}
void ShadowPipeline::beginShadowPass(){
    if(!m_fbo) return;
    GLES.glGetIntegerv(GL_FRAMEBUFFER_BINDING,&m_savedFBO);
    GLES.glGetIntegerv(GL_VIEWPORT,m_savedVP);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,m_fbo);
    GLES.glViewport(0,0,m_size,m_size);
    GLES.glClear(GL_DEPTH_BUFFER_BIT);
    GLES.glColorMask(GL_FALSE,GL_FALSE,GL_FALSE,GL_FALSE);
    GLES.glEnable(GL_DEPTH_TEST);
    GLES.glDepthFunc(GL_LESS);
    GLES.glDepthMask(GL_TRUE);
}
void ShadowPipeline::endShadowPass(){
    if(!m_fbo) return;
    GLES.glColorMask(GL_TRUE,GL_TRUE,GL_TRUE,GL_TRUE);
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,(GLuint)m_savedFBO);
    GLES.glViewport(m_savedVP[0],m_savedVP[1],m_savedVP[2],m_savedVP[3]);
}
void ShadowPipeline::bindShadowMap(int unit){
    if(!m_tex) return;
    GLES.glActiveTexture(GL_TEXTURE0+unit);
    GLES.glBindTexture(GL_TEXTURE_2D,m_tex);
}

} // MG
#endif
