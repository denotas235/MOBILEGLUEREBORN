#pragma once
// MobileGlues - gl/rendering_pipeline.h
// PLS + Godrays + Rayleigh Sky + ACES Composite
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>

namespace MG {
namespace RenderPipeline {

void init();
void updateSkyState(float timeOfDay);
bool runGodrayPass(GLuint sceneTex, float sunSX, float sunSY, float scrW, float scrH);
bool runSkyPass(float scrW, float scrH);
bool runCompositePass(GLuint godrayTex, GLuint skyTex, float scrW, float scrH);
bool hasPLS();
bool hasGodraySupport();

} // RenderPipeline
} // MG
#endif
