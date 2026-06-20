package com.deno.maliworld;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.ElytraPredictor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public final class MaliWorldClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MaliWorldMod.LOGGER.info("[MaliWorld] Cliente inicializado.");

        if (MaliWorldConfig.ELYTRA_PREDICTOR) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player == null || client.level == null) return;
                try {
                    if (client.player.isFallFlying()) {
                        ElytraPredictor.tick(client.player);
                    }
                } catch (Throwable t) { /* never crash */ }
            });
        }
    }
}