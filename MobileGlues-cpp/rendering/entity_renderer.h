#pragma once
// MobileGlues - rendering/entity_renderer.h
// Instanced Entity Rendering - 1 draw call per entity type
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <vector>
namespace MG {
struct EntityInstance {
    float x,y,z;
    float rx,ry,rz;  // rotation
    float scale;
    float texU,texV; // atlas offset
};
class EntityRenderer {
public:
    static constexpr int MAX_INSTANCES=512;
    bool   init();
    void   destroy();
    void   beginFrame();
    void   addInstance(int typeId,float x,float y,float z,float ry,float scale);
    void   flush(GLuint shader,GLuint atlas);
    bool   isAvailable() const { return m_instanceBuf!=0; }
private:
    GLuint m_instanceBuf=0;
    struct Batch { int typeId; std::vector<EntityInstance> instances; };
    std::vector<Batch> m_batches;
};
EntityRenderer& entityRenderer();
} // MG
#endif
