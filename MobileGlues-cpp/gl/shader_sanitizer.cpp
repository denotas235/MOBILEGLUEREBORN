// MobileGlues - gl/shader_sanitizer.cpp
// Phase 1: Mali GLES 3.0 Shader Sanitizer (à prova de falhas) + Diagnostic Log
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_sanitizer.h"

#include <jni.h>
#include <string>
#include <sstream>
#include <algorithm>
#include <cstdio>
#include <ctime>
#include <sys/stat.h>
#include <android/log.h>

#define SANITIZER_TAG   "MG_ShaderSanitizer"
#define SANITIZER_LOG   "/sdcard/MG/shader_sanitizer.log"
#define MAX_LOG_BYTES   (4 * 1024 * 1024)   // Rota o ficheiro após 4 MB

// ─────────────────────────────────────────────────────────────────────────────
// Extensões que a Mali-G52 NÃO suporta dentro de shaders.
// Se alguma aparecer ANTES do #version (bug de código legado / mods antigos),
// o driver lança "extension not supported" na linha 0:1 ou 0:2,
// empurrando o #version para uma linha errada e quebrando toda a pipeline.
// ─────────────────────────────────────────────────────────────────────────────
static const char* const UNSUPPORTED_EXTENSIONS[] = {
    "GL_EXT_texture_compression_astc",
    "GL_KHR_texture_compression_astc_ldr",
    "GL_KHR_texture_compression_astc_hdr",
    nullptr
};

// ─────────────────────────────────────────────────────────────────────────────
// Sistema de Log Diagnóstico
// Escreve em /sdcard/MG/shader_sanitizer.log E no logcat Android.
// Roda o ficheiro quando excede MAX_LOG_BYTES para não encher o armazenamento.
// ─────────────────────────────────────────────────────────────────────────────
static void ensure_log_dir() {
    mkdir("/sdcard/MG", 0777);
}

static void rotate_log_if_needed() {
    struct stat st;
    if (stat(SANITIZER_LOG, &st) == 0 && st.st_size > MAX_LOG_BYTES) {
        // Renomeia o log atual para .bak (sobrescreve o anterior)
        rename(SANITIZER_LOG, SANITIZER_LOG ".bak");
    }
}

static void write_log(const char* level, const char* fmt, ...) {
    // 1. Logcat
    va_list args_logcat;
    va_start(args_logcat, fmt);
    __android_log_vprint(ANDROID_LOG_DEBUG, SANITIZER_TAG, fmt, args_logcat);
    va_end(args_logcat);

    // 2. Ficheiro em /sdcard/MG/shader_sanitizer.log
    ensure_log_dir();
    rotate_log_if_needed();

    FILE* f = fopen(SANITIZER_LOG, "a");
    if (!f) return;

    // Timestamp ISO 8601
    time_t now = time(nullptr);
    struct tm* tm_info = localtime(&now);
    char ts[32];
    strftime(ts, sizeof(ts), "%Y-%m-%dT%H:%M:%S", tm_info);

    fprintf(f, "[%s] [%s] ", ts, level);

    va_list args_file;
    va_start(args_file, fmt);
    vfprintf(f, fmt, args_file);
    va_end(args_file);

    fprintf(f, "\n");
    fclose(f);
}

