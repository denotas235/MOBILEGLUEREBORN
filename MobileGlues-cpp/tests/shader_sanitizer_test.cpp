// MobileGlues — shader_sanitizer_test.cpp
// Suite de testes para sanitizeForMaliGLES() — modo pass-through
//
// Comportamento actual: a funcao devolve o source original sem modificacoes.
// Motivo: precision highp sampler2D causava erros de compilacao no ANGLE/Mali-G52.
// O motor usa o mecanismo de fallback existente no shader.cpp para lidar com
// shaders que nao compilam, sem necessitar de transformacoes em runtime.
// SPDX-License-Identifier: LGPL-2.1-only

#include "../gl/shader_sanitizer.h"

#include <string>
#include <iostream>
#include <cstdlib>

static int g_passed = 0;
static int g_failed = 0;

static void check(const std::string& name, bool ok, const std::string& detail = "") {
    if (ok) {
        std::cout << "  [PASS] " << name << "\n";
        ++g_passed;
    } else {
        std::cerr << "  [FAIL] " << name << "\n";
        if (!detail.empty()) std::cerr << "         " << detail << "\n";
        ++g_failed;
    }
}

// Caso 1 — Pass-through: Desktop GL 4.5 vertex shader nao e alterado
static void test_passthrough_desktop() {
    const std::string src =
        "#version 450\n"
        "in vec3 Position;\n"
        "uniform mat4 MVP;\n"
        "void main() { gl_Position = MVP * vec4(Position, 1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);

    check("Pass-through — output igual ao input",
          out == src,
          "input len=" + std::to_string(src.size()) +
          " output len=" + std::to_string(out.size()));
}

// Caso 2 — Pass-through: Shader ja em GLES 3.0 nao e alterado
static void test_passthrough_gles30() {
    const std::string src =
        "#version 300 es\n"
        "precision mediump float;\n"
        "out vec4 fragColor;\n"
        "void main() { fragColor = vec4(1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);
    check("Pass-through GLES 3.0 — output igual ao input", out == src);
}

// Caso 3 — Pass-through: Shader com extensao ASTC nao e alterado
// (A remocao seria incorrecta se o hardware suportasse a extensao)
static void test_passthrough_astc() {
    const std::string src =
        "#extension GL_EXT_texture_compression_astc : enable\n"
        "#version 330\n"
        "void main() { gl_FragColor = vec4(1.0); }\n";

    std::string out = sanitizeForMaliGLES(src);
    check("Pass-through ASTC — output igual ao input", out == src);
}

// Caso 4 — Pass-through: Shader com texture2D nao e alterado
static void test_passthrough_texture2d() {
    const std::string src =
        "#version 330\n"
        "uniform sampler2D u_tex;\n"
        "in vec2 v_uv;\n"
        "void main() { gl_FragColor = texture2D(u_tex, v_uv); }\n";

    std::string out = sanitizeForMaliGLES(src);
    check("Pass-through texture2D — nao substituido", out == src);
    check("Pass-through texture2D — 'texture2D(' preservado",
          out.find("texture2D(") != std::string::npos);
}

// Caso 5 — Robustez: source vazia nao causa crash
static void test_empty_no_crash() {
    const std::string src = "";
    std::string out;
    bool crashed = false;
    try {
        out = sanitizeForMaliGLES(src);
    } catch (...) {
        crashed = true;
    }
    check("Source vazia — sem crash", !crashed);
    check("Source vazia — output igual ao input (vazio)", out == src);
}

// Caso 6 — Robustez: shader longo (512 KB) nao causa crash
static void test_large_shader_no_crash() {
    std::string src;
    src.reserve(512 * 1024);
    src = "#version 300 es\nprecision highp float;\nvoid main() {\n";
    for (int i = 0; i < 10000; ++i)
        src += "  float v" + std::to_string(i) + " = float(" + std::to_string(i) + ");\n";
    src += "  gl_FragColor = vec4(0.0);\n}\n";

    std::string out;
    bool crashed = false;
    try {
        out = sanitizeForMaliGLES(src);
    } catch (...) {
        crashed = true;
    }
    check("Shader 512KB — sem crash", !crashed);
    check("Shader 512KB — output identico ao input", out == src);
}

// Caso 7 — Robustez: shader com caracteres especiais / Unicode nao causa crash
static void test_unicode_comment_no_crash() {
    const std::string src =
        "#version 300 es\n"
        "// Nomes portugueses: precisao, versao, textura\n"
        "// Caracteres: \xc3\xa9\xc3\xa3\xc3\xa7\n"
        "precision mediump float;\n"
        "void main() { gl_FragColor = vec4(0.0); }\n";

    std::string out;
    bool crashed = false;
    try {
        out = sanitizeForMaliGLES(src);
    } catch (...) {
        crashed = true;
    }
    check("Unicode em comentario — sem crash", !crashed);
    check("Unicode em comentario — output igual ao input", out == src);
}

int main() {
    std::cout << "\n=== MobileGlues Shader Sanitizer Tests (pass-through mode) ===\n\n";

    std::cout << "--- Caso 1: Desktop GL 4.5 ---\n";
    test_passthrough_desktop();

    std::cout << "\n--- Caso 2: GLES 3.0 ja correcto ---\n";
    test_passthrough_gles30();

    std::cout << "\n--- Caso 3: Extension ASTC ---\n";
    test_passthrough_astc();

    std::cout << "\n--- Caso 4: texture2D preservado ---\n";
    test_passthrough_texture2d();

    std::cout << "\n--- Caso 5: Source vazia ---\n";
    test_empty_no_crash();

    std::cout << "\n--- Caso 6: Shader 512 KB ---\n";
    test_large_shader_no_crash();

    std::cout << "\n--- Caso 7: Unicode em comentarios ---\n";
    test_unicode_comment_no_crash();

    std::cout << "\n============================================================\n";
    std::cout << "Resultados: " << g_passed << " passaram, " << g_failed << " falharam.\n";

    if (g_failed == 0) {
        std::cout << "OK — todos os testes passaram.\n\n";
        return 0;
    }
    std::cerr << "FALHA — " << g_failed << " teste(s) falharam.\n\n";
    return 1;
}
