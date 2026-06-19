package com.deno.maliworld.optimization;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

/**
 * Pré-carrega chunks na trajetória da elytra.
 * Calcula posição estimada em 3s com base em velocidade + direção atuais.
 *
 * MC 1.21.11: addRegionTicket foi removido. Usa addTicketWithRadius(TicketType, ChunkPos, int).
 */
public final class ElytraPredictor {

    private static final int PREDICT_TICKS  = 60;
    private static final int PRELOAD_RADIUS = 2;

    private ElytraPredictor() {}

    public static void tick(Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        double vx = sp.getDeltaMovement().x;
        double vz = sp.getDeltaMovement().z;

        double px = sp.getX() + vx * PREDICT_TICKS;
        double pz = sp.getZ() + vz * PREDICT_TICKS;

        ChunkPos center = new ChunkPos((int) px >> 4, (int) pz >> 4);

        level.getChunkSource().addTicketWithRadius(
            TicketType.FORCED,
            center,
            PRELOAD_RADIUS
        );
    }
}
