// MobileGlues - lighting/shadow_pipeline.cpp
// PCF Shadow Map - Depth-only FBO, sampled with GL_EXT_shadow_samplers
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "shadow_pipeline.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>
#include <cmath>
#include <ankerl/unordered_dense.h>

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

// ── updateLightDirection ──────────────────────────────────────────────────
// Builds a column-major orthographic light MVP from the given direction.
// Coverage: 256×256 blocks wide, 512 blocks deep — suitable for any render
// distance Minecraft Bedrock uses on mobile.
//
// Matrix layout expected by GLES: column-major (same as glm::mat4).
//
void ShadowPipeline::updateLightDirection(float lx, float ly, float lz) {
    // Normalise
    float len = sqrtf(lx*lx + ly*ly + lz*lz);
    if (len < 1e-5f) return;
    lx /= len; ly /= len; lz /= len;

    // Eye: far away in the light direction (sun/moon is treated as directional)
    float dist = 300.0f;
    float ex = lx * dist, ey = ly * dist, ez = lz * dist;

    // Build view matrix: lookAt(eye, origin, up)
    // Forward direction (towards scene)
    float fx = -lx, fy = -ly, fz = -lz;

    // Up vector: world Y, unless light is nearly vertical
    float ux = 0, uy = 1, uz = 0;
    if (fabsf(ly) > 0.98f) { ux = 1; uy = 0; uz = 0; }

    // Right = forward × up
    float rx = fy*uz - fz*uy, ry = fz*ux - fx*uz, rz = fx*uy - fy*ux;
    float rl = sqrtf(rx*rx+ry*ry+rz*rz);
    if (rl < 1e-5f) return;
    rx/=rl; ry/=rl; rz/=rl;

    // Recompute up = right × forward
    ux = ry*fz - rz*fy; uy = rz*fx - rx*fz; uz = rx*fy - ry*fx;

    // View matrix (column-major):
    float view[16] = {
         rx,             ux,             -fx,            0.0f,
         ry,             uy,             -fy,            0.0f,
         rz,             uz,             -fz,            0.0f,
        -(rx*ex+ry*ey+rz*ez), -(ux*ex+uy*ey+uz*ez), fx*ex+fy*ey+fz*ez, 1.0f
    };

    // Orthographic projection (column-major, clip [-1,1]):
    //   left=-128, right=128, bottom=-128, top=128, near=-256, far=256
    float L=-128.f,R=128.f,B=-128.f,T=128.f,N=-256.f,F=256.f;
    float proj[16] = {
        2.0f/(R-L),      0,               0,               0,
        0,               2.0f/(T-B),      0,               0,
        0,               0,              -2.0f/(F-N),       0,
        -(R+L)/(R-L),   -(T+B)/(T-B),   -(F+N)/(F-N),     1.0f
    };

    // lightMVP = proj × view  (column-major multiply: C[col][row])
    for (int col = 0; col < 4; col++) {
        for (int row = 0; row < 4; row++) {
            float sum = 0;
            for (int k = 0; k < 4; k++)
                sum += proj[k*4+row] * view[col*4+k];
            m_lightMVP[col*4+row] = sum;
        }
    }
    MG_LOG_I("ShadowPipeline: lightMVP updated dir=(%.2f,%.2f,%.2f)",lx,ly,lz);
}

// ── uploadShadowUniforms ──────────────────────────────────────────────────
// Per-program uniform location cache: avoids glGetUniformLocation every draw.
// Returns true if the program has MG shadow uniforms.
//
static ankerl::unordered_dense::map<GLuint, GLint> g_locLightMVP;
static ankerl::unordered_dense::map<GLuint, GLint> g_locShadowMap;
static constexpr int MG_SHADOW_TEX_UNIT = 7; // reserved unit for shadow map

bool ShadowPipeline::uploadShadowUniforms(GLuint program) {
    if (!m_fbo || !program) return false;

    // Cache miss — query locations once per program
    if (g_locLightMVP.find(program) == g_locLightMVP.end()) {
        g_locLightMVP[program] = GLES.glGetUniformLocation(program, "u_mg_lightMVP");
        g_locShadowMap[program] = GLES.glGetUniformLocation(program, "u_mg_shadowMap");
    }

    GLint locMVP = g_locLightMVP[program];
    GLint locMap = g_locShadowMap[program];

    bool uploaded = false;
    if (locMVP >= 0) {
        GLES.glUniformMatrix4fv(locMVP, 1, GL_FALSE, m_lightMVP);
        uploaded = true;
    }
    if (locMap >= 0 && m_tex) {
        // Bind shadow depth texture to the reserved unit
        GLES.glActiveTexture(GL_TEXTURE0 + MG_SHADOW_TEX_UNIT);
        GLES.glBindTexture(GL_TEXTURE_2D, m_tex);
        GLES.glUniform1i(locMap, MG_SHADOW_TEX_UNIT);
        // Note: caller must restore active texture unit
        uploaded = true;
    }
    return uploaded;
}

} // MG
#endif
