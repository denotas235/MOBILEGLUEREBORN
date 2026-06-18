// MobileGlues - gl/shader_sanitizer.cpp
// Phase 1: Mali GLES 3.0 Shader Sanitizer (à prova de falhas)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

#include "shader_sanitizer.h"

#include <jni.h>
#include <string>
#include <sstream>
#include <algorithm>

// ─────────────────────────────────────────────────────────────────────────────
// Lista de extensões que a Mali-G52 NÃO suporta em shaders.
// Se alguma destas aparecer antes do #version (bug de mods antigos),
// causa erro "extension not supported" na linha 1 ou 2, empurrando
// o #version para a linha errada e quebrando toda a pipeline.
// ─────────────────────────────────────────────────────────────────────────────
static const char* const UNSUPPORTED_EXTENSIONS[] = {
    "GL_EXT_texture_compression_astc",
    "GL_KHR_texture_compression_astc_ldr",
    "GL_KHR_texture_compression_astc_hdr",
    nullptr
};

static bool line_contains_unsupported_extension(const std::string& line) {
    for (int i = 0; UNSUPPORTED_EXTENSIONS[i] != nullptr; ++i) {
        if (line.find(UNSUPPORTED_EXTENSIONS[i]) != std::string::npos) {
            return true;
        }
    }
    return false;
}

// ─────────────────────────────────────────────────────────────────────────────
// sanitizeForMaliGLES
//
// Algoritmo linha-a-linha (sem regex) para garantir robustez total:
//   1. Remover whitespace/lixo no início de cada linha.
//   2. Descartar extensões problemáticas.
//   3. Capturar/descartar qualquer #version existente.
//   4. Substituir texture2D() → texture() (obsoleto no GLES 3.0).
//   5. Reconstruir o shader com o boilerplate GLES 3.0 na linha 1.
// ─────────────────────────────────────────────────────────────────────────────
std::string sanitizeForMaliGLES(const std::string& source) {
    std::istringstream stream(source);
    std::string line;
    std::string body;
    bool version_found = false;

    body.reserve(source.size());

    while (std::getline(stream, line)) {
        // 1. Remover whitespace inicial (espaços, tabs, \r)
        size_t first = line.find_first_not_of(" \t\r");
        if (first != std::string::npos) {
            line = line.substr(first);
        } else {
            // Linha completamente vazia — ignorar para não criar linhas em branco
            // antes do #version no output final.
            continue;
        }

        // 2. Descartar extensões não suportadas (causa erro 0:1 / 0:2 na Mali)
        if (line.find("#extension") != std::string::npos &&
            line_contains_unsupported_extension(line)) {
            continue;
        }

        // 3. Capturar e descartar a diretiva #version original do PC
        //    (Desktop GL usa #version 150 / 330 / 400 etc. sem "es")
        if (line.find("#version") != std::string::npos) {
            if (!version_found) {
                version_found = true;
            }
            continue; // Descarta — vamos injetar a versão GLES correta no início
        }

        // 4. Substituir texture2D() → texture() (descontinuado no GLES 3.0+)
        size_t pos = 0;
        while ((pos = line.find("texture2D(", pos)) != std::string::npos) {
            line.replace(pos, 10, "texture(");
            pos += 8;
        }

        // 5. Adicionar linha ao corpo
        if (!line.empty()) {
            body += line + "\n";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Montar o shader final com boilerplate GLES 3.0 garantidamente na linha 1.
    //
    // Regra da Mali: qualquer caractere antes do #version = crash imediato.
    // Aqui o #version é literalmente o primeiro byte do string de saída.
    // ─────────────────────────────────────────────────────────────────────────
    std::string result;
    result.reserve(body.size() + 128);

    result =
        "#version 300 es\n"
        "precision highp float;\n"
        "precision highp int;\n"
        "precision highp sampler2D;\n";

    result += body;

    return result;
}

// ─────────────────────────────────────────────────────────────────────────────
// Ponte JNI — permite ao lado Java (Mixin) invocar o sanitizador nativo
// sem overhead do GC do Java nem cópias desnecessárias.
//
// Classe Java esperada: com.nexus.MobileGlues
// Método:              String sanitizeShaderNative(String shaderSource)
// ─────────────────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jstring JNICALL
Java_com_nexus_MobileGlues_sanitizeShaderNative(JNIEnv* env, jobject /* obj */,
                                                jstring shaderSource) {
    if (!shaderSource) {
        return env->NewStringUTF("");
    }

    const char* nativeString = env->GetStringUTFChars(shaderSource, nullptr);
    if (!nativeString) {
        return env->NewStringUTF("");
    }

    std::string sanitized = sanitizeForMaliGLES(std::string(nativeString));
    env->ReleaseStringUTFChars(shaderSource, nativeString);

    return env->NewStringUTF(sanitized.c_str());
}
