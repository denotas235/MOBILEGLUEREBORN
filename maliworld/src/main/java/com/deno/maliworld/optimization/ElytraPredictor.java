package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Elytra flight chunk predictor.
 * When a player is flying with Elytra, this system:
 * 1. Tracks their velocity and heading
 * 2. Predicts position 3 seconds ahead
 * 3. Requests chunk loading for the predicted cone ahead
 *
 * Prevents the "white chunk" freeze during high-speed elytra flight.
 * Called from ElytraMixin each tick when the player is fall-flying.
 */
public final class ElytraPredictor {

    // Prediction parameters
    private static final int   PREDICT_SECONDS  = 3;
    private static final int   TICKS_PER_SECOND = 20;
    private static final float MIN_SPEED        = 15.0f; // blocks/sec, don't predict slow players
    private static final int   CONE_HALF_WIDTH  = 3;     // chunks on each side of travel axis

    // Per-player state: last known velocity
    private static final Map<UUID, Vec3> lastVelocity = new ConcurrentHashMap<>();
    private static volatile boolean      initialized   = false;

    private ElytraPredictor() {}

    public static void init() {
        initialized = true;
        MaliWorldMod.LOGGER.info("[MaliWorld] ElytraPredictor inicializado - voo sem chunks brancos.");
    }

    public static boolean isEnabled() { return initialized; }

    /**
     * Called each tick while a player is elytra-flying.
     * Computes the prediction cone and requests chunk loads.
     *
     * @param player  the flying player (server-side)
     * @param level   the server world
     */
    public static void tick(Player player, ServerLevel level) {
        if (!initialized) return;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;

        Vec3 vel = player.getDeltaMovement();
        double speed = vel.length() * TICKS_PER_SECOND; // blocks/sec

        if (speed < MIN_SPEED) return;

        // Smooth velocity with last known
        Vec3 last = lastVelocity.getOrDefault(player.getUUID(), vel);
        Vec3 smoothed = new Vec3(
            lerp(last.x, vel.x, 0.3),
            lerp(last.y, vel.y, 0.3),
            lerp(last.z, vel.z, 0.3)
        );
        lastVelocity.put(player.getUUID(), smoothed);

        // Predict position N seconds ahead
        double predX = player.getX() + smoothed.x * TICKS_PER_SECOND * PREDICT_SECONDS;
        double predZ = player.getZ() + smoothed.z * TICKS_PER_SECOND * PREDICT_SECONDS;

        // Request chunk loading for the prediction cone
        int predChunkX = (int)predX >> 4;
        int predChunkZ = (int)predZ >> 4;

        for (int dx = -CONE_HALF_WIDTH; dx <= CONE_HALF_WIDTH; dx++) {
            for (int dz = -CONE_HALF_WIDTH; dz <= CONE_HALF_WIDTH; dz++) {
                level.getChunk(predChunkX + dx, predChunkZ + dz); // load/queue chunk
            }
        }
    }

    /** Remove player from predictor state on dismount. */
    public static void onStopFlying(UUID playerId) {
        lastVelocity.remove(playerId);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}