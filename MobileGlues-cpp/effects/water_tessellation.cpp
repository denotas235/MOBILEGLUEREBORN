// MobileGlues - effects/water_tessellation.cpp
// Injects Gerstner wave displacement into water vertex shaders
// No tessellation HW required - uses sine wave vertex displacement
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "water_tessellation.h"
#include "extension_scanner.h"
#include "log.h"
#include <string>

namespace MG {
WaterTessellation& waterTess(){ static WaterTessellation s; return s; }

// Gerstner wave displacement code injected into vertex shader
static const char* WAVE_CODE = R"glsl(
// MobileGlues wave displacement
uniform float mg_waterTime;
vec3 mg_gerstnerWave(vec3 pos){
    float wave1=sin(pos.x*0.5+mg_waterTime*1.2)*0.12;
    float wave2=sin(pos.z*0.7+mg_waterTime*0.9+1.0)*0.08;
    float wave3=cos((pos.x+pos.z)*0.3+mg_waterTime*1.5)*0.05;
    return vec3(0.0,wave1+wave2+wave3,0.0);
}
)glsl";

// Check if a shader is a water shader (Minecraft uses specific uniforms)
static bool isWater(const std::string& src){
    return src.find("water")!=std::string::npos ||
           src.find("Water")!=std::string::npos ||
           src.find("WATER")!=std::string::npos ||
           (src.find("gl_Position")!=std::string::npos &&
            src.find("FogStart")!=std::string::npos &&
            src.find("fogStart")!=std::string::npos);
}

bool WaterTessellation::init(){
    m_hasTess=has_extension("GL_EXT_tessellation_shader");
    m_ready=true;
    MG_LOG_I("WaterTess: hasTessHW=%d (using vertex displacement)",(int)m_hasTess);
    return true;
}
void WaterTessellation::destroy(){ m_ready=false; }

std::string WaterTessellation::injectWaveDisplacement(const std::string& vertSrc,bool isWaterShader){
    if(!m_ready) return vertSrc;
    if(!isWaterShader && !isWater(vertSrc)) return vertSrc;
    // Already patched?
    if(vertSrc.find("mg_gerstnerWave")!=std::string::npos) return vertSrc;
    // Find #version line end
    size_t nl=vertSrc.find('\n');
    if(nl==std::string::npos) return vertSrc;
    // Inject wave code after #version, before main
    std::string patched=vertSrc.substr(0,nl+1)+WAVE_CODE+vertSrc.substr(nl+1);
    // Inject displacement call just before gl_Position assignment
    size_t glpos=patched.find("gl_Position");
    if(glpos!=std::string::npos){
        size_t lineStart=patched.rfind('\n',glpos)+1;
        std::string indent="    ";
        std::string inject=indent+"vec3 mg_waveOff=mg_gerstnerWave(vec3(gl_Position.x,0.0,gl_Position.z));\n";
        patched.insert(lineStart,inject);
        // After gl_Position assignment, apply offset
        size_t semi=patched.find(';',patched.find("gl_Position"));
        if(semi!=std::string::npos){
            patched.insert(semi+1,"\n    gl_Position.y+=mg_waveOff.y;");
        }
    }
    MG_LOG_I("WaterTess: wave displacement injected");
    return patched;
}
} // MG
#endif
