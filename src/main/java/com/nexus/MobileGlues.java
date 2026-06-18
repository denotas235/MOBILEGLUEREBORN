// MobileGlues - com/nexus/MobileGlues.java
// Ponte JNI para o Motor de Sanitização GLES 3.0
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

package com.nexus;

/**
 * Classe principal que expõe o motor nativo C++ de sanitização de shaders
 * ao lado Java via JNI.
 *
 * A biblioteca nativa "mobileglues" é carregada uma única vez no início
 * do processo. O método sanitizeShaderNative() é seguro para chamadas
 * concorrentes porque opera apenas sobre a string de entrada sem estado global.
 */
public class MobileGlues {

    /** Instância singleton usada pelo Mixin. */
    public static final MobileGlues instance = new MobileGlues();

    static {
        System.loadLibrary("mobileglues");
    }

    private MobileGlues() {}

    /**
     * Sanitiza o código GLSL de Desktop para GLES 3.0 compatível com a Mali.
     *
     * O motor nativo garante:
     *  - #version 300 es é o primeiro byte absoluto do output.
     *  - precision highp float/int/sampler2D injetados logo após.
     *  - GL_EXT_texture_compression_astc e extensões problemáticas removidas.
     *  - texture2D() substituído por texture().
     *
     * @param shaderSource Código GLSL original (Desktop GL / Java Edition)
     * @return Código GLSL sanitizado, pronto para o driver da Mali-G52
     */
    public native String sanitizeShaderNative(String shaderSource);
}
