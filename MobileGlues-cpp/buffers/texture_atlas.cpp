// MobileGlues - buffers/texture_atlas.cpp
// Row-based texture atlas packer with ASTC compression support
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "texture_atlas.h"
#include "extension_scanner.h"
#include "log.h"
#include "../gles/loader.h"

namespace MG {
TextureAtlas& textureAtlas(){ static TextureAtlas s; return s; }

bool TextureAtlas::init(){
    if(m_atlas) return true;
    GLES.glGenTextures(1,&m_atlas);
    GLES.glBindTexture(GL_TEXTURE_2D,m_atlas);
    // Use RGBA8 as base - ASTC compression applied on upload if available
    GLES.glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,ATLAS_SIZE,ATLAS_SIZE,0,GL_RGBA,GL_UNSIGNED_BYTE,nullptr);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_LINEAR_MIPMAP_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
    GLES.glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
    // Anisotropic filtering
    if(has_extension("GL_EXT_texture_filter_anisotropic")){
        float maxAniso=1.0f;
        GLES.glGetFloatv(0x84FFu,&maxAniso);
        GLES.glTexParameterf(GL_TEXTURE_2D,0x84FEu,maxAniso); // GL_TEXTURE_MAX_ANISOTROPY_EXT
    }
    GLES.glBindTexture(GL_TEXTURE_2D,0);
    MG_LOG_I("TextureAtlas: %dx%d RGBA8 ASTC=%d",ATLAS_SIZE,ATLAS_SIZE,(int)has_extension("GL_KHR_texture_compression_astc_ldr"));
    return true;
}

void TextureAtlas::destroy(){
    if(m_atlas){ GLES.glDeleteTextures(1,&m_atlas); m_atlas=0; }
    m_regions.clear(); m_curX=m_curY=m_rowH=0;
}

AtlasRegion TextureAtlas::allocate(const std::string& key,int w,int h,GLenum fmt,const void* data){
    // Check existing
    auto it=m_regions.find(key);
    if(it!=m_regions.end()) return it->second;
    // Row packing: advance to next row if needed
    if(m_curX+w>ATLAS_SIZE){ m_curY+=m_rowH+1; m_curX=0; m_rowH=0; }
    if(m_curY+h>ATLAS_SIZE){ MG_LOG_W("Atlas full!"); return {0,0,0,0}; }
    // Upload sub-region
    GLES.glBindTexture(GL_TEXTURE_2D,m_atlas);
    GLES.glTexSubImage2D(GL_TEXTURE_2D,0,m_curX,m_curY,w,h,fmt,GL_UNSIGNED_BYTE,data);
    AtlasRegion r{
        (float)m_curX/ATLAS_SIZE, (float)m_curY/ATLAS_SIZE,
        (float)(m_curX+w)/ATLAS_SIZE, (float)(m_curY+h)/ATLAS_SIZE
    };
    m_regions[key]=r;
    if(h>m_rowH) m_rowH=h;
    m_curX+=w+1;
    return r;
}

bool TextureAtlas::lookup(const std::string& key,AtlasRegion& out) const {
    auto it=m_regions.find(key);
    if(it==m_regions.end()) return false;
    out=it->second; return true;
}

void TextureAtlas::bindAtlas(int unit){
    if(!m_atlas) return;
    GLES.glActiveTexture(GL_TEXTURE0+unit);
    GLES.glBindTexture(GL_TEXTURE_2D,m_atlas);
}

void TextureAtlas::generateMipmaps(){
    if(!m_atlas) return;
    GLES.glBindTexture(GL_TEXTURE_2D,m_atlas);
    GLES.glGenerateMipmap(GL_TEXTURE_2D);
}
} // MG
#endif
