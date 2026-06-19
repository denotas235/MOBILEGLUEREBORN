package com.deno.maliworld.optimization;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

/**
 * Pré-carrega chunks na trajetória da elytra.
 * Calcula posição estimada em 3s com base em velocidade + direção atuais.
 */
public final class ElytraPredictor {

    private static final int PREDICT_TICKS  = 60;
    private static final int PRELOAD_RADIUS = 2;

    private ElytraPredictor() {}

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        double vx = sp.getDeltaMovement().x;
        double vy = sp.getDeltaMovement().y;
        double vz = sp.getDeltaMovement().z;

        double px = sp.getX() + vx * PREDICT_TICKS;
        double pz = sp.getZ() + vz * PREDICT_TICKS;

        ChunkPos center = new ChunkPos((int) px >> 4, (int) pz >> 4);

        for (int dx = -PRELOAD_RADIUS; dx <= PRELOAD_RADIUS; dx++) {
            for (int dz = -PRELOAD_RADIUS; dz <= PRELOAD_RADIUS; dz++) {
                level.getChunkSource().addRegionTicket(
                    net.minecraft.server.level.TicketType.FORCED,
                    new ChunkPos(center.x + dx, center.z + dz),
                    0,
                    new ChunkPos(center.x + dx, center.z + dz)
                );
            }
        }
    }
}
