// MobileGlues - NativeASTCLoader.java
// JNI bridge para ASTC cache, shadow pass e sun direction
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.astcmod;

import java.io.File;

/**
 * JNI bridge ao loader nativo de ASTC e ao pipeline de sombras.
 * Todas as chamadas nativas sao seguras: se a lib nao carregar,
 * MobileGlues.isAvailable() retorna false e os mixins ignoram.
 */
public final class NativeASTCLoader {

    public static final String CACHE_DIR = "/sdcard/MG/cache";

    private NativeASTCLoader() {}

    // ── ASTC ──────────────────────────────────────────────────────────────────

    /**
     * Diz ao C++ para substituir o proximo glTexImage2D por ASTC comprimido.
     * Chame imediatamente antes do upload GL da textura.
     */
    public static native void setNextAstcCache(String path, int width, int height);

    /**
     * Upload direto ASTC para GL_TEXTURE_2D actualmente bound.
     * @return true se bem-sucedido
     */
    public static native boolean uploadASTC(String path, int width, int height);

    // ── SHADOW PASS ───────────────────────────────────────────────────────────

    /**
     * Actualiza a direccao do sol para o calculo do light MVP.
     * Chame a cada tick do cliente com o angulo solar actual do Minecraft.
     *
     * @param lx componente X da direccao (sol → mundo)
     * @param ly componente Y da direccao (negativo = apontando para baixo ao meio-dia)
     * @param lz componente Z da direccao
     */
    public static native void updateSunDirection(float lx, float ly, float lz);

    /**
     * Inicia o shadow depth pre-pass.
     * Liga o shadow FBO (1024×1024 depth-only) e desactiva escrita de cor.
     * Chame ANTES de renderizar a geometria do terreno.
     */
    public static native void beginShadowPass();

    /**
     * Termina o shadow depth pre-pass.
     * Restaura o FBO e viewport originais. Activa escrita de cor.
     * Chame APOS renderizar a geometria do terreno.
     */
    public static native void endShadowPass();

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /** Verifica se existe cache ASTC para o nome de textura dado. */
    public static boolean hasCachedAstc(String textureName) {
        return new File(buildCachePath(textureName)).exists();
    }

    /** Converte um ResourceLocation string para o path .astc no cache. */
    public static String buildCachePath(String textureName) {
        String cleaned = textureName
                .replace(":", "_")
                .replace("/", "_")
                .replace("\\", "_");
        return CACHE_DIR + "/" + cleaned + ".astc";
    }
}