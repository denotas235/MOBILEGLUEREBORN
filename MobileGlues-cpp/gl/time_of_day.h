#pragma once
// MobileGlues - gl/time_of_day.h
// Time-of-Day sky state — pure math, no GL dependency
// SPDX-License-Identifier: LGPL-2.1-only
#include <cmath>

namespace MG {

struct SkyState {
    float sunColorR, sunColorG, sunColorB;
    float godrayColorR, godrayColorG, godrayColorB;
    float godrayIntensity;
    float sunElevation;    // 0.0=horizon  1.0=zenith
    float ambientIntensity;
};

// timeOfDay: 0.0=midnight  0.25=sunrise  0.5=noon  0.75=sunset  1.0=midnight
inline SkyState computeSkyState(float t) {
    SkyState s{};
    auto lerp = [](float a, float b, float f){ return a + f*(b-a); };
    if (t < 0.25f) {
        float f = t / 0.25f;
        s.sunColorR=lerp(0.f,1.0f,f); s.sunColorG=lerp(0.f,0.3f,f); s.sunColorB=lerp(0.f,0.05f,f);
        s.godrayColorR=1.0f; s.godrayColorG=0.4f; s.godrayColorB=0.1f;
        s.godrayIntensity=0.8f*f; s.sunElevation=f*0.3f; s.ambientIntensity=0.05f+0.3f*f;
    } else if (t < 0.5f) {
        float f=(t-0.25f)/0.25f;
        s.sunColorR=1.f; s.sunColorG=lerp(0.7f,0.95f,f); s.sunColorB=lerp(0.3f,0.8f,f);
        s.godrayColorR=1.f; s.godrayColorG=0.9f; s.godrayColorB=0.7f;
        s.godrayIntensity=0.4f; s.sunElevation=0.3f+f*0.7f; s.ambientIntensity=0.4f+0.3f*f;
    } else if (t < 0.75f) {
        float f=(t-0.5f)/0.25f;
        s.sunColorR=1.f; s.sunColorG=lerp(0.95f,0.5f,f); s.sunColorB=lerp(0.8f,0.1f,f);
        s.godrayColorR=1.f; s.godrayColorG=0.6f; s.godrayColorB=0.2f;
        s.godrayIntensity=lerp(0.4f,1.0f,f); s.sunElevation=lerp(1.0f,0.3f,f); s.ambientIntensity=lerp(0.7f,0.3f,f);
    } else {
        float f=(t-0.75f)/0.25f;
        s.sunColorR=lerp(1.f,0.f,f); s.sunColorG=lerp(0.3f,0.f,f); s.sunColorB=lerp(0.05f,0.f,f);
        s.godrayColorR=0.8f; s.godrayColorG=0.2f; s.godrayColorB=0.05f;
        s.godrayIntensity=lerp(1.2f,0.f,f); s.sunElevation=lerp(0.3f,0.f,f); s.ambientIntensity=lerp(0.3f,0.05f,f);
    }
    return s;
}

} // namespace MG
