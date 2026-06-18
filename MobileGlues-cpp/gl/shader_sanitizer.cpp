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
#include <cstdarg>   // va_list, va_start, va_end — obrigatório para write_log
#include <ctime>
#include <cstring>
#include <sys/stat.h>
#include <android/log.h>

#define SANITIZER_TAG  "MG_ShaderSanitizer"
#define SANITIZER_LOG  "/sdcard/MG/shader_sanitizer.log"
#define MAX_LOG_BYTES  (4 * 1024 * 1024)   // Rota ficheiro após 4 MB

// ─────────────────────────────────────────────────────────────────────────────
// Extensões que a Mali-G52 NÃO suporta dentro de shaders.
// Se qualquer uma destas aparecer ANTES do #version (bug de código legado),
// o driver lança "extension not supported" na linha 0:1 ou 0:2 e empurra
// o #version para a posição errada, quebrando toda a pipeline de renderização.
// ─────────────────────────────────────────────────────────────────────────────
static const char* const UNSUPPORTED_EXTENSIONS[] = {
    "GL_EXT_texture_compression_astc",
    "GL_KHR_texture_compression_astc_ldr",
    "GL_KHR_texture_compression_astc_hdr",
    nullptr
};

// ─────────────────────────────────────────────────────────────────────────────
// Sistema de Log Diagnóstico
// Duplo output: logcat Android + ficheiro /sdcard/MG/shader_sanitizer.log
// Rota automaticamente ao atingir MAX_LOG_BYTES (guarda cópia como .bak).
// Se /sdcard/ não for acessível, continua em silêncio (sem crash).
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
    // 1. Logcat Android
    va_list ap1;
    va_start(ap1, fmt);
    __android_log_vprint(ANDROID_LOG_DEBUG, SANITIZER_TAG, fmt, ap1);
    va_end(ap1);

    // 2. Ficheiro /sdcard/MG/shader_sanitizer.log
    ensure_log_dir();
    rotate_log_if_needed();

    FILE* f = fopen(SANITIZER_LOG, "a");
    if (!f) return;   // /sdcard não acessível — continua sem crash

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

// Primeiros 80 caracteres do shader numa única linha (para identificação no log)
static std::string shader_fingerprint(const std::string& src) {
    std::string fp;
    fp.reserve(80);
    for (char c : src) {
        if (fp.size() >= 80) { fp += "..."; break; }
        fp += (c == '\n' || c == '\r') ? ' ' : c;
    }
    return fp;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers internos
// ─────────────────────────────────────────────────────────────────────────────
static bool line_has_unsupported_ext(const std::string& line) {
    for (int i = 0; UNSUPPORTED_EXTENSIONS[i]; ++i)
        if (line.find(UNSUPPORTED_EXTENSIONS[i]) != std::string::npos)
            return true;
    return false;
}

static const char* find_ext_name(const std::string& line) {
    for (int i = 0; UNSUPPORTED_EXTENSIONS[i]; ++i)
        if (line.find(UNSUPPORTED_EXTENSIONS[i]) != std::string::npos)
            return UNSUPPORTED_EXTENSIONS[i];
    return "(desconhecida)";
}

// ─────────────────────────────────────────────────────────────────────────────
// sanitizeForMaliGLES — núcleo do motor
//
// Algoritmo linha-a-linha SEM std::regex para robustez máxima.
// Garante que o output começa SEMPRE com "#version 300 es\n" como
// primeiro byte absoluto — qualquer desvio causa crash no driver Mali.
// ─────────────────────────────────────────────────────────────────────────────
std::string sanitizeForMaliGLES(const std::string& source) {
    std::istringstream stream(source);
    std::string line, body;
    body.reserve(source.size());

    bool had_desktop_version = false;
    bool had_astc             = false;
    int  astc_removed         = 0;
    int  tex2d_replaced       = 0;
    int  line_num             = 0;
    std::string orig_version;
    std::string first_bad_ext;

    while (std::getline(stream, line)) {
        ++line_num;

        // 1. Strip leading whitespace / \r
        size_t first = line.find_first_not_of(" \t\r");
        if (first == std::string::npos) continue;   // linha vazia — pular
        if (first > 0) line = line.substr(first);

        // 2. Remover extensões ASTC não suportadas na Mali
        if (line.find("#extension") != std::string::npos &&
            line_has_unsupported_ext(line))
        {
            const char* ext = find_ext_name(line);
            if (!had_astc) { had_astc = true; first_bad_ext = ext; }
            ++astc_removed;
            write_log("WARN",
                "Linha %d: extensao bloqueada [%s] — causaria erro 0:%d no driver Mali",
                line_num, ext, line_num);
            continue;
        }

        // 3. Descartar #version do Desktop GL (será substituída por 300 es)
        if (line.find("#version") != std::string::npos) {
            if (!had_desktop_version) {
                had_desktop_version = true;
                orig_version = line;
                write_log("INFO",
                    "Linha %d: versao Desktop GL [%s] removida — sera forcado '#version 300 es'",
                    line_num, line.c_str());
            }
            continue;
        }

        // 4. texture2D() → texture()  (obsoleto no GLES 3.0+)
        for (size_t pos = 0;
             (pos = line.find("texture2D(", pos)) != std::string::npos; )
        {
            line.replace(pos, 10, "texture(");
            pos += 8;
            ++tex2d_replaced;
        }

        // 5. Acumular linha no corpo
        body += line;
        body += '\n';
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Montar output: boilerplate GLES 3.0 GARANTIDAMENTE na linha 1.
    // "#version" é o primeiro byte — sem espaço, sem BOM, sem nada antes.
    // ─────────────────────────────────────────────────────────────────────────
    std::string result;
    result.reserve(body.size() + 128);
    result =
        "#version 300 es\n"
        "precision highp float;\n"
        "precision highp int;\n"
        "precision highp sampler2D;\n";
    result += body;

    // Relatório final
    if (had_desktop_version || had_astc || tex2d_replaced > 0) {
        write_log("REPORT",
            "=== Sanitizacao OK | linhas=%d versao_desktop=%s orig=[%s] "
            "astc_removidas=%d primeira_ext=[%s] texture2D_sub=%d | fp=[%s]",
            line_num,
            had_desktop_version ? "SIM" : "NAO",
            orig_version.empty() ? "-" : orig_version.c_str(),
            astc_removed,
            first_bad_ext.empty() ? "-" : first_bad_ext.c_str(),
            tex2d_replaced,
            shader_fingerprint(source).c_str());
    } else {
        write_log("OK",
            "Shader ja compativel GLES 3.0 (sem alteracoes) | linhas=%d | fp=[%s]",
            line_num, shader_fingerprint(source).c_str());
    }

    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Ponte JNI
// Classe Java: com.nexus.MobileGlues
// Método:      String sanitizeShaderNative(String shaderSource)
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
