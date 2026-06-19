// MobileGlues — shader_sanitizer_test.cpp
// Suite de testes para sanitizeForMaliGLES()
// Compila para Linux x86_64 — sem contexto GL, sem Android SDK.
//
// Compile command (CI):
//   g++ -std=c++17 -I<mock-android> -I<jni-include> \
//       MobileGlues-cpp/gl/shader_sanitizer.cpp    \
//       MobileGlues-cpp/tests/shader_sanitizer_test.cpp \
//       -o shader_sanitizer_test
//
// Garante (por ordem de prioridade):
//   1. '#version 300 es' e o PRIMEIRO byte do output em todos os casos.
//   2. 'precision highp float;' e injectado imediatamente apos.
//   3. Extensoes ASTC nao suportadas pela Mali sao removidas.
//   4. 'texture2D(' e substituido por 'texture('.
//   5. '#version' de Desktop GL e descartado.
// SPDX-License-Identifier: LGPL-2.1-only

#include "../gl/shader_sanitizer.h"

#include <string>
#include <iostream>
#include <cstdlib>

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

static int g_passed = 0;
static int g_failed = 0;

static void check(const std::string& test_name, bool condition, const std::string& detail = "") {
    if (condition) {
        std::cout << "  [PASS] " << test_name << "\n";
        ++g_passed;
    } else {
        std::cerr << "  [FAIL] " << test_name << "\n";
        if (!detail.empty())
            std::cerr << "         " << detail << "\n";
        ++g_failed;
    }
}

static bool starts_with(const std::string& s, const std::string& prefix) {
    return s.size() >= prefix.size() &&
           s.compare(0, prefix.size(), prefix) == 0;
}

static bool contains(const std::string& s, const std::string& sub) {
    return s.find(sub) != std::string::npos;
}

// ─────────────────────────────────────────────────────────────────────────────
// Casos de teste
// ─────────────────────────────────────────────────────────────────────────────

// Caso 1 — Desktop GL 4.5 vertex shader
// A versao '#version 450' deve ser descartada e '#version 300 es' adicionada.
static void test_desktop_gl_450() {
    const std::string src =
        "#version 450\n"
        "in vec3 Position;\n"
        "in vec2 TexCoord;\n"
        "out vec2 vTexCoord;\n"
        "uniform mat4 ModelViewProjectionMatrix;\n"
        "void main() {\n"
        "    vTexCoord = TexCoord;\n"
        "    gl_Position = ModelViewProjectionMatrix * vec4(Position, 1.0);\n"
        "}\n";

    std::string out = sanitizeForMaliGLES(src);

    check("Desktop GL 4.5 — primeira linha e '#version 300 es'",
          starts_with(out, "#version 300 es\n"),
          "output: " + out.substr(0, 40));

    check("Desktop GL 4.5 — precision highp float injectado",
          contains(out, "precision highp float;"));

    check("Desktop GL 4.5 — '#version 450' removido do output",
          !contains(out, "#version 450"));

    check("Desktop GL 4.5 — corpo do shader preservado",
          contains(out, "ModelViewProjectionMatrix"));
}

// Caso 2 — Extensao ASTC ANTES do #version (o bug principal que este modulo resolve)
// O driver Mali-G52 crashava ao encontrar #extension antes de #version.
static void test_astc_before_version() {
    const std::string src =
        "#extension GL_EXT_texture_compression_astc : enable\n"
        "#version 330\n"
        "precision mediump float;\n"
        "void main() { gl_FragColor = vec4(1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);

    check("ASTC+version fora de ordem — primeira linha e '#version 300 es'",
          starts_with(out, "#version 300 es\n"),
          "output[0..60]: " + out.substr(0, 60));

    check("ASTC+version fora de ordem — extensao ASTC removida do output",
          !contains(out, "GL_EXT_texture_compression_astc"));

    check("ASTC+version fora de ordem — '#version 330' removido",
          !contains(out, "#version 330"));
}

// Caso 3 — texture2D() → texture()
// GLES 3.0 deprecou texture2D; o driver Mali nao aceita em #version 300 es.
static void test_texture2d_replacement() {
    const std::string src =
        "#version 330\n"
        "uniform sampler2D u_texture;\n"
        "in vec2 v_uv;\n"
        "out vec4 fragColor;\n"
        "void main() {\n"
        "    fragColor = texture2D(u_texture, v_uv);\n"
        "    vec4 extra = texture2D(u_texture, v_uv * 2.0);\n"
        "    fragColor += extra * 0.5;\n"
        "}\n";

    std::string out = sanitizeForMaliGLES(src);

    check("texture2D — substituido por texture() no output",
          !contains(out, "texture2D("),
          "output ainda tem 'texture2D(': " + out.substr(0, 200));

    check("texture2D — 'texture(' presente no output",
          contains(out, "texture("));

    check("texture2D — primera linha ainda e '#version 300 es'",
          starts_with(out, "#version 300 es\n"));
}

