// MobileGlues - com/nexus/mixins/MaliShaderSanitizerMixin.java
// Interceptor Fabric: converte shaders Desktop GL → GLES 3.0 antes do driver
// Alvo: Minecraft 1.21.11 · Fabric Loader 0.19+ · Mojang Mappings
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1
// SPDX-License-Identifier: LGPL-2.1-only

package com.nexus.mixins;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.nexus.MobileGlues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin que intercepta {@link GlStateManager#glShaderSource(int, String)} antes
 * de o Minecraft enviar o codigo GLSL raw ao driver OpenGL da Mali-G52.
 *
 * <h3>Mudanca de API em MC 1.21.11 (vs 1.21.x anterior)</h3>
 * <ul>
 *   <li>Pacote: {@code com.mojang.blaze3d.opengl.GlStateManager}
 *       (antes: {@code com.mojang.blaze3d.platform.GlStateManager})</li>
 *   <li>Metodo: {@code glShaderSource(int, String)} — sem underscore,
 *       recebe {@code String} directa (antes: {@code _glShaderSource(int, List<String>)})</li>
 *   <li>Descriptor interno: {@code (ILjava/lang/String;)V}</li>
 * </ul>
 * Verificado via Mojang mappings oficiais do MC 1.21.11
 * (client.txt linhas 132-149 em GlStateManager).
 *
 * <h3>require = 0 — Modo seguro</h3>
 * Se o metodo alvo nao for encontrado (mudanca futura de API), o Mixin
 * e ignorado silenciosamente em vez de crashar o jogo.
 *
 * <h3>Fallback</h3>
 * Se a sanitizacao JNI falhar, devolve o shader ORIGINAL.
 */
@Mixin(GlStateManager.class)
public abstract class MaliShaderSanitizerMixin {

    /**
     * Intercepta a {@code String} de codigo GLSL imediatamente
     * antes do upload ao driver OpenGL.
     *
     * <p>Assinatura do metodo alvo (Mojang Mappings 1.21.11):
     * <pre>
     *   GlStateManager.glShaderSource(int shaderId, String glslSource)
     *   Descriptor interno: (ILjava/lang/String;)V
     * </pre>
     *
     * @param originalSource Codigo GLSL original
     * @return Codigo GLSL sanitizado para GLES 3.0 (Mali-G52) ou original se fallback
     */
    @ModifyVariable(
        method = "glShaderSource(ILjava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private static String mg_interceptAndSanitize(String originalSource) {
        if (!MobileGlues.isAvailable() || originalSource == null || originalSource.isEmpty()) {
            return originalSource;
        }

        String sanitized;
        try {
            sanitized = MobileGlues.INSTANCE.sanitizeShaderNative(originalSource);
        } catch (Throwable t) {
            MobileGlues.LOGGER.error(
                "[MobileGlues] Falha CRITICA na sanitizacao nativa: {}. Usando shader original!",
                t.getMessage());
            return originalSource;
        }

        if (sanitized == null || sanitized.isEmpty()) {
            MobileGlues.LOGGER.warn("[MobileGlues] Sanitizacao retornou shader vazio. Usando original.");
            return originalSource;
        }

        MobileGlues.LOGGER.debug("[MobileGlues] Shader sanitizado ({} -> {} bytes)",
            originalSource.length(), sanitized.length());

        return sanitized;
    }
}
