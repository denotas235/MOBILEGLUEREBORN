package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Prediz a posicao futura do jogador em voo de elytra e
 * solicita pre-carregamento de chunks nessa direcao.
 *
 * Funciona apenas em single-player ou se o servidor tambem tiver o mod.
 */
public final class ElytraPredictor {

    private static final float PREDICT_SECONDS = 3.0f;
    private static final float ELYTRA_SPEED    = 20.0f;
    private static int tickCounter = 0;

    private ElytraPredictor() {}

    public static void tick(Player player) {
        // Executa a cada 10 ticks (0.5s) para reduzir overhead
        if (tickCounter++ % 10 != 0) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            float yaw   = player.getYRot();
            float pitch = player.getXRot();
            double px   = player.getX();
            double py   = player.getY();
            double pz   = player.getZ();

            double radYaw   = Math.toRadians(-yaw);
            double radPitch = Math.toRadians(pitch);
            double distance = ELYTRA_SPEED * PREDICT_SECONDS;

            double futureX = px + distance * Math.sin(radYaw) * Math.cos(radPitch);
            double futureZ = pz + distance * Math.cos(radYaw) * Math.cos(radPitch);

            // Solicita chunks na direcao do voo (5x5)
            int cx = (int) Math.floor(futureX / 16);
            int cz = (int) Math.floor(futureZ / 16);

            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (!mc.level.hasChunk(cx + dx, cz + dz)) {
                        mc.level.getChunkSource().getChunk(cx + dx, cz + dz, false);
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}