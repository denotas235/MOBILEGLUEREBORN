// MobileGlues - MobileGluesClient.java
// Fabric ClientModInitializer — registers sky-state tick update
// MC 1.21.11 / Mojang Mappings / Fabric API
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client-side entry point.
 *
 * <p>Responsibilities:
 * <ol>
 *   <li>Log native library availability at startup.</li>
 *   <li>Register a per-tick callback that forwards the Minecraft level's
 *       day-time (0–24000) to the native sky-state engine as a 0.0–1.0
 *       float so that godrays, sun color, and atmospheric scattering
 *       track the in-game time of day.</li>
 * </ol>
 *
 * <p>All operations are wrapped in {@code try/catch} — this callback must
 * never crash the game under any circumstances.
 */
public final class MobileGluesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MobileGlues.LOGGER.info(
            "[MobileGlues] Client initialised — MC 1.21.11, native={}",
            MobileGlues.isAvailable());

        if (!MobileGlues.isAvailable()) {
            MobileGlues.LOGGER.warn(
                "[MobileGlues] Native lib unavailable — GPU features disabled. " +
                "Ensure libmobileglues.so is present in the APK/app lib dir.");
            return;
        }

        // Update atmospheric sky state every client tick.
        // timeOfDay: 0.0 = midnight, 0.25 = sunrise, 0.5 = noon, 0.75 = sunset
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null) return;
            try {
                long dayTime  = client.level.getDayTime();
                float normalized = (dayTime % 24000L) / 24000.0f;
                MobileGlues.updateSkyState(normalized);
            } catch (Throwable t) {
                // Non-critical — silently swallow. Never logs in hot path.
            }
        });

        MobileGlues.LOGGER.info("[MobileGlues] Sky-state tick registered.");
    }
}
