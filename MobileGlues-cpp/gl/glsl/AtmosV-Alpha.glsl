// AtmosV-Alpha.glsl - MobileGlues Phase 4 fog & atmosphere shader
// Uses derivatives (dFdx, dFdy) and limits for foveated atmosphere masking.
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#version 300 es
precision mediump float;

in vec3 v_position;
in vec2 v_texCoord;

out vec4 fragColor;

uniform sampler2D u_texture;
uniform vec3 u_viewPos;
uniform vec4 u_fogColor;
uniform float u_fogDensity;

void main() {
    float distance = length(v_position - u_viewPos);
    
    // Limits and derivative based atmospheric fog factors
    // fogFactor = exp(-(d * density)^2)
    float fogFactor = exp(-pow(distance * u_fogDensity, 2.0));
    fogFactor = clamp(fogFactor, 0.0, 1.0);

    vec4 texColor = texture(u_texture, v_texCoord);
    
    // Variable Rate Shading (VRS) emulation using derivatives
    // Skip complex lighting if color change rate is low
    float dx = dFdx(texColor.r);
    float dy = dFdy(texColor.g);
    float rateOfChange = abs(dx) + abs(dy);
    
    vec4 finalColor;
    if (rateOfChange < 0.01) {
        // Fast path: blend fog directly
        finalColor = mix(u_fogColor, texColor, fogFactor);
    } else {
        // Full path: apply detailed light/fog blend
        finalColor = mix(u_fogColor, texColor * 1.05, fogFactor);
    }
    
    fragColor = finalColor;
}
