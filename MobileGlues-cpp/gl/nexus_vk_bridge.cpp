// NEXUS_VK_RENDER (NVR) — Vulkan bridge implementation
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#include "nexus_vk_bridge.h"
#include "glsl/glsl_for_es.h"
#include "log.h"
#include "../config/settings.h"
#include "../version.h"

#include <vector>
#include <string>
#include <mutex>

// Android JNI — only compiled when targeting Android
#if defined(__ANDROID__)
#include <jni.h>
#endif

#define DEBUG 0

// ── Thread-local SPIRV storage ────────────────────────────────────────────
// Each thread keeps its own last-compiled SPIRV so concurrent shader
// compilations (e.g. background thread in Minecraft) don't race.
static thread_local std::vector<uint32_t> tl_last_spirv;

extern "C" {

// ─────────────────────────────────────────────────────────────────────────
// nvr_is_active
// ─────────────────────────────────────────────────────────────────────────
int nvr_is_active(void) {
    return (global_settings.angle == AngleMode::Enabled) ? 1 : 0;
}

// ─────────────────────────────────────────────────────────────────────────
// nvr_compile_glsl_to_spirv
// ─────────────────────────────────────────────────────────────────────────
int nvr_compile_glsl_to_spirv(
    const char*      glsl_source,
    unsigned int     glsl_type,
    const uint32_t** out_spirv,
    size_t*          out_word_count
) {
    if (!glsl_source || !out_spirv || !out_word_count) {
        LOG_E("[NVR] nvr_compile_glsl_to_spirv: null argument");
        return -1;
    }

    std::string src(glsl_source);

    // Ensure a #version directive exists (glslang requires it)
    int glsl_version = getGLSLVersion(glsl_source);
    if (glsl_version == -1) {
        src.insert(0, "#version 150\n");
        glsl_version = 150;
    }

    const char* srcs[] = { src.c_str() };
    int errc = 0;

    std::vector<uint32_t> spirv = glsl_to_spirv(
        static_cast<GLenum>(glsl_type),
        glsl_version,
        srcs,
        errc
    );

    if (errc != 0 || spirv.empty()) {
        LOG_E("[NVR] GLSL→SPIRV compilation failed (errc=%d, type=0x%x)", errc, glsl_type);
        return -1;
    }

    tl_last_spirv = std::move(spirv);
    *out_spirv     = tl_last_spirv.data();
    *out_word_count = tl_last_spirv.size();

    LOG_D("[NVR] GLSL→SPIRV OK — %zu words, type=0x%x", *out_word_count, glsl_type);
    return 0;
}

// ─────────────────────────────────────────────────────────────────────────
// nvr_shutdown
// ─────────────────────────────────────────────────────────────────────────
void nvr_shutdown(void) {
    tl_last_spirv.clear();
    tl_last_spirv.shrink_to_fit();
    LOG_V("[NVR] Bridge shutdown.");
}

// ─────────────────────────────────────────────────────────────────────────
// JNI entry points — VulkanMod calls these from Java/Kotlin
// Class: com.nexus.vulkan.NexusVkBridge
// ─────────────────────────────────────────────────────────────────────────
#if defined(__ANDROID__)

JNIEXPORT jint JNICALL
Java_com_nexus_vulkan_NexusVkBridge_isActive(JNIEnv*, jclass) {
    return static_cast<jint>(nvr_is_active());
}

JNIEXPORT jbyteArray JNICALL
Java_com_nexus_vulkan_NexusVkBridge_compileGlslToSpirv(
    JNIEnv*  env,
    jclass,
    jstring  jGlslSource,
    jint     jGlslType
) {
    if (!jGlslSource) return nullptr;

    const char* src = env->GetStringUTFChars(jGlslSource, nullptr);
    if (!src) return nullptr;

    const uint32_t* spirv_ptr  = nullptr;
    size_t          word_count = 0;

    int result = nvr_compile_glsl_to_spirv(
        src,
        static_cast<unsigned int>(jGlslType),
        &spirv_ptr,
        &word_count
    );
    env->ReleaseStringUTFChars(jGlslSource, src);

    if (result != 0 || !spirv_ptr || word_count == 0) return nullptr;

    const jsize byte_count = static_cast<jsize>(word_count * sizeof(uint32_t));
    jbyteArray  arr        = env->NewByteArray(byte_count);
    if (!arr) return nullptr;

    env->SetByteArrayRegion(arr, 0, byte_count,
                            reinterpret_cast<const jbyte*>(spirv_ptr));
    return arr;
}

#endif // __ANDROID__

} // extern "C"
