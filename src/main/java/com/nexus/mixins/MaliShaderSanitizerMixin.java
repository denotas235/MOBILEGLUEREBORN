// MobileGlues - com/nexus/mixins/MaliShaderSanitizerMixin.java
// Interceptor Java: converte shaders Desktop GL → GLES 3.0 antes de chegarem ao driver
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only

package com.nexus.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.mojang.blaze3d.platform.GlStateManager;
import com.nexus.MobileGlues;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin que interceta o fluxo de upload de shaders do Minecraft Java Edition
 * na borda exata onde a LWJGL enviaria os dados brutos ao driver OpenGL.
 *
 * Problema resolvido:
 *   O Minecraft Java usa shaders Desktop GL (#version 150/330) com zero
 *   declarações de precisão. A GPU Mali-G52 exige:
 *     1. #version 300 es na linha 1 absoluta (sem qualquer caractere antes).
 *     2. precision highp float/int obrigatório em todos os shaders de fragmento.
 *     3. Extensões não suportadas (ex: GL_EXT_texture_compression_astc) causam
 *        erro "WARNING: 0:1" que empurra o #version para a linha 2 → crash.
 *
 * Solução:
 *   Interceptamos a List<String> de código fonte antes de ser alocada na
 *   memória OpenGL, consolidamo-la, passamos pelo sanitizador nativo (C++ JNI)
 *   e devolvemos o resultado numa nova lista com uma única string limpa.
 *
 * Nota de integração:
 *   Este Mixin requer Fabric Loader >= 0.14 com o MixinExtras de suporte.
 *   Adicionar esta classe ao ficheiro mixins.json do mod.
 */
@Mixin(GlStateManager.class)
public class MaliShaderSanitizerMixin {

    /**
     * Intercepta a lista de fragmentos de código GLSL antes do upload ao driver.
     *
     * O método glShaderSource da LWJGL aceita uma List<String> porque a spec
     * OpenGL permite que o shader seja dividido em múltiplos fragmentos de texto.
     * O Minecraft frequentemente usa esta API com 1-2 fragmentos.
     * Aqui consolidamos tudo numa única string, sanitizamos e devolvemos como
     * uma lista de um único elemento.
     *
     * @param originalSource Lista original de fragmentos de código GLSL
     * @return Lista com um único elemento: o shader sanitizado para GLES 3.0
     */
    @ModifyVariable(
        method = "glShaderSource(ILjava/util/List;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private static List<String> interceptAndSanitizeShader(List<String> originalSource) {
        if (originalSource == null || originalSource.isEmpty()) {
            return originalSource;
        }

        // 1. Consolidar todos os fragmentos de texto numa única string contínua
        StringBuilder combinedSource = new StringBuilder();
        for (String fragment : originalSource) {
            if (fragment != null) {
                combinedSource.append(fragment);
            }
        }

        // 2. Invocar o Motor de Sanitização Nativo (zero overhead sobre o GC do Java)
        //    O motor C++ garante que #version 300 es é o primeiro byte absoluto.
        String glesSource = MobileGlues.instance.sanitizeShaderNative(combinedSource.toString());

        // 3. Reempacotar o shader sanitizado numa lista de um único elemento
        List<String> sanitizedSource = new ArrayList<>(1);
        sanitizedSource.add(glesSource);

        return sanitizedSource;
    }
}
