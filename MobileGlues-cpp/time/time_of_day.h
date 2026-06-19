// MobileGlues - time/time_of_day.h
// Time-of-Day sky state computation
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef TIME_OF_DAY_H
#define TIME_OF_DAY_H

#include <cmath>
#include <algorithm>

struct SkyState {
    float sunElevation = 0.5f;     // 0.0 = horizon, 1.0 = zenith
    float sunColorR = 1.0f;
    float sunColorG = 0.9f;
    float sunColorB = 0.7f;
    float ambientR = 0.3f;
    float ambientG = 0.3f;
    float ambientB = 0.4f;
};

inline SkyState computeSkyState(float timeOfDay) {
    // timeOfDay: 0.0 = midnight, 0.5 = noon, 1.0 = midnight again
    SkyState s;
    
    // Sun elevation based on time (sine wave from 0 to 1)
    // Peak at noon (0.5), zero at midnight (0.0 and 1.0)
    float angle = (timeOfDay - 0.25f) * 2.0f * 3.14159265f;
    s.sunElevation = std::max(0.0f, std::sin(angle));
    
    // Sun color: warm orange/yellow during day, blue at night
    float dayness = s.sunElevation;
    s.sunColorR = 0.2f + dayness * 0.8f;  // 0.2 to 1.0
    s.sunColorG = 0.2f + dayness * 0.7f;  // 0.2 to 0.9
    s.sunColorB = 0.4f + dayness * 0.3f;  // 0.4 to 0.7
    
    // Ambient: blue-ish during day, dark at night
    s.ambientR = 0.1f + dayness * 0.2f;
    s.ambientG = 0.1f + dayness * 0.2f;
    s.ambientB = 0.2f + dayness * 0.2f;
    
    return s;
}

#endif // TIME_OF_DAY_H
