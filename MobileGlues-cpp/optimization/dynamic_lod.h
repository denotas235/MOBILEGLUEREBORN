#pragma once
// MobileGlues - optimization/dynamic_lod.h
// Dynamic LOD: adjusts chunk detail by distance, speed, altitude
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
enum class LODLevel { LOD0=0, LOD1=1, LOD2=2, LOD3=3 };
struct LODParams {
    float distanceSq;
    LODLevel level;
    float vertexRatio; // 1.0=full, 0.5=half, 0.25=quarter, 0.0=silhouette
};
class DynamicLOD {
public:
    bool   init();
    void   update(float px,float py,float pz,
                  float speed,float altitude,float lookDirY);
    LODParams evalChunk(float cx,float cy,float cz) const;
    GLsizei   applyLOD(GLsizei indexCount,LODLevel lod) const;
    bool   isAvailable() const { return m_ready; }
private:
    bool  m_ready=false;
    float m_px=0,m_py=0,m_pz=0;
    float m_speed=0,m_altitude=0,m_lookDirY=0;
};
DynamicLOD& dynLOD();
} // MG
#endif
