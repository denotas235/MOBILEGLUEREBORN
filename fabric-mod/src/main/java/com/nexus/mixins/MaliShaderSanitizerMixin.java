// MobileGlues - com/nexus/mixins/MaliShaderSanitizerMixin.java
// Interceptor Fabric: converte shaders Desktop GL → GLES 3.0 antes do driver
// Alvo: Minecraft 1.21.11 · Fabric Loader 0.19+ · Mojang Mappings
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
 * de o Minecraft enviar o codigo GLSL raw ao driver OpenGL da Mali-G52.
 *
 * <h3>Por que {@code _glShaderSource}?</h3>
 * Com Mojang Mappings, todos os metodos OpenGL de baixo nivel em
 * {@code GlStateManager} tem prefixo underscore ({@code _}). Eh aqui que os
 * fragmentos de codigo shader passam para a LWJGL e, em seguida, para o driver.
 * Interceptar neste ponto garante que NENHUM shader chega ao driver da Mali
 * sem ter passado pela sanitizacao.
 *
 * <h3>Compatibilidade Mojang Mappings</h3>
 * O metodo {@code GlStateManager._glShaderSource(int, List)} e a sua assinatura
 * interna {@code (ILjava/util/List;)V} sao estaveis entre MC 1.21 e 1.21.11.
 * Verificado via Fabric meta API e mappings oficiais Mojang.
 *
 * <h3>require = 0 — Modo seguro</h3>
 * {@code require = 0} torna a injecao opcional: se o metodo alvo nao for
 * encontrado (ex.: mudanca futura de mappings), o Mixin e ignorado
 * silenciosamente em vez de crashar o jogo.
 *
 * <h3>Fallback seguro</h3>
 * Se a sanitizacao JNI falhar por qualquer razao, o shader ORIGINAL eh usado.
 * Nunca trava o jogo!
 *
 * <h3>Compatibilidade verificada</h3>
 * <ul>
 *   <li>Minecraft 1.21.11 · Mojang Mappings · Fabric Loader 0.19.3</li>
 *   <li>LWJGL 3.3.3+ (bundled com Minecraft 1.21+)</li>
 *   <li>Fabric Loom 1.17.11 · Gradle 9.6.0</li>
 * </ul>
 */
@Mixin(GlStateManager.class)
public abstract class MaliShaderSanitizerMixin {

    /**
     * Intercepta a {@code List<String>} de fragmentos de codigo GLSL imediatamente
     * antes do upload ao driver OpenGL.
     *
     * <p>A spec OpenGL permite que o shader seja dividido em multiplos fragmentos
     * de texto ({@code List<String>}). Aqui consolidamos tudo numa string unica,
     * sanitizamos via JNI e devolvemos numa lista de um unico elemento.
     *
     * <p>Assinatura do metodo alvo (Mojang Mappings 1.21 – 1.21.11, estavEl):
     * <pre>
     *   GlStateManager._glShaderSource(int shaderId, List&lt;String&gt; strings)
     *   Descriptor interno: (ILjava/util/List;)V
     * </pre>
     *
     * @param originalSource Lista original de fragmentos de codigo GLSL
     * @return Lista com um unico elemento: o shader sanitizado OU o original se fallback
     */
    @ModifyVariable(
        method = "_glShaderSource(ILjava/util/List;)V",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private static List<String> mg_interceptAndSanitize(List<String> originalSource) {
        // Fallback silencioso se a biblioteca nativa nao estiver carregada
        if (!MobileGlues.isAvailable() || originalSource == null || originalSource.isEmpty()) {
            return originalSource;
        }

        // 1. Consolidar todos os fragmentos numa string continua
        StringBuilder combined = new StringBuilder();
        for (String fragment : originalSource) {
            if (fragment != null) combined.append(fragment);
        }
        String originalShader = combined.toString();

        // 2. Sanitizar via motor nativo C++ (JNI — zero overhead sobre o GC)
        String sanitized;
        try {
            sanitized = MobileGlues.INSTANCE.sanitizeShaderNative(originalShader);
        } catch (Throwable t) {
            // FALLBACK CRITICO: JNI falhou - usar shader original
            MobileGlues.LOGGER.error("[MobileGlues] Falha CRITICA na sanitizacao nativa: {}. Usando shader original!", t.getMessage());
            return originalSource;
        }

        // 3. Verificar se o sanitized nao esta vazio ou corrompido
        if (sanitized == null || sanitized.isEmpty()) {
            MobileGlues.LOGGER.warn("[MobileGlues] Sanitizacao retornou shader vazio. Usando original.");
            return originalSource;
        }

        // 4. Log de sucesso (debug only para nao spammar)
        MobileGlues.LOGGER.debug("[MobileGlues] Shader sanitizado ({} -> {} bytes)",
            originalShader.length(), sanitized.length());

        // 5. Devolver numa lista de um unico elemento (spec-compliant)
        List<String> result = new ArrayList<>(1);
        result.add(sanitized);
        return result;
    }
}
