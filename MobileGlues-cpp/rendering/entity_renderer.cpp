// MobileGlues - rendering/entity_renderer.cpp
// Batches entities by type, renders with glDrawArraysInstanced
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "entity_renderer.h"
#include "log.h"
#include "../gles/loader.h"
#include <algorithm>
#include <cstring>

namespace MG {
EntityRenderer& entityRenderer(){ static EntityRenderer s; return s; }

bool EntityRenderer::init(){
    GLES.glGenBuffers(1,&m_instanceBuf);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_instanceBuf);
    GLES.glBufferData(GL_ARRAY_BUFFER,MAX_INSTANCES*(GLsizeiptr)sizeof(EntityInstance),nullptr,GL_DYNAMIC_DRAW);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,0);
    m_batches.reserve(32);
    MG_LOG_I("EntityRenderer: max=%d instances",MAX_INSTANCES);
    return m_instanceBuf!=0;
}

void EntityRenderer::destroy(){
    if(m_instanceBuf){ GLES.glDeleteBuffers(1,&m_instanceBuf); m_instanceBuf=0; }
    m_batches.clear();
}

void EntityRenderer::beginFrame(){
    m_batches.clear();
}

void EntityRenderer::addInstance(int typeId,float x,float y,float z,float ry,float scale){
    // Find or create batch for this type
    for(auto& b : m_batches){
        if(b.typeId==typeId && (int)b.instances.size()<MAX_INSTANCES){
            EntityInstance inst{x,y,z,0.0f,ry,0.0f,scale,0.0f,0.0f};
            b.instances.push_back(inst);
            return;
        }
    }
    Batch nb; nb.typeId=typeId;
    EntityInstance inst{x,y,z,0.0f,ry,0.0f,scale,0.0f,0.0f};
    nb.instances.push_back(inst);
    m_batches.push_back(std::move(nb));
}

void EntityRenderer::flush(GLuint /*shader*/,GLuint /*atlas*/){
    if(!m_instanceBuf) return;
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_instanceBuf);
    for(auto& batch : m_batches){
        if(batch.instances.empty()) continue;
        int n=std::min((int)batch.instances.size(),MAX_INSTANCES);
        GLES.glBufferSubData(GL_ARRAY_BUFFER,0,
                             n*(GLsizeiptr)sizeof(EntityInstance),
                             batch.instances.data());
        // Instanced draw: caller must have geometry VAO bound
        GLES.glDrawArraysInstanced(GL_TRIANGLES,0,24,n); // 24 verts = cube entity approximation
    }
    GLES.glBindBuffer(GL_ARRAY_BUFFER,0);
}
} // MG
#endif
