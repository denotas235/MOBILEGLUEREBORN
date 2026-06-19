// MobileGlues - gl/persistent_buffers.cpp
// Persistent Mapped Buffers implementation
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "persistent_buffers.h"
#include "extension_scanner.h"
#include "../gles/loader.h"
#include "log.h"

namespace MG {

static const GLbitfield kMapWrite   = 0x0002u;
static const GLbitfield kPersistent = 0x0040u;
static const GLbitfield kCoherent   = 0x0080u;
static const GLbitfield kDynamic    = 0x0100u;

bool hasPersistentBufSupport(){ return has_extension("GL_EXT_buffer_storage"); }

bool PersistentBuffer::create(GLenum target, size_t bytes){
    destroy(); m_target=target; m_size=bytes;
    GLES.glGenBuffers(1,&m_buf);
    GLES.glBindBuffer(target,m_buf);
    if(hasPersistentBufSupport()){
        typedef void(*BStor)(GLenum,GLsizeiptr,const void*,GLbitfield);
        auto bstor=(BStor)eglGetProcAddress("glBufferStorageEXT");
        if(bstor){
            bstor(target,(GLsizeiptr)bytes,nullptr,kMapWrite|kPersistent|kCoherent|kDynamic);
            typedef void*(*MBR)(GLenum,GLintptr,GLsizeiptr,GLbitfield);
            auto mbr=(MBR)eglGetProcAddress("glMapBufferRange");
            if(mbr) m_mapped=mbr(target,0,(GLsizeiptr)bytes,kMapWrite|kPersistent|kCoherent);
            if(m_mapped){MG_LOG_I("PersistBuf %zu B @ %p",bytes,m_mapped);return true;}
            MG_LOG_W("PersistBuf: map failed, fallback");
        }
    }
    GLES.glBufferData(target,(GLsizeiptr)bytes,nullptr,GL_DYNAMIC_DRAW);
    m_mapped=nullptr; return m_buf!=0;
}

void PersistentBuffer::destroy(){
    if(m_buf){
        if(m_mapped){
            typedef GLboolean(*UB)(GLenum);
            auto ub=(UB)eglGetProcAddress("glUnmapBuffer");
            if(ub)ub(m_target); m_mapped=nullptr;
        }
        GLES.glDeleteBuffers(1,&m_buf); m_buf=0;
    }
}

void PersistentBuffer::bind() const { if(m_buf)GLES.glBindBuffer(m_target,m_buf); }

void PersistentBuffer::upload(size_t off,const void* data,size_t len){
    if(!m_buf||!data)return;
    GLES.glBindBuffer(m_target,m_buf);
    GLES.glBufferSubData(m_target,(GLintptr)off,(GLsizeiptr)len,data);
}

} // MG
#endif
