#pragma once
// MobileGlues - effects/water_tessellation.h
// Water surface animation via vertex displacement (no tessellation HW required)
// Falls back to geometry shader if GL_EXT_tessellation_shader unavailable
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <string>
#include <GLES3/gl3.h>
namespace MG {
class WaterTessellation {
public:
    bool   init();
    void   destroy();
    // Inject wave displacement into a water vertex shader
    std::string injectWaveDisplacement(const std::string& vertSrc,bool isWaterShader);
    bool   isAvailable() const { return m_ready; }
    void   setTime(float t)   { m_time=t; }
private:
    bool   m_ready=false;
    bool   m_hasTess=false;
    float  m_time=0.0f;
};
WaterTessellation& waterTess();
} // MG
#endif
