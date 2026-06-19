#pragma once
// MobileGlues - gpu_driven/async_compute.h
// Async Compute: fence-based overlap of compute + render
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <functional>

namespace MG {

class AsyncCompute {
public:
    bool  init();
    void  destroy();
    void  dispatch(int gx,int gy,int gz);
    void  submitFence();
    bool  isFenceDone();
    void  waitFence(GLuint64 nsTimeout=1000000000ULL);
    bool  isAvailable() const { return m_supported; }

private:
    bool   m_supported = false;
    GLsync m_fence     = nullptr;
    using FenceCreate  = GLsync(*)(GLenum,GLbitfield);
    using FenceWait    = GLenum(*)(GLsync,GLbitfield,GLuint64);
    using FenceDelete  = void(*)(GLsync);
    using FenceStatus  = void(*)(GLsync,GLenum,GLsizei,GLsizei*,GLint*);
    FenceCreate  m_fnCreate = nullptr;
    FenceWait    m_fnWait   = nullptr;
    FenceDelete  m_fnDelete = nullptr;
    FenceStatus  m_fnStatus = nullptr;
};

AsyncCompute& asyncCompute();

} // MG
#endif
