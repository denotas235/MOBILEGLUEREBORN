#pragma once
// MobileGlues - rendering/weather_renderer.h
// Rain/Snow instanced rendering - single draw call for thousands of drops
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include <GLES3/gl3.h>
namespace MG {
enum class WeatherType { NONE, RAIN, SNOW };
class WeatherRenderer {
public:
    static constexpr int MAX_DROPS=2048;
    bool   init(int w,int h);
    void   destroy();
    void   setWeather(WeatherType type,float intensity);
    void   render(float dt,const float* viewPos,const float* viewProj);
    bool   isAvailable() const { return m_prog!=0; }
private:
    GLuint m_prog=0,m_vbo=0,m_vao=0;
    WeatherType m_type=WeatherType::NONE;
    float  m_intensity=0.0f,m_time=0.0f;
    int    m_w=0,m_h=0;
};
WeatherRenderer& weather();
} // MG
#endif
