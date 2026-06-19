#pragma once
// MobileGlues - buffers/texture_atlas.h
// Texture Atlas - packs textures into single 4096x4096 ASTC/RGB8 atlas
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
#include <string>
#include <unordered_map>
namespace MG {
struct AtlasRegion { float u0,v0,u1,v1; };
class TextureAtlas {
public:
    static constexpr int ATLAS_SIZE=4096;
    bool   init();
    void   destroy();
    // Register a texture into the atlas (returns atlas UV coords)
    AtlasRegion allocate(const std::string& key,int w,int h,GLenum fmt,const void* data);
    bool         lookup(const std::string& key,AtlasRegion& out) const;
    void         bindAtlas(int unit);
    bool         isAvailable() const { return m_atlas!=0; }
    GLuint       atlasTexture() const { return m_atlas; }
    void         generateMipmaps();
private:
    GLuint m_atlas=0;
    int    m_curX=0,m_curY=0,m_rowH=0;
    std::unordered_map<std::string,AtlasRegion> m_regions;
};
TextureAtlas& textureAtlas();
} // MG
#endif
