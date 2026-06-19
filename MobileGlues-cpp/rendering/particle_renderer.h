#pragma once
// MobileGlues - rendering/particle_renderer.h
// GPU Particle System - compute shader physics, zero CPU per particle
// Falls back to CPU-side billboards when compute unavailable
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
struct Particle {
    float x,y,z,w;     // position + age
    float vx,vy,vz,vw; // velocity + lifetime
    float r,g,b,a;      // color
    float size,pad[3];
};
class ParticleRenderer {
public:
    static constexpr int MAX_PARTICLES=4096;
    bool   init(int w,int h);
    void   destroy();
    void   emit(float x,float y,float z,float vx,float vy,float vz,
                float r,float g,float b,float life,float size);
    void   update(float dt);
    void   render(const float* viewProj);
    bool   isAvailable() const { return m_vbo!=0; }
private:
    GLuint m_vbo=0,m_prog=0,m_vao=0;
    int    m_count=0,m_w=0,m_h=0;
    Particle m_particles[MAX_PARTICLES];
};
ParticleRenderer& particles();
} // MG
#endif
