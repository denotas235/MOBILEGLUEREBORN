// MobileGlues - com/nexus/mixins/MaliShaderSanitizerMixin.java
// Interceptor Fabric: converte shaders Desktop GL → GLES 3.0 antes do driver
// Alvo: Minecraft 1.21.1 · Fabric Loader 0.16+ · Mojang Mappings
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

package com.nexus.mixins;

import com.mojang.blaze3d.platform.GlStateManager;
import com.nexus.MobileGlues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin que intercepta {@link GlStateManager#_glShaderSource(int, List)} antes
 * de o Minecraft enviar o código GLSL raw ao driver OpenGL da Mali-G52.
 *
 * <h3>Por que {@code _glShaderSource}?</h3>
 * Com Mojang Mappings, todos os métodos OpenGL de baixo nível em
 * {@code GlStateManager} têm prefixo underscore ({@code _}). É aqui que os
 * fragmentos de código shader passam para a LWJGL e, em seguida, para o driver.
 * Interceptar neste ponto garante que NENHUM shader chega ao driver da Mali
 * sem ter passado pela sanitização.
 *
 * <h3>O que acontece se a biblioteca nativa não estiver disponível?</h3>
 * O método retorna a lista original sem modificação — sem crash, sem exceção.
 *
 * <h3>Compatibilidade verificada</h3>
 * <ul>
 *   <li>Minecraft 1.21.1 · Mojang Mappings · Fabric Loader 0.16.9</li>
 *   <li>LWJGL 3.3.3+ (bundled with Minecraft 1.21+)</li>
 * </ul>
 */
@Mixin(GlStateManager.class)
public abstract class MaliShaderSanitizerMixin {

    /**
     * Intercepta a {@code List<String>} de fragmentos de código GLSL imediatamente
     * antes do upload ao driver OpenGL.
     *
     * <p>A spec OpenGL permite que o shader seja dividido em múltiplos fragmentos
     * de texto ({@code List<String>}). Aqui consolidamos tudo numa string única,
     * sanitizamos via JNI e devolvemos numa lista de um único elemento.
     *
     * <p>Assinatura do método alvo (Mojang Mappings 1.21.1):
     * <pre>
     *   GlStateManager._glShaderSource(int shaderId, List&lt;String&gt; strings)
     * </pre>
     *
     * @param originalSource Lista original de fragmentos de código GLSL
     * @return Lista com um único elemento: o shader sanitizado para GLES 3.0
     */
    @ModifyVariable(
        method = "_glShaderSource(ILjava/util/List;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static List<String> mg_interceptAndSanitize(List<String> originalSource) {
        // Fallback silencioso se a biblioteca nativa não estiver carregada
        if (!MobileGlues.isAvailable() || originalSource == null || originalSource.isEmpty()) {
            return originalSource;
        }

        // 1. Consolidar todos os fragmentos numa string contínua
        StringBuilder combined = new StringBuilder();
        for (String fragment : originalSource) {
            if (fragment != null) combined.append(fragment);
        }

        // 2. Sanitizar via motor nativo C++ (JNI — zero overhead sobre o GC)
        String sanitized;
        try {
            sanitized = MobileGlues.INSTANCE.sanitizeShaderNative(combined.toString());
        } catch (Throwable t) {
            // Nunca deixar um erro JNI derrubar o Minecraft
            MobileGlues.LOGGER.error("[MobileGlues] Falha na sanitizacao nativa: {}", t.getMessage());
            return originalSource;
        }

        // 3. Devolver numa lista de um único elemento (spec-compliant)
        List<String> result = new ArrayList<>(1);
        result.add(sanitized);
        return result;
    }
}
