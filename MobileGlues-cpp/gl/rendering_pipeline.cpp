// MobileGlues - gl/rendering_pipeline.cpp
// PLS Godrays + Rayleigh Sky + ACES Tonemapping
// SPDX-License-Identifier: LGPL-2.1-only
#ifndef __APPLE__
#include "rendering_pipeline.h"
#include "time_of_day.h"
#include "extension_scanner.h"
#include "../gles/loader.h"
#include "log.h"
#include <cmath>
#include <cstring>

namespace MG {
namespace RenderPipeline {

static const char* VERT_QUAD = R"glsl(
#version 310 es
precision highp float;
layout(location=0) in vec2 a_pos;
out vec2 v_uv;
void main(){ v_uv=a_pos*0.5+0.5; gl_Position=vec4(a_pos,0.0,1.0); }
)glsl";

static const char* FRAG_GODRAY = R"glsl(
#version 310 es
precision highp float;
in vec2 v_uv;
uniform sampler2D u_scene;
uniform vec2  u_sunPos;
uniform float u_exposure;
uniform vec3  u_sunColor;
uniform float u_sunElev;
out vec4 fragColor;
void main(){
    if(u_sunElev<0.02){fragColor=vec4(0.0);return;}
    const int N=80;
    const float DECAY=0.97,WEIGHT=0.004,DENSITY=0.96;
    vec2 delta=(v_uv-u_sunPos)*(1.0/float(N)*DENSITY);
    vec2 tc=v_uv;
    float decay=1.0;
    vec3 acc=vec3(0.0);
    for(int i=0;i<N;i++){
        tc-=delta;
        vec3 s=texture(u_scene,clamp(tc,0.0,1.0)).rgb;
        float lum=dot(s,vec3(0.2126,0.7152,0.0722));
        acc+=s*decay*WEIGHT*max(lum-0.3,0.0)*3.0;
        decay*=DECAY;
    }
    float boost=smoothstep(0.0,0.3,u_sunElev);
    fragColor=vec4(acc*u_exposure*u_sunColor*boost,1.0);
}
)glsl";

static const char* FRAG_SKY = R"glsl(
#version 310 es
precision highp float;
in vec2 v_uv;
uniform vec3 u_sunDir;
uniform vec2 u_screen;
out vec4 fragColor;
const vec3 RC=vec3(5.5e-6,13.0e-6,22.4e-6);
const vec3 MC=vec3(21e-6);
const float MG=0.758,AR=6420e3,PR=6360e3;
float rPhase(float c){return(3.0/(16.0*3.14159))*(1.0+c*c);}
float mPhase(float c,float g){float g2=g*g;return(3.0/(8.0*3.14159))*((1.0-g2)*(1.0+c*c))/((2.0+g2)*pow(abs(1.0+g2-2.0*g*c),1.5));}
vec3 ACESFilm(vec3 x){return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0);}
void main(){
    vec2 uv=gl_FragCoord.xy/u_screen;
    vec3 ray=normalize(vec3((uv*2.0-1.0)*vec2(1.7778,1.0),-1.0));
    vec3 sun=normalize(u_sunDir);
    float up=max(dot(ray,vec3(0,1,0)),0.001);
    float ad=sqrt(max(AR*AR-PR*PR*(1.0-up*up),0.0));
    float ct=dot(ray,sun);
    vec3 r=RC*rPhase(ct)*ad;
    vec3 m=MC*mPhase(ct,MG)*ad;
    vec3 ext=exp(-(RC+MC)*ad*0.000001);
    vec3 col=(r+m)*ext*20.0;
    col=ACESFilm(col);
    fragColor=vec4(pow(col,vec3(1.0/2.2)),1.0);
}
)glsl";

static const char* FRAG_COMPOSITE = R"glsl(
#version 310 es
precision highp float;
in vec2 v_uv;
uniform sampler2D u_scene;
uniform sampler2D u_godrays;
uniform sampler2D u_sky;
uniform vec2 u_screen;
out vec4 fragColor;
vec3 ACES(vec3 x){return clamp((x*(2.51*x+0.03))/(x*(2.43*x+0.59)+0.14),0.0,1.0);}
void main(){
    vec2 uv=gl_FragCoord.xy/u_screen;
    vec3 scene=texture(u_scene,uv).rgb;
    vec3 gr=texture(u_godrays,uv).rgb;
    vec3 sky=texture(u_sky,uv).rgb;
    vec3 lit=scene+gr*0.18+sky*0.05;
    float lum=dot(lit,vec3(0.2126,0.7152,0.0722));
    lit*=clamp(0.5/(lum+0.001),0.3,3.0);
    fragColor=vec4(pow(ACES(lit),vec3(1.0/2.2)),1.0);
}
)glsl";

static GLuint compileShader(GLenum type,const char* src){
    GLuint s=GLES.glCreateShader(type); if(!s)return 0;
    GLES.glShaderSource(s,1,&src,nullptr); GLES.glCompileShader(s);
    GLint ok=0; GLES.glGetShaderiv(s,GL_COMPILE_STATUS,&ok);
    if(!ok){char l[512]={};GLES.glGetShaderInfoLog(s,512,nullptr,l);MG_LOG_E("shader: %s",l);GLES.glDeleteShader(s);return 0;}
    return s;
}
static GLuint linkProg(GLuint v,GLuint f){
    if(!v||!f)return 0;
    GLuint p=GLES.glCreateProgram();
    GLES.glAttachShader(p,v); GLES.glAttachShader(p,f); GLES.glLinkProgram(p);
    GLint ok=0; GLES.glGetProgramiv(p,GL_LINK_STATUS,&ok);
    if(!ok){char l[512]={};GLES.glGetProgramInfoLog(p,512,nullptr,l);MG_LOG_E("prog: %s",l);GLES.glDeleteProgram(p);return 0;}
    return p;
}

