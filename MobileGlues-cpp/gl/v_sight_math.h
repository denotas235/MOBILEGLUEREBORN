// MobileGlues - gl/v_sight_math.h
// Phase 4: V-Sight Math & Trigonometric Look-Up Tables for Mali GPU
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef V_SIGHT_MATH_H
#define V_SIGHT_MATH_H

#define LUT_SIZE 360

extern float g_sin_lut[LUT_SIZE];
extern float g_cos_lut[LUT_SIZE];

// Initialize Look-Up Tables
void v_sight_math_init();

// Fast sin and cos implementation using degrees
float fast_sin(float degrees);
float fast_cos(float degrees);

#endif // V_SIGHT_MATH_H