// Caso 4 — Shader ja compativel (sem transformacoes necessarias)
// Nao deve duplicar #version; primeira linha correcta.
static void test_already_gles30() {
    const std::string src =
        "#version 300 es\n"
        "precision mediump float;\n"
        "out vec4 fragColor;\n"
        "void main() { fragColor = vec4(0.0, 1.0, 0.0, 1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);

    check("Ja GLES 3.0 — primeira linha e '#version 300 es'",
          starts_with(out, "#version 300 es\n"),
          "output[0..50]: " + out.substr(0, 50));

    // Garante que nao fica '#version 300 es\n#version 300 es' duplicado
    size_t first_pos = out.find("#version 300 es");
    size_t second_pos = out.find("#version 300 es", first_pos + 1);
    check("Ja GLES 3.0 — '#version 300 es' nao duplicado",
          second_pos == std::string::npos);

    check("Ja GLES 3.0 — corpo do shader preservado",
          contains(out, "fragColor = vec4"));
}

// Caso 5 — Multiplas extensoes ASTC variantes (KHR LDR + KHR HDR)
// Ambas as variantes na lista UNSUPPORTED_EXTENSIONS devem ser removidas.
static void test_multiple_astc_variants() {
    const std::string src =
        "#extension GL_KHR_texture_compression_astc_ldr : require\n"
        "#extension GL_KHR_texture_compression_astc_hdr : enable\n"
        "#version 150\n"
        "void main() { gl_Position = vec4(0.0); }\n";

    std::string out = sanitizeForMaliGLES(src);

    check("ASTC multi-variante — primeira linha e '#version 300 es'",
          starts_with(out, "#version 300 es\n"));

    check("ASTC multi-variante — KHR LDR removido",
          !contains(out, "GL_KHR_texture_compression_astc_ldr"));

    check("ASTC multi-variante — KHR HDR removido",
          !contains(out, "GL_KHR_texture_compression_astc_hdr"));
}

// Caso 6 — Source vazia (edge case: nao deve crashar)
static void test_empty_source() {
    const std::string src = "";
    std::string out = sanitizeForMaliGLES(src);

    check("Source vazia — nao crasha e devolve boilerplate",
          starts_with(out, "#version 300 es\n"),
          "out: '" + out.substr(0, 40) + "'");
}

// Caso 7 — Shader com #version precedido de whitespace/BOM
// O algoritmo faz strip de whitespace leading; a linha #version
// deve ser detectada e descartada mesmo que tenha espacos a frente.
static void test_version_with_leading_whitespace() {
    const std::string src =
        "   #version 120\n"
        "void main() { gl_FragColor = vec4(1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);

    check("Whitespace antes do #version — '#version 120' descartado",
          !contains(out, "#version 120"));

    check("Whitespace antes do #version — primeira linha e '#version 300 es'",
          starts_with(out, "#version 300 es\n"));
}

// ─────────────────────────────────────────────────────────────────────────────
// main
// ─────────────────────────────────────────────────────────────────────────────
int main() {
    std::cout << "\n=== MobileGlues Shader Sanitizer Tests ===\n\n";

    std::cout << "--- Caso 1: Desktop GL 4.5 vertex shader ---\n";
    test_desktop_gl_450();

    std::cout << "\n--- Caso 2: ASTC extension antes do #version ---\n";
    test_astc_before_version();

    std::cout << "\n--- Caso 3: texture2D() -> texture() ---\n";
    test_texture2d_replacement();

    std::cout << "\n--- Caso 4: Shader ja GLES 3.0 (sem modificacoes) ---\n";
    test_already_gles30();

    std::cout << "\n--- Caso 5: Multiplas variantes ASTC (KHR LDR + HDR) ---\n";
    test_multiple_astc_variants();

    std::cout << "\n--- Caso 6: Source vazia (edge case) ---\n";
    test_empty_source();

    std::cout << "\n--- Caso 7: #version com whitespace a frente ---\n";
    test_version_with_leading_whitespace();

    std::cout << "\n==========================================\n";
    std::cout << "Resultados: " << g_passed << " passaram, "
              << g_failed << " falharam.\n";

    if (g_failed == 0) {
        std::cout << "OK — todos os testes passaram.\n\n";
        return 0;
    } else {
        std::cerr << "FALHA — " << g_failed << " teste(s) falharam.\n\n";
        return 1;
    }
}