static struct {
    bool init=false,pls=false,cbf=false;
    GLuint progGodray=0,progSky=0,progComp=0;
    GLuint qVAO=0,qVBO=0;
    SkyState sky{};
} G;

static void buildQuad(){
    static const float Q[]={-1,-1,1,-1,-1,1,1,1};
    GLES.glGenVertexArrays(1,&G.qVAO); GLES.glBindVertexArray(G.qVAO);
    GLES.glGenBuffers(1,&G.qVBO); GLES.glBindBuffer(GL_ARRAY_BUFFER,G.qVBO);
    GLES.glBufferData(GL_ARRAY_BUFFER,sizeof(Q),Q,GL_STATIC_DRAW);
    GLES.glEnableVertexAttribArray(0); GLES.glVertexAttribPointer(0,2,GL_FLOAT,GL_FALSE,0,nullptr);
    GLES.glBindVertexArray(0);
}

void init(){
    if(G.init)return; G.init=true;
    G.pls=get_gpu_capabilities().has_pls;
    G.cbf=has_extension("GL_EXT_color_buffer_float");
    GLuint v=compileShader(GL_VERTEX_SHADER,VERT_QUAD);
    if(G.cbf){GLuint f=compileShader(GL_FRAGMENT_SHADER,FRAG_GODRAY);G.progGodray=linkProg(v,f);if(f)GLES.glDeleteShader(f);}
    {GLuint f=compileShader(GL_FRAGMENT_SHADER,FRAG_SKY);G.progSky=linkProg(v,f);if(f)GLES.glDeleteShader(f);}
    {GLuint f=compileShader(GL_FRAGMENT_SHADER,FRAG_COMPOSITE);G.progComp=linkProg(v,f);if(f)GLES.glDeleteShader(f);}
    if(v)GLES.glDeleteShader(v);
    buildQuad();
    MG_LOG_I("RenderPipeline: PLS=%d CBF=%d godray=%u sky=%u comp=%u",(int)G.pls,(int)G.cbf,G.progGodray,G.progSky,G.progComp);
}

void updateSkyState(float tod){ G.sky=computeSkyState(tod); }

bool runGodrayPass(GLuint sceneTex,float sx,float sy,float sw,float sh){
    if(!G.progGodray)return false;
    GLES.glUseProgram(G.progGodray);
    GLES.glActiveTexture(GL_TEXTURE0); GLES.glBindTexture(GL_TEXTURE_2D,sceneTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(G.progGodray,"u_scene"),0);
    GLES.glUniform2f(GLES.glGetUniformLocation(G.progGodray,"u_sunPos"),sx/sw,sy/sh);
    GLES.glUniform1f(GLES.glGetUniformLocation(G.progGodray,"u_exposure"),1.0f);
    GLES.glUniform3f(GLES.glGetUniformLocation(G.progGodray,"u_sunColor"),G.sky.sunColorR,G.sky.sunColorG,G.sky.sunColorB);
    GLES.glUniform1f(GLES.glGetUniformLocation(G.progGodray,"u_sunElev"),G.sky.sunElevation);
    GLES.glBindVertexArray(G.qVAO); GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4); GLES.glBindVertexArray(0);
    return true;
}

bool runSkyPass(float sw,float sh){
    if(!G.progSky)return false;
    GLES.glUseProgram(G.progSky);
    float elev=G.sky.sunElevation;
    GLES.glUniform3f(GLES.glGetUniformLocation(G.progSky,"u_sunDir"),0.f,elev,(float)std::sqrt(std::max(0.f,1.f-elev*elev)));
    GLES.glUniform2f(GLES.glGetUniformLocation(G.progSky,"u_screen"),sw,sh);
    GLES.glBindVertexArray(G.qVAO); GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4); GLES.glBindVertexArray(0);
    return true;
}

bool runCompositePass(GLuint godrayTex,GLuint skyTex,float sw,float sh){
    if(!G.progComp)return false;
    GLES.glUseProgram(G.progComp);
    GLES.glActiveTexture(GL_TEXTURE1); GLES.glBindTexture(GL_TEXTURE_2D,godrayTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(G.progComp,"u_godrays"),1);
    GLES.glActiveTexture(GL_TEXTURE2); GLES.glBindTexture(GL_TEXTURE_2D,skyTex);
    GLES.glUniform1i(GLES.glGetUniformLocation(G.progComp,"u_sky"),2);
    GLES.glUniform2f(GLES.glGetUniformLocation(G.progComp,"u_screen"),sw,sh);
    GLES.glBindVertexArray(G.qVAO); GLES.glDrawArrays(GL_TRIANGLE_STRIP,0,4); GLES.glBindVertexArray(0);
    return true;
}

bool hasPLS(){return G.pls;}
bool hasGodraySupport(){return G.progGodray!=0;}

} // RenderPipeline
} // MG
#endif
