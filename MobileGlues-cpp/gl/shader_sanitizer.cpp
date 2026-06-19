// MobileGlues - gl/shader_sanitizer.cpp
// Phase 1: Mali GLES 3.0 Shader Sanitizer + Diagnostic Log
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_sanitizer.h"

#include <jni.h>
#include <string>
#include <sstream>
#include <cstdio>
#include <cstdarg>
#include <ctime>
#include <cstring>
#include <sys/stat.h>
#include <android/log.h>

#define SANITIZER_TAG  "MG_ShaderSanitizer"
#define SANITIZER_LOG  "/sdcard/MG/shader_sanitizer.log"
#define MAX_LOG_BYTES  (4 * 1024 * 1024)

// ─────────────────────────────────────────────────────────────────────────────
// Sistema de Log Diagnóstico (preservado para debugging)
// ─────────────────────────────────────────────────────────────────────────────
static void ensure_log_dir() {
    mkdir("/sdcard/MG", 0755);
}

static void rotate_log_if_needed() {
    struct stat st{};
    if (stat(SANITIZER_LOG, &st) == 0 && st.st_size > MAX_LOG_BYTES) {
        rename(SANITIZER_LOG, SANITIZER_LOG ".bak");
    }
}

static void write_log(const char* level, const char* fmt, ...) {
    va_list ap1;
    va_start(ap1, fmt);
    __android_log_vprint(ANDROID_LOG_DEBUG, SANITIZER_TAG, fmt, ap1);
    va_end(ap1);

    ensure_log_dir();
    rotate_log_if_needed();

    FILE* f = fopen(SANITIZER_LOG, "a");
    if (!f) return;

    time_t now = time(nullptr);
    struct tm* t = localtime(&now);
    char ts[32];
    strftime(ts, sizeof(ts), "%Y-%m-%dT%H:%M:%S", t);
    fprintf(f, "[%s] [%-6s] ", ts, level);

    va_list ap2;
    va_start(ap2, fmt);
    vfprintf(f, fmt, ap2);
    va_end(ap2);

    fputc('\n', f);
    fclose(f);
}

// ─────────────────────────────────────────────────────────────────────────────
// sanitizeForMaliGLES — pass-through (sem transformacoes)
//
// Motivo da desativacao:
//   A injecao de 'precision highp sampler2D;' causava falhas de compilacao
//   em cadeia no driver ANGLE/Mali-G52:
//     ERROR: 0:2: '' : No precision specified for (float)
//   ANGLE (GLES 3.2) rejeita qualificadores de precisao para tipos opacos
//   (samplers) — estes sao implicitamente lowp/mediump no standard GLES 3.x.
//
//   Adicionalmente, o fallback ja funciona: quando o shader sanitizado falha,
//   o motor tenta o original e este compila. O overhead duplo nao tem vantagem
//   se a taxa de sucesso da sanitizacao e proxima de zero.
//
//   Proximos passos: optimizacoes na pipeline de rendering (buffers, textures,
//   draw calls) em vez de transformacoes GLSL em runtime.
// ─────────────────────────────────────────────────────────────────────────────
std::string sanitizeForMaliGLES(const std::string& source) {
    write_log("PASS", "shader pass-through (%zu bytes) — sanitizacao desativada", source.size());
    return source;
}

// ─────────────────────────────────────────────────────────────────────────────
// Ponte JNI
// Classe Java: com.nexus.MobileGlues
// Metodo:      String sanitizeShaderNative(String shaderSource)
// ─────────────────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_MobileGlues_sanitizeShaderNative(JNIEnv* env, jobject /*obj*/,
                                                jstring shaderSource)
{
    if (!shaderSource) {
        write_log("ERROR", "sanitizeShaderNative: source nulo recebido");
        return env->NewStringUTF("");
    }
    const char* raw = env->GetStringUTFChars(shaderSource, nullptr);
    if (!raw) {
        write_log("ERROR", "sanitizeShaderNative: GetStringUTFChars falhou");
        return env->NewStringUTF("");
    }
    std::string result = sanitizeForMaliGLES(std::string(raw));
    env->ReleaseStringUTFChars(shaderSource, raw);
    return env->NewStringUTF(result.c_str());
}