// Abrevia o shader para identificação no log (primeiros 80 chars, sem newlines)
static std::string shader_fingerprint(const std::string& src) {
    std::string fp;
    fp.reserve(80);
    for (char c : src) {
        if (fp.size() >= 80) break;
        if (c == '\n' || c == '\r') fp += ' ';
        else fp += c;
    }
    return fp;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers internos
// ─────────────────────────────────────────────────────────────────────────────
static bool line_contains_unsupported_extension(const std::string& line) {
    for (int i = 0; UNSUPPORTED_EXTENSIONS[i] != nullptr; ++i) {
        if (line.find(UNSUPPORTED_EXTENSIONS[i]) != std::string::npos) {
            return true;
        }
    }
    return false;
}

static const char* find_unsupported_extension_name(const std::string& line) {
    for (int i = 0; UNSUPPORTED_EXTENSIONS[i] != nullptr; ++i) {
        if (line.find(UNSUPPORTED_EXTENSIONS[i]) != std::string::npos) {
            return UNSUPPORTED_EXTENSIONS[i];
        }
    }
    return nullptr;
}

// ─────────────────────────────────────────────────────────────────────────────
// sanitizeForMaliGLES
//
// Algoritmo linha-a-linha (SEM std::regex) para robustez total.
// Registos de diagnóstico em /sdcard/MG/shader_sanitizer.log.
//
// Garante:
//   1. #version 300 es é o PRIMEIRO BYTE absoluto do output.
//   2. precision highp float/int/sampler2D injetados logo após.
//   3. Extensões ASTC incompatíveis removidas (com log da extensão exata).
//   4. texture2D() → texture() (com contagem de substituições no log).
// ─────────────────────────────────────────────────────────────────────────────
std::string sanitizeForMaliGLES(const std::string& source) {
    std::istringstream stream(source);
    std::string line;
    std::string body;

    // Contadores para o relatório de diagnóstico
    bool     had_desktop_version  = false;
    bool     had_astc_extension   = false;
    int      astc_extensions_removed = 0;
    int      texture2d_replaced   = 0;
    int      line_number          = 0;
    std::string original_version_line;
    std::string first_bad_extension;

    body.reserve(source.size());

    while (std::getline(stream, line)) {
        line_number++;

        // 1. Remover whitespace inicial (espaços, tabs, \r)
        size_t first = line.find_first_not_of(" \t\r");
        if (first != std::string::npos) {
            line = line.substr(first);
        } else {
            // Linha completamente vazia — ignorar para não criar linhas em
            // branco antes do #version no output final.
            continue;
        }

        // 2. Descartar extensões não suportadas na Mali (causa erro 0:1/0:2)
        if (line.find("#extension") != std::string::npos &&
            line_contains_unsupported_extension(line)) {
            const char* ext_name = find_unsupported_extension_name(line);
            if (!had_astc_extension) {
                had_astc_extension = true;
                first_bad_extension = ext_name ? ext_name : "(desconhecida)";
            }
            astc_extensions_removed++;
            write_log("WARN",
                "Linha %d: extensao removida [%s] — causaria erro 0:%d no driver Mali",
                line_number, ext_name ? ext_name : "?", line_number);
            continue;
        }

        // 3. Capturar e descartar a diretiva #version original do Desktop GL
        if (line.find("#version") != std::string::npos) {
            if (!had_desktop_version) {
                had_desktop_version = true;
                original_version_line = line;
                write_log("INFO",
                    "Linha %d: versao Desktop GL detetada [%s] — sera substituida por '#version 300 es'",
                    line_number, line.c_str());
            }
            continue; // Descarta — injetamos a versão GLES correta no início
        }

        // 4. Substituir texture2D() → texture() (obsoleto no GLES 3.0+)
        size_t pos = 0;
        while ((pos = line.find("texture2D(", pos)) != std::string::npos) {
            line.replace(pos, 10, "texture(");
            pos += 8;
            texture2d_replaced++;
        }

        // 5. Acumular linha no corpo
        if (!line.empty()) {
            body += line + "\n";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Montar o shader final com boilerplate GLES 3.0 garantidamente na linha 1.
    //
    // Regra da Mali: qualquer caractere antes do #version = crash imediato.
    // Aqui o #version é literalmente o primeiro byte da string de saída.
    // ─────────────────────────────────────────────────────────────────────────
    std::string result;
    result.reserve(body.size() + 128);
    result =
        "#version 300 es\n"
        "precision highp float;\n"
        "precision highp int;\n"
        "precision highp sampler2D;\n";
    result += body;

    // ─────────────────────────────────────────────────────────────────────────
    // Relatório final de diagnóstico
    // ─────────────────────────────────────────────────────────────────────────
    bool any_issue = had_desktop_version || had_astc_extension || (texture2d_replaced > 0);

    if (any_issue) {
        write_log("REPORT",
            "=== Sanitizacao concluida | linhas_processadas=%d "
            "versao_desktop=%s versao_original=[%s] "
            "extensoes_astc_removidas=%d primeira_extensao_problematica=[%s] "
            "texture2D_substituidos=%d | fingerprint=[%s]",
            line_number,
            had_desktop_version  ? "SIM" : "NAO",
            original_version_line.empty() ? "(nenhuma)" : original_version_line.c_str(),
            astc_extensions_removed,
            first_bad_extension.empty() ? "(nenhuma)" : first_bad_extension.c_str(),
            texture2d_replaced,
            shader_fingerprint(source).c_str());
    } else {
        write_log("OK",
            "Shader ja compativel com GLES 3.0 (sem alteracoes necessarias) | linhas=%d | fingerprint=[%s]",
            line_number,
            shader_fingerprint(source).c_str());
    }

    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Ponte JNI — permite ao lado Java (Mixin) invocar o sanitizador nativo
// sem overhead do GC do Java nem cópias desnecessárias.
//
// Classe Java: com.nexus.MobileGlues
// Método:      String sanitizeShaderNative(String shaderSource)
// ─────────────────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_MobileGlues_sanitizeShaderNative(JNIEnv* env, jobject /* obj */,
                                                jstring shaderSource) {
    if (!shaderSource) {
        write_log("ERROR", "sanitizeShaderNative chamado com source nulo");
        return env->NewStringUTF("");
    }

    const char* nativeString = env->GetStringUTFChars(shaderSource, nullptr);
    if (!nativeString) {
        write_log("ERROR", "GetStringUTFChars falhou — out of memory?");
        return env->NewStringUTF("");
    }

    std::string sanitized = sanitizeForMaliGLES(std::string(nativeString));
    env->ReleaseStringUTFChars(shaderSource, nativeString);

    return env->NewStringUTF(sanitized.c_str());
}
