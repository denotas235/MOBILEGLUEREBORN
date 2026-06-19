#pragma once
// MobileGlues - pipeline/geometry_pass.h
// Geometry pass interface stub (logic is in phase2_lighting + rendering_pipeline)
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
namespace GeometryPass {
    bool init(int w,int h);
    void resize(int w,int h);
    void destroy();
    bool run(GLuint input,GLuint depth);
    GLuint outputTex();
}
} // MG
#endif
