// MobileGlues - gpu_driven/async_compute.cpp
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "async_compute.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"

namespace MG {

AsyncCompute& asyncCompute(){ static AsyncCompute s; return s; }

bool AsyncCompute::init(){
    m_supported = (has_extension("GL_ANDROID_extension_pack_es31a") ||
                   has_extension("GL_OES_gpu_shader5"));
    // Load fence functions via EGL
    m_fnCreate=(FenceCreate)eglGetProcAddress("glFenceSync");
    m_fnWait  =(FenceWait)  eglGetProcAddress("glClientWaitSync");
    m_fnDelete=(FenceDelete) eglGetProcAddress("glDeleteSync");
    m_fnStatus=(FenceStatus) eglGetProcAddress("glGetSynciv");
    if(!m_fnCreate||!m_fnWait||!m_fnDelete){ m_supported=false; }
    MG_LOG_I("AsyncCompute: supported=%d",(int)m_supported);
    return m_supported;
}

void AsyncCompute::destroy(){
    if(m_fence&&m_fnDelete){ m_fnDelete(m_fence); m_fence=nullptr; }
}

void AsyncCompute::dispatch(int gx,int gy,int gz){
    if(!m_supported) return;
    typedef void(*DispatchFn)(GLuint,GLuint,GLuint);
    auto fn=(DispatchFn)eglGetProcAddress("glDispatchCompute");
    if(fn) fn((GLuint)gx,(GLuint)gy,(GLuint)gz);
    // Memory barrier for SSBO/indirect buffer visibility
    typedef void(*BarrierFn)(GLbitfield);
    auto bar=(BarrierFn)eglGetProcAddress("glMemoryBarrier");
    if(bar) bar(0x00000200u|0x00000100u); // SHADER_STORAGE|COMMAND_BARRIER
}

void AsyncCompute::submitFence(){
    if(!m_fnCreate) return;
    if(m_fence&&m_fnDelete){ m_fnDelete(m_fence); m_fence=nullptr; }
    m_fence=m_fnCreate(0x9117u,0); // GL_SYNC_GPU_COMMANDS_COMPLETE
}

bool AsyncCompute::isFenceDone(){
    if(!m_fence||!m_fnStatus) return true;
    GLint val=0; GLsizei len=0;
    m_fnStatus(m_fence,0x9114u,1,&len,&val); // GL_SYNC_STATUS
    return val==0x9119; // GL_SIGNALED
}

void AsyncCompute::waitFence(GLuint64 ns){
    if(!m_fence||!m_fnWait) return;
    m_fnWait(m_fence,0,ns);
}

} // MG
#endif
