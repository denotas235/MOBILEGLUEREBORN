// MobileGlues - optimization/dynamic_lod.cpp
// LOD0: full, LOD1: 50%, LOD2: 25%, LOD3: silhouette
// Adjusts for elytra speed, altitude, and camera direction
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "dynamic_lod.h"
#include "log.h"
#include <cmath>
#include <algorithm>

namespace MG {
DynamicLOD& dynLOD(){ static DynamicLOD s; return s; }

bool DynamicLOD::init(){
    m_ready=true;
    MG_LOG_I("DynamicLOD: ready LOD0=0-32 LOD1=32-64 LOD2=64-128 LOD3=128+");
    return true;
}

void DynamicLOD::update(float px,float py,float pz,float speed,float altitude,float lookDirY){
    m_px=px; m_py=py; m_pz=pz;
    m_speed=speed; m_altitude=altitude; m_lookDirY=lookDirY;
}

LODParams DynamicLOD::evalChunk(float cx,float cy,float cz) const {
    float dx=cx-m_px, dy=cy-m_py, dz=cz-m_pz;
    float distSq=dx*dx+dy*dy+dz*dz;
    LODLevel lod=LODLevel::LOD0;
    if     (distSq> 128.0f*128.0f) lod=LODLevel::LOD3;
    else if(distSq>  64.0f* 64.0f) lod=LODLevel::LOD2;
    else if(distSq>  32.0f* 32.0f) lod=LODLevel::LOD1;

    // Elytra speed boost
    if(m_speed>20.0f){
        int bump=1+std::min(2,(int)((m_speed-20.0f)/20.0f));
        lod=(LODLevel)std::min((int)LODLevel::LOD3,(int)lod+bump);
    }
    // High altitude: chunks below get demoted
    if(m_altitude>150.0f && dy<-32.0f){
        lod=(LODLevel)std::min((int)LODLevel::LOD3,(int)lod+1);
    }
    // Looking up: floor chunks get lower priority
    if(m_lookDirY>0.6f && dy<0.0f){
        lod=(LODLevel)std::min((int)LODLevel::LOD3,(int)lod+1);
    }

    static const float ratios[]={1.0f,0.5f,0.25f,0.0f};
    return {distSq,lod,ratios[(int)lod]};
}

GLsizei DynamicLOD::applyLOD(GLsizei count,LODLevel lod) const {
    switch(lod){
        case LODLevel::LOD3: return std::max((GLsizei)36,count/8);  // silhouette
        case LODLevel::LOD2: return count/4;
        case LODLevel::LOD1: return count/2;
        default:             return count;
    }
}
} // MG
#endif
