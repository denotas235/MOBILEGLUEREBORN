// MobileGlues - gl/shader_sanitizer.cpp
// Phase 1: Mali GLES 3.0 Shader Sanitizer
// Metodo: mesmo da branch main — chama GLSLtoGLSLES (glslang → SPIRV → GLSL ES)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_sanitizer.h"
#include "glsl/glsl_for_es.h"

#include <jni.h>
#include <string>
#include <android/log.h>

#define SANITIZER_TAG "MG_ShaderSanitizer"

// ---------------------------------------------------------------------------
// Detecta o tipo de shader a partir do codigo-fonte GLSL.
// Necessario porque o JNI recebe apenas a String — sem o GLenum do driver.
// ---------------------------------------------------------------------------
static GLenum detectShaderType(const std::string& src) {
    // Compute shaders
    if (src.find("local_size_x")    != std::string::npos ||
        src.find("layout(local_size") != std::string::npos) {
        return GL_COMPUTE_SHADER;
    }
    // Vertex shaders: escrevem em gl_Position
    if (src.find("gl_Position")   != std::string::npos ||
        src.find("gl_PointSize")  != std::string::npos) {
        return GL_VERTEX_SHADER;
    }
    // Default: fragment
    return GL_FRAGMENT_SHADER;
}

// ---------------------------------------------------------------------------
// Verifica se o shader ja esta em formato GLES e nao precisa de conversao.
// ---------------------------------------------------------------------------
static bool isAlreadyGLES(const std::string& src) {
    // "#version NNN es"
    auto pos = src.find("#version");
    if (pos == std::string::npos) return false;
    auto eol = src.find(n, pos);
    std::string vline = src.substr(pos, eol - pos);
    return vline.find(" es") != std::string::npos ||
           src.find("#version 100") != std::string::npos;
}

// ---------------------------------------------------------------------------
// sanitizeForMaliGLES
//
// Converte GLSL desktop para GLSL ES 3.0 usando o mesmo pipeline da branch
// main (glsl_for_es.cpp: glslang → SPIRV → SPIRV-Cross → GLSL ES).
//
// Garante:
//   • #version 300 es como primeira linha
//   • precision highp float/int injetados automaticamente (forceSupporterOutput)
//   • gl_FragColor traduzido para out vec4 pelo SPIRV-Cross
//   • texture2D() → texture() pelo SPIRV-Cross
//   • layout(binding=N) removidos (removeLayoutBinding)
//
// Em caso de falha de conversao, retorna o source original para nao crashar.
// ---------------------------------------------------------------------------
std::string sanitizeForMaliGLES(const std::string& source) {
    if (source.empty()) return source;

    if (isAlreadyGLES(source)) {
        __android_log_print(ANDROID_LOG_DEBUG, SANITIZER_TAG,
            "Shader ja e GLES (%zu bytes) — pass-through", source.size());
        return source;
    }

    GLenum  shaderType  = detectShaderType(source);
    int     glslVersion = getGLSLVersion(source.c_str());
    if (glslVersion <= 0) glslVersion = 150;

    int return_code = -1;
    std::string converted = GLSLtoGLSLES(
        source.c_str(), shaderType,
        /*essl_version=*/300,
        /*glsl_version=*/glslVersion,
        return_code);

    if (return_code >= 0 && !converted.empty()) {
        __android_log_print(ANDROID_LOG_DEBUG, SANITIZER_TAG,
            "Conversao OK (%zu → %zu bytes, rc=%d)",
            source.size(), converted.size(), return_code);
        return converted;
    }

    __android_log_print(ANDROID_LOG_WARN, SANITIZER_TAG,
        "Conversao falhou (rc=%d) — usando source original", return_code);
    return source;
}

// ---------------------------------------------------------------------------
// Ponte JNI
// Classe Java : com.nexus.MobileGlues
// Metodo      : String sanitizeShaderNative(String shaderSource)
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_MobileGlues_sanitizeShaderNative(JNIEnv* env, jobject /*obj*/,
                                                jstring shaderSource)
{
    if (!shaderSource) {
        __android_log_print(ANDROID_LOG_ERROR, SANITIZER_TAG,
            "sanitizeShaderNative: source nulo recebido");
        return env->NewStringUTF("");
    }

    const char* raw = env->GetStringUTFChars(shaderSource, nullptr);
    if (!raw) {
        __android_log_print(ANDROID_LOG_ERROR, SANITIZER_TAG,
            "sanitizeShaderNative: GetStringUTFChars falhou");
        return env->NewStringUTF("");
    }

    std::string result = sanitizeForMaliGLES(std::string(raw));
    env->ReleaseStringUTFChars(shaderSource, raw);
    return env->NewStringUTF(result.c_str());
}
