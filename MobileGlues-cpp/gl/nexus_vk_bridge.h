// NEXUS_VK_RENDER (NVR) — Vulkan bridge header
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#ifndef NVR_NEXUS_VK_BRIDGE_H
#define NVR_NEXUS_VK_BRIDGE_H

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns 1 if NVR is in Vulkan-passthrough mode (ANGLE=Enabled).
 * In this mode: no EGL/GLES context is created — GPU is owned by VulkanMod.
 */
int nvr_is_active(void);

/**
 * Compile GLSL source to SPIRV using NVR's full Mali-optimised pipeline.
 *
 * Includes all preprocessing:
 *   - #line directive removal
 *   - atomic counter → SSBO emulation
 *   - samplerBuffer emulation
 *   - precision qualifiers
 *   - NVR macro injection
 *   - glslang optimiser
 *
 * @param glsl_source    Null-terminated GLSL source
 * @param glsl_type      GL_VERTEX_SHADER / GL_FRAGMENT_SHADER /
 *                       GL_COMPUTE_SHADER / GL_GEOMETRY_SHADER /
 *                       GL_TESS_CONTROL_SHADER / GL_TESS_EVALUATION_SHADER
 * @param out_spirv      Set to internal SPIRV word buffer (valid until
 *                       next call from the same thread)
 * @param out_word_count Number of uint32_t words in *out_spirv
 * @return 0 on success, -1 on compile error
 */
int nvr_compile_glsl_to_spirv(
    const char*      glsl_source,
    unsigned int     glsl_type,
    const uint32_t** out_spirv,
    size_t*          out_word_count
);

/** Release internal resources. Call on renderer shutdown. */
void nvr_shutdown(void);

#ifdef __cplusplus
}
#endif

#endif // NVR_NEXUS_VK_BRIDGE_H
