#pragma once
// MobileGlues - gl/gpu_driven.h
// GPU-Driven Indirect Rendering via glMultiDrawArraysIndirect
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <vector>
#include <cstddef>

namespace MG {

struct DrawIndirectCmd { GLuint count,instanceCount,first,baseInstance; };
struct GPUChunkData   { float wx,wy,wz,pad; GLuint vOffset,vCount,lod,visible; };

class GPUDrivenRenderer {
public:
    static constexpr size_t MEGA_VBO = 64u<<20; // 64 MB
    static constexpr int    MAX_CHUNKS = 1024;

    bool init();
    void destroy();
    GLuint uploadVertices(const void* data, GLsizei bytes);
    void   updateChunks(const std::vector<GPUChunkData>& chunks);
    void   renderAll(int n);
    bool   isAvailable() const { return m_ok; }

private:
    GLuint m_vbo=0, m_indirect=0, m_ssbo=0;
    size_t m_vboOff=0;
    bool   m_ok=false, m_multiDraw=false, m_bufStorage=false;
};

GPUDrivenRenderer& gpuRenderer();
} // MG
#endif
