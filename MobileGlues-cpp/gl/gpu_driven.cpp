// MobileGlues - gl/gpu_driven.cpp
// GPU-Driven Indirect Rendering implementation
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "gpu_driven.h"
#include "extension_scanner.h"
#include "../gles/loader.h"
#include "log.h"
#include <algorithm>

// GL enum constants not in all gl3.h versions
#define MG_DRAW_INDIRECT_BUFFER    0x8F3Fu
#define MG_SHADER_STORAGE_BUFFER   0x90D2u
#define MG_DYNAMIC_STORAGE_BIT     0x0100u
#define MG_MAP_WRITE_BIT           0x0002u

namespace MG {

GPUDrivenRenderer& gpuRenderer(){ static GPUDrivenRenderer s; return s; }

bool GPUDrivenRenderer::init(){
    if(m_ok)return true;
    m_multiDraw  = has_extension("GL_EXT_multi_draw_indirect");
    m_bufStorage = has_extension("GL_EXT_buffer_storage");

    GLES.glGenBuffers(1,&m_vbo);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferData(GL_ARRAY_BUFFER,(GLsizeiptr)MEGA_VBO,nullptr,GL_DYNAMIC_DRAW);

    GLES.glGenBuffers(1,&m_indirect);
    GLES.glBindBuffer((GLenum)MG_DRAW_INDIRECT_BUFFER,m_indirect);
    GLES.glBufferData((GLenum)MG_DRAW_INDIRECT_BUFFER,MAX_CHUNKS*(GLsizeiptr)sizeof(DrawIndirectCmd),nullptr,GL_DYNAMIC_DRAW);

    GLES.glGenBuffers(1,&m_ssbo);
    GLES.glBindBuffer((GLenum)MG_SHADER_STORAGE_BUFFER,m_ssbo);
    GLES.glBufferData((GLenum)MG_SHADER_STORAGE_BUFFER,MAX_CHUNKS*(GLsizeiptr)sizeof(GPUChunkData),nullptr,GL_DYNAMIC_DRAW);

    GLES.glBindBuffer(GL_ARRAY_BUFFER,0);
    m_ok=true;
    MG_LOG_I("GPUDriven: OK multiDraw=%d bufStorage=%d",(int)m_multiDraw,(int)m_bufStorage);
    return true;
}

void GPUDrivenRenderer::destroy(){
    if(m_vbo)     {GLES.glDeleteBuffers(1,&m_vbo);     m_vbo=0;}
    if(m_indirect){GLES.glDeleteBuffers(1,&m_indirect);m_indirect=0;}
    if(m_ssbo)    {GLES.glDeleteBuffers(1,&m_ssbo);    m_ssbo=0;}
    m_ok=false; m_vboOff=0;
}

GLuint GPUDrivenRenderer::uploadVertices(const void* data,GLsizei bytes){
    if(!m_ok||!data||bytes<=0)return (GLuint)-1;
    if(m_vboOff+bytes>MEGA_VBO){MG_LOG_W("GPUDriven VBO full, reset");m_vboOff=0;}
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferSubData(GL_ARRAY_BUFFER,(GLintptr)m_vboOff,bytes,data);
    GLuint off=(GLuint)m_vboOff; m_vboOff+=bytes; return off;
}

void GPUDrivenRenderer::updateChunks(const std::vector<GPUChunkData>& chunks){
    if(!m_ok||chunks.empty())return;
    int n=(int)std::min((size_t)MAX_CHUNKS,chunks.size());
    GLES.glBindBuffer((GLenum)MG_SHADER_STORAGE_BUFFER,m_ssbo);
    GLES.glBufferSubData((GLenum)MG_SHADER_STORAGE_BUFFER,0,n*(GLsizeiptr)sizeof(GPUChunkData),chunks.data());

    std::vector<DrawIndirectCmd> cmds(n);
    for(int i=0;i<n;i++){
        cmds[i]={chunks[i].vCount, chunks[i].visible?1u:0u, chunks[i].vOffset/20u, (GLuint)i};
    }
    GLES.glBindBuffer((GLenum)MG_DRAW_INDIRECT_BUFFER,m_indirect);
    GLES.glBufferSubData((GLenum)MG_DRAW_INDIRECT_BUFFER,0,n*(GLsizeiptr)sizeof(DrawIndirectCmd),cmds.data());
}

void GPUDrivenRenderer::renderAll(int n){
    if(!m_ok||n<=0)return;
    n=std::min(n,MAX_CHUNKS);
    GLES.glBindBuffer((GLenum)MG_DRAW_INDIRECT_BUFFER,m_indirect);
    if(m_multiDraw){
        typedef void(*PFNMDI)(GLenum,const void*,GLsizei,GLsizei);
        auto fn=(PFNMDI)eglGetProcAddress("glMultiDrawArraysIndirectEXT");
        if(fn){fn(GL_TRIANGLES,nullptr,n,0);return;}
    }
    for(int i=0;i<n;i++)
        GLES.glDrawArraysIndirect(GL_TRIANGLES,(const void*)(intptr_t)(i*sizeof(DrawIndirectCmd)));
}

} // MG
#endif
