// MobileGlues - pipeline/geometry_pass.cpp
// Geometry pass - deferred to phase2_lighting and rendering_pipeline
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "geometry_pass.h"
#include "log.h"
#include "../gles/loader.h"
static GLuint s_outTex=0;
namespace MG {
namespace GeometryPass {
    bool init(int w,int h){ (void)w;(void)h; MG_LOG_I("GeometryPass: init"); return true; }
    void resize(int w,int h){ (void)w;(void)h; }
    void destroy(){ if(s_outTex){ GLES.glDeleteTextures(1,&s_outTex); s_outTex=0; } }
    bool run(GLuint input,GLuint depth){ (void)input;(void)depth; return true; }
    GLuint outputTex(){ return s_outTex; }
}
} // MG
#endif
