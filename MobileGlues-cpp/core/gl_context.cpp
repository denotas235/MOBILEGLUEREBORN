// MobileGlues - core/gl_context.cpp
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "gl_context.h"
#include "../gles/loader.h"
#include <cstring>

namespace MG {

GLStateCache& glCache(){ static GLStateCache s; return s; }

void GLStateCache::setBlend(bool en){
    if(blendEnabled==en) return;
    blendEnabled=en;
    if(en) GLES.glEnable(GL_BLEND); else GLES.glDisable(GL_BLEND);
}
void GLStateCache::setBlendFunc(GLenum sRGB,GLenum dRGB,GLenum sA,GLenum dA){
    if(blendSrcRGB==sRGB&&blendDstRGB==dRGB&&blendSrcAlpha==sA&&blendDstAlpha==dA) return;
    blendSrcRGB=sRGB;blendDstRGB=dRGB;blendSrcAlpha=sA;blendDstAlpha=dA;
    GLES.glBlendFuncSeparate(sRGB,dRGB,sA,dA);
}
void GLStateCache::setDepthTest(bool en){
    if(depthTest==en) return;
    depthTest=en;
    if(en) GLES.glEnable(GL_DEPTH_TEST); else GLES.glDisable(GL_DEPTH_TEST);
}
void GLStateCache::setDepthMask(GLboolean m){
    if(depthMask==m) return;
    depthMask=m;
    GLES.glDepthMask(m);
}
void GLStateCache::setDepthFunc(GLenum f){
    if(depthFunc==f) return;
    depthFunc=f;
    GLES.glDepthFunc(f);
}
void GLStateCache::setCull(bool en,GLenum face){
    if(cullEnabled!=en){ cullEnabled=en; if(en)GLES.glEnable(GL_CULL_FACE); else GLES.glDisable(GL_CULL_FACE); }
    if(cullEnabled&&cullFace!=face){ cullFace=face; GLES.glCullFace(face); }
}
void GLStateCache::setScissor(bool en){
    if(scissorEnabled==en) return;
    scissorEnabled=en;
    if(en) GLES.glEnable(GL_SCISSOR_TEST); else GLES.glDisable(GL_SCISSOR_TEST);
}
void GLStateCache::useProgram(GLuint p){
    if(prog==p) return;
    prog=p;
    GLES.glUseProgram(p);
}
void GLStateCache::bindFBO(GLuint f){
    if(fbo==f) return;
    fbo=f;
    GLES.glBindFramebuffer(GL_FRAMEBUFFER,f);
}
void GLStateCache::bindVAO(GLuint v){
    if(vao==v) return;
    vao=v;
    GLES.glBindVertexArray(v);
}
void GLStateCache::bindTex2D(int unit,GLuint t){
    if(unit<0||unit>=16) return;
    if(tex2d[unit]==t) return;
    tex2d[unit]=t;
    GLES.glActiveTexture(GL_TEXTURE0+unit);
    GLES.glBindTexture(GL_TEXTURE_2D,t);
}
void GLStateCache::invalidate(){
    blendEnabled=false; depthTest=false; cullEnabled=false; scissorEnabled=false;
    blendSrcRGB=GL_ONE; blendDstRGB=GL_ZERO; blendSrcAlpha=GL_ONE; blendDstAlpha=GL_ZERO;
    depthFunc=GL_LESS; depthMask=GL_TRUE; prog=0; fbo=0; vao=0;
    for(int i=0;i<16;i++) tex2d[i]=0;
    activeUnit=0;
}

} // namespace MG
#endif
