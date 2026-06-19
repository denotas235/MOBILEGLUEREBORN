// MobileGlues - rendering/particle_renderer.cpp
// CPU-side particle simulation, GPU instanced billboard rendering
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "particle_renderer.h"
#include "log.h"
#include "../gles/loader.h"
#include <cstring>
#include <algorithm>

namespace MG {
ParticleRenderer& particles(){ static ParticleRenderer s; return s; }

static const char* VERT = R"(
#version 310 es
precision highp float;
layout(location=0) in vec4 a_pos;    // xyz=position w=age
layout(location=1) in vec4 a_vel;    // xyz=velocity w=lifetime
layout(location=2) in vec4 a_color;  // rgba
layout(location=3) in float a_size;
uniform mat4 u_vp;
out vec4 v_color;
out float v_age;
void main(){
    v_color=a_color;
    v_age=a_pos.w/a_vel.w;
    v_color.a*=1.0-v_age;
    gl_PointSize=max(1.0,a_size*(1.0-v_age*0.5));
    gl_Position=u_vp*vec4(a_pos.xyz,1.0);
}
)";
static const char* FRAG = R"(
#version 310 es
precision mediump float;
in vec4 v_color;
in float v_age;
out vec4 fragColor;
void main(){
    vec2 uv=gl_PointCoord*2.0-1.0;
    float d=dot(uv,uv);
    if(d>1.0) discard;
    fragColor=v_color*(1.0-d*0.5);
}
)";

static GLuint mkS(GLenum t,const char* s){
    GLuint sh=GLES.glCreateShader(t); if(!sh) return 0;
    GLES.glShaderSource(sh,1,&s,nullptr); GLES.glCompileShader(sh);
    GLint ok=0; GLES.glGetShaderiv(sh,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[256]={};GLES.glGetShaderInfoLog(sh,256,nullptr,l);MG_LOG_W("Particles: %s",l);GLES.glDeleteShader(sh);return 0;}
    return sh;
}

bool ParticleRenderer::init(int w,int h){
    m_w=w;m_h=h;
    memset(m_particles,0,sizeof(m_particles));
    // Create VBO for all particles
    GLES.glGenBuffers(1,&m_vbo);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferData(GL_ARRAY_BUFFER,MAX_PARTICLES*(GLsizeiptr)sizeof(Particle),nullptr,GL_DYNAMIC_DRAW);
    // VAO
    GLES.glGenVertexArrays(1,&m_vao); GLES.glBindVertexArray(m_vao);
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLsizei stride=(GLsizei)sizeof(Particle);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,4,GL_FLOAT,GL_FALSE,stride,(void*)0);
    GLES.glEnableVertexAttribArray(1); GLES.glVertexAttribPointer(1,4,GL_FLOAT,GL_FALSE,stride,(void*)16);
    GLES.glEnableVertexAttribArray(2); GLES.glVertexAttribPointer(2,4,GL_FLOAT,GL_FALSE,stride,(void*)32);
    GLES.glEnableVertexAttribArray(3); GLES.glVertexAttribPointer(3,1,GL_FLOAT,GL_FALSE,stride,(void*)48);
    GLES.glBindVertexArray(0);
    // Shader
    GLuint vs=mkS(GL_VERTEX_SHADER,VERT);
    GLuint fs=mkS(GL_FRAGMENT_SHADER,FRAG);
    if(vs&&fs){ m_prog=GLES.glCreateProgram();
        GLES.glAttachShader(m_prog,vs); GLES.glAttachShader(m_prog,fs); GLES.glLinkProgram(m_prog); }
    if(vs) GLES.glDeleteShader(vs);
    if(fs) GLES.glDeleteShader(fs);
    MG_LOG_I("Particles: max=%d prog=%u",MAX_PARTICLES,m_prog);
    return m_vbo!=0;
}

void ParticleRenderer::destroy(){
    if(m_prog){ GLES.glDeleteProgram(m_prog); m_prog=0; }
    if(m_vbo) { GLES.glDeleteBuffers(1,&m_vbo); m_vbo=0; }
    if(m_vao) { GLES.glDeleteVertexArrays(1,&m_vao); m_vao=0; }
    m_count=0;
}

void ParticleRenderer::emit(float x,float y,float z,float vx,float vy,float vz,
                             float r,float g,float b,float life,float size){
    if(m_count>=MAX_PARTICLES) return;
    auto& p=m_particles[m_count++];
    p={x,y,z,0.0f, vx,vy,vz,life, r,g,b,1.0f, size,{0,0,0}};
}

void ParticleRenderer::update(float dt){
    int live=0;
    for(int i=0;i<m_count;i++){
        auto& p=m_particles[i];
        p.w+=dt; // age
        if(p.w>=p.vw) continue; // dead
        p.x+=p.vx*dt; p.y+=p.vy*dt-4.9f*dt*dt; p.z+=p.vz*dt;
        p.vy-=9.8f*dt; // gravity
        m_particles[live++]=p;
    }
    m_count=live;
}

void ParticleRenderer::render(const float* viewProj){
    if(!m_prog||!m_vbo||m_count==0) return;
    GLES.glBindBuffer(GL_ARRAY_BUFFER,m_vbo);
    GLES.glBufferSubData(GL_ARRAY_BUFFER,0,m_count*(GLsizeiptr)sizeof(Particle),m_particles);
    GLES.glUseProgram(m_prog);
    GLES.glUniformMatrix4fv(GLES.glGetUniformLocation(m_prog,"u_vp"),1,GL_FALSE,viewProj);
    GLES.glEnable(GL_BLEND); GLES.glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
    GLES.glDisable(GL_DEPTH_TEST);
    GLES.glBindVertexArray(m_vao);
    GLES.glDrawArrays(GL_POINTS,0,m_count);
    GLES.glBindVertexArray(0);
    GLES.glDisable(GL_BLEND);
}
} // MG
#endif
