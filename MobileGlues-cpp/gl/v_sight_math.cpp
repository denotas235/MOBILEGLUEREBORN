// MobileGlues - gl/v_sight_math.cpp
// Phase 4: V-Sight Math & Trigonometric Look-Up Tables for Mali GPU
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#include "v_sight_math.h"
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846f
#endif

float g_sin_lut[LUT_SIZE];
float g_cos_lut[LUT_SIZE];

void v_sight_math_init() {
    for (int i = 0; i < LUT_SIZE; ++i) {
        float rad = (i * M_PI) / 180.0f;
        g_sin_lut[i] = std::sin(rad);
        g_cos_lut[i] = std::cos(rad);
    }
}

float fast_sin(float degrees) {
    int deg = static_cast<int>(degrees) % 360;
    if (deg < 0) deg += 360;
    return g_sin_lut[deg];
}

float fast_cos(float degrees) {
    int deg = static_cast<int>(degrees) % 360;
    if (deg < 0) deg += 360;
    return g_cos_lut[deg];
}
