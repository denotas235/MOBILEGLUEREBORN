// MobileGlues - rendering/weather_renderer.cpp
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "weather_renderer.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>
#include <cmath>

namespace MG {
WeatherRenderer& weather(){ static WeatherRenderer s; return s; }

static const char* VERT_RAIN = R"(
#version 310 es
precision highp float;
layout(location=0) in float a_id;
uniform mat4  u_vp;
uniform vec3  u_viewPos;
uniform float u_time;
uniform float u_intensity;
out float v_alpha;
// Simple LCG hash for per-drop randomness
float hash(float n){ return fract(sin(n)*43758.5453); }
void main(){
    float id=a_id;
    float ox=hash(id)*64.0-32.0;
    float oz=hash(id+1.1)*64.0-32.0;
    float oy=mod(hash(id+2.2)*20.0+u_time*8.0,20.0);
    // Wind displacement
    float windX=sin(u_time*0.3)*0.5;
    vec3 pos=u_viewPos+vec3(ox+windX,10.0-oy,oz);
    v_alpha=u_intensity*0.6;
    gl_PointSize=2.0;
    gl_Position=u_vp*vec4(pos,1.0);
}
)";
static const char* FRAG_RAIN = R"(
#version 310 es
precision mediump float;
in float v_alpha;
out vec4 fragColor;
void main(){
    fragColor=vec4(0.7,0.8,1.0,v_alpha);
}
)";

static GLuint mkS(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("Weather: %s",l);GLES.glDeleteShader(sh);return 0;}
    return sh;
}

bool WeatherRenderer::init(int w,int h){
    m_w=w;m_h=h;
    GLuint vs=mkS(GL_VERTEX_SHADER,VERT_RAIN);
    GLuint fs=mkS(GL_FRAGMENT_SHADER,FRAG_RAIN);
    if(!vs||!fs){ if(vs)GLES.glDeleteShader(vs); if(fs)GLES.glDeleteShader(fs); return false; }
    m_prog=GLES.glCreateProgram();
    GLES.glAttachShader(m_prog,vs); GLES.glAttachShader(m_prog,fs); GLES.glLinkProgram(m_prog);
    GLES.glDeleteShader(vs); GLES.glDeleteShader(fs);
    // VBO: just indices [0..MAX_DROPS)
    float ids[MAX_DROPS];
    for(int i=0;i<MAX_DROPS;i++) ids[i]=(float)i;
    GLES.glGenBuffers(1,&m_vbo); GLES.glGenVertexArrays(1,&m_vao);
    GLES.glBindVertexArray(m_vao);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(ids),ids,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,1,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);
    MG_LOG_I("Weather: prog=%u",m_prog);
    return m_prog!=0;
}
void WeatherRenderer::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_vbo) { GLES.glDeleteBuffers(1,&m_vbo); m_vbo=0; }
    if(m_vao) { GLES.glDeleteVertexArrays(1,&m_vao); m_vao=0; }
}
void WeatherRenderer::setWeather(WeatherType type,float intensity){
    m_type=type; m_intensity=intensity;
}
void WeatherRenderer::render(float dt,const float* viewPos,const float* viewProj){
    if(!m_prog||m_type==WeatherType::NONE||m_intensity<0.01f) return;
    m_time+=dt;
    int drops=(int)(MAX_DROPS*m_intensity);
    GLES.glUseProgram(m_prog);
    GLES.glUniformMatrix4fv(GLES.glGetUniformLocation(m_prog,"u_vp"),1,GL_FALSE,viewProj);
    GLES.glUniform3f(GLES.glGetUniformLocation(m_prog,"u_viewPos"),viewPos[0],viewPos[1],viewPos[2]);
    GLES.glUniform1f(GLES.glGetUniformLocation(m_prog,"u_time"),m_time);
    GLES.glUniform1f(GLES.glGetUniformLocation(m_prog,"u_intensity"),m_intensity);
    GLES.glEnable(GL_BLEND); GLES.glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glBindVertexArray(m_vao);
    GLES.glDrawArrays(GL_POINTS,0,drops);
    GLES.glBindVertexArray(0);
    GLES.glDisable(GL_BLEND);
}
} // MG
#endif
