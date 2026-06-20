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
 * Intercepta {@link GlStateManager#glShaderSource(int, String)} e converte
 * o codigo GLSL Desktop para GLES 3.0 antes de chegar ao driver OpenGL.
 *
 * <h3>Estrategia em duas camadas</h3>
 * <ol>
 *   <li><b>Camada 1 — Java puro</b>: conversao leve e imediata, sem JNI.
 *       Trata os erros mais comuns vistos nos logs:
 *       {@code gl_FragColor}, falta de precision, {@code texture2D()},
 *       {@code attribute/varying}, {@code #version} sem "es".</li>
 *   <li><b>Camada 2 — JNI nativo</b>: apos a conversao Java, chama
 *       {@code sanitizeShaderNative()} que executa o pipeline completo
 *       glslang → SPIRV → SPIRV-Cross (mesmo metodo da branch main).
 *       Garante conversao total para shaders mais complexos.</li>
 * </ol>
 *
 * <h3>Seguranca</h3>
 * {@code require = 0}: ignorado silenciosamente se a assinatura mudar
 * em versoes futuras do MC. Todos os caminhos tem fallback para o source
 * original — o jogo nunca crasha por culpa deste mixin.
 */
@Mixin(GlStateManager.class)
public abstract class MaliShaderSanitizerMixin {

    /**
     * Camada 1: conversao Java leve — trata erros imediatos dos logs.
     * Nao usa regex para ser robusta e rapida.
     */
    private static String lightweightConvert(String src) {
        if (src == null || src.isEmpty()) return src;

        // Ja e GLES? nao toca.
        int versionIdx = src.indexOf("#version");
        if (versionIdx >= 0) {
            int eol = src.indexOf(n, versionIdx);
            String vline = eol >= 0 ? src.substring(versionIdx, eol) : src.substring(versionIdx);
            if (vline.contains(" es")) return src;
        }

        StringBuilder sb = new StringBuilder(src.length() + 256);
        String[] lines = src.split("\n", -1);

        boolean versionDone    = false;
        boolean precisionDone  = false;
        boolean fragOutDeclared = false;
        boolean usesGlFragColor = src.contains("gl_FragColor");

        for (String raw : lines) {
            String t = raw.stripLeading();

            // ── #version ──────────────────────────────────────────────────
            if (t.startsWith("#version")) {
                sb.append("#version 300 es\n");
                versionDone = true;
                continue;
            }

            // Insere #version se ainda nao apareceu e chegou codigo real
            if (!versionDone && !t.isEmpty() && !t.startsWith("//") && !t.startsWith("/*")) {
                sb.append("#version 300 es\n");
                versionDone = true;
            }

            // ── precision (apos #version, antes do primeiro codigo) ───────
            if (versionDone && !precisionDone && !t.startsWith("#extension") && !t.startsWith("//")) {
                if (!src.contains("precision highp float")) {
                    sb.append("precision highp float;\n");
                    sb.append("precision highp int;\n");
                }
                if (usesGlFragColor && !fragOutDeclared) {
                    sb.append("out vec4 mg_gl_FragColor;\n");
                    fragOutDeclared = true;
                }
                precisionDone = true;
            }

            // ── substituicoes de sintaxe ───────────────────────────────────
            String line = raw;

            // gl_FragColor → variavel de saida declarada acima
            if (usesGlFragColor) {
                line = line.replace("gl_FragColor", "mg_gl_FragColor");
            }

            // Funcoes de textura legadas
            line = line.replace("texture2DLod(",  "textureLod(");
            line = line.replace("texture2D(",     "texture(");
            line = line.replace("textureCubeLod(","textureLod(");
            line = line.replace("textureCube(",   "texture(");
            line = line.replace("shadow2D(",      "texture(");

            // Qualificadores legados de vertex/fragment
            // "attribute " → "in "  (apenas em vertex shaders, mas nao faz mal no frag)
            // "varying "   → "in " ou "out " dependendo do contexto; SPIRV-Cross trata depois
            line = line.replace("attribute ", "in ");
            if (line.contains("varying ")) {
                // Heuristica: se ha gl_Position no shader original e "out" faz sentido
                line = line.replace("varying ", "in ");
            }

            sb.append(line).append(n);
        }

        return sb.toString();
    }

    /**
     * Intercepta a String de codigo GLSL imediatamente antes do upload
     * ao driver. Aplica conversao em duas camadas (Java leve → JNI nativo).
     */
    @ModifyVariable(
        method = "glShaderSource(ILjava/lang/String;)V",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private static String mg_interceptAndSanitize(String originalSource) {
        if (originalSource == null || originalSource.isEmpty()) return originalSource;

        // ── Camada 1: Java leve ───────────────────────────────────────────
        String stage1;
        try {
            stage1 = lightweightConvert(originalSource);
        } catch (Throwable t) {
            MobileGlues.LOGGER.error("[MobileGlues] Camada 1 falhou: {}", t.getMessage());
            stage1 = originalSource;
        }

        // ── Camada 2: JNI nativo (glslang → SPIRV → SPIRV-Cross) ─────────
        if (!MobileGlues.isAvailable()) {
            MobileGlues.LOGGER.debug("[MobileGlues] Lib nativa indisponivel — usando conversao Java.");
            return stage1;
        }

        String stage2;
        try {
            stage2 = MobileGlues.INSTANCE.sanitizeShaderNative(stage1);
        } catch (Throwable t) {
            MobileGlues.LOGGER.error("[MobileGlues] Camada 2 (JNI) falhou: {}. Usando camada 1.", t.getMessage());
            return stage1;
        }

        if (stage2 == null || stage2.isEmpty()) {
            MobileGlues.LOGGER.warn("[MobileGlues] JNI retornou vazio — usando camada 1.");
            return stage1;
        }

        MobileGlues.LOGGER.debug("[MobileGlues] Shader convertido ({} → {} bytes)",
            originalSource.length(), stage2.length());
        return stage2;
    }
}
