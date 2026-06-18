// MobileGlues - com/nexus/MobileGlues.java
// Ponte JNI para o Motor de Sanitização GLES 3.0 (lado Fabric / Minecraft JVM)
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

package com.nexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton que expõe o motor nativo de sanitização de shaders via JNI.
 *
 * Contexto de execução: Minecraft Java Edition rodando dentro do PojavLauncher
 * (ou launcher Android compatível). A biblioteca nativa "mobileglues" já está
 * carregada pelo processo Android antes do JVM do Minecraft iniciar. O
 * System.loadLibrary() aqui serve como declaração de intenção — se já estiver
 * carregada, o JVM retorna silenciosamente sem recarregar.
 *
 * Se a biblioteca não estiver disponível (execução fora do contexto Android),
 * {@link #isAvailable()} retorna false e o Mixin passa o shader original
 * sem modificação — zero crash garantido.
 */
public final class MobileGlues {

    public static final Logger LOGGER = LoggerFactory.getLogger("MobileGlues");

    /** Instância singleton usada pelo Mixin. */
    public static final MobileGlues INSTANCE = new MobileGlues();

    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("mobileglues");
            loaded = true;
            LOGGER.info("[MobileGlues] Biblioteca nativa carregada com sucesso.");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("[MobileGlues] Biblioteca nativa nao encontrada — sanitizacao desativada. ({})", e.getMessage());
        }
        NATIVE_AVAILABLE = loaded;
    }

    private MobileGlues() {}

    /**
     * @return true se a biblioteca nativa está disponível e as chamadas JNI são seguras.
     */
    public static boolean isAvailable() {
        return NATIVE_AVAILABLE;
    }

    /**
     * Sanitiza código GLSL Desktop para GLES 3.0 compatível com Mali-G52.
     *
     * Garante:
     *  - {@code #version 300 es} é o primeiro byte absoluto do output.
     *  - {@code precision highp float/int/sampler2D} injetados logo após.
     *  - {@code GL_EXT_texture_compression_astc} e variantes removidas.
     *  - {@code texture2D()} substituído por {@code texture()}.
     *  - Log diagnóstico em {@code /sdcard/MG/shader_sanitizer.log}.
     *
     * @param shaderSource Código GLSL original (Desktop GL)
     * @return Código GLSL sanitizado para GLES 3.0
     */
    public native String sanitizeShaderNative(String shaderSource);
}
