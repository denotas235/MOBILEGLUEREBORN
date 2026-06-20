// MobileGlues - MobileGluesClient.java
// Fabric ClientModInitializer — sky state + sun direction tick update
// MC 1.21.11 / Mojang Mappings / Fabric API
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus;

import com.nexus.astcmod.NativeASTCLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Entry point do lado do cliente.
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>Verifica disponibilidade da lib nativa ao iniciar.</li>
 *   <li>Regista callback por tick que:
 *       <ul>
 *         <li>Actualiza o sky state (0.0–1.0 do tempo do dia) para godrays e scattering.</li>
 *         <li>Actualiza a direccao do sol ({@code lx, ly, lz}) para o light MVP do shadow pipeline.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Todas as operacoes estao em {@code try/catch} — este callback nunca crasha o jogo.
 */
public final class MobileGluesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MobileGlues.LOGGER.info(
            "[MobileGlues] Cliente inicializado — MC 1.21.11, native={}",
            MobileGlues.isAvailable());

        if (!MobileGlues.isAvailable()) {
            MobileGlues.LOGGER.warn(
                "[MobileGlues] Lib nativa indisponivel — features GPU desactivadas. " +
                "Verifique se libmobileglues.so esta presente no APK/lib dir.");
            return;
        }

        // Actualiza sky state e sun direction a cada tick do cliente.
        // timeOfDay: 0.0 = meia-noite, 0.25 = nascer, 0.5 = meio-dia, 0.75 = por
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) return;
            try {
                long dayTime   = client.level.getDayTime();
                float normalized = (dayTime % 24000L) / 24000.0f;

                // 1. Sky state para godrays e scattering atmosferico
                MobileGlues.updateSkyState(normalized);

                // 2. Direccao do sol para light MVP do shadow pipeline
                // O sol do Minecraft roda em torno do eixo X (Este-Oeste).
                // angle=0 = meia-noite (sol abaixo do horizonte)
                // angle=PI = meio-dia (sol directamente acima)
                float sunAngle = normalized * (float)(Math.PI * 2.0);
                float lx = 0.15f;                              // ligeiro desvio E-W
                float ly = -(float)Math.cos(sunAngle);        // -1 = acima, +1 = abaixo
                float lz = (float)Math.sin(sunAngle);         // rotacao N-S

                NativeASTCLoader.updateSunDirection(lx, ly, lz);
            } catch (Throwable t) {
                // Nao-critico — ignora silenciosamente. Nunca loga em hot path.
            }
        });

        MobileGlues.LOGGER.info("[MobileGlues] Sky-state + sun-direction tick registados.");
    }
}