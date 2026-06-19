package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Enhances vanilla caves with:
 * - Pointed dripstone in chambers
 * - Moss patches on wet walls
 * - Deep lava pools
 *
 * MC 1.21.11: isSolidRender() was removed, use canOcclude() instead.
 */
public final class CaveEnhancer {

    private static final double CHAMBER_FREQ   = 0.008;
    private static final double CHAMBER_THRESH = 0.75;
    private static final double MOSS_FREQ      = 0.03;

    private CaveEnhancer() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] CaveEnhancer pronto para seed={}", seed);
    }

    public static boolean isChamber(int x, int y, int z) {
        double n = SimplexNoise.noise(x * CHAMBER_FREQ, y * CHAMBER_FREQ, z * CHAMBER_FREQ);
        return n > CHAMBER_THRESH;
    }

    public static boolean hasMoss(int x, int y, int z) {
        return SimplexNoise.noise(x * MOSS_FREQ, z * MOSS_FREQ) > 0.5 && y < 50;
    }

    /**
     * Enhance a cave position.
     * NOTE: uses canOcclude() instead of removed isSolidRender() (MC 1.21.11).
     */
    public static void enhanceCaveAt(ChunkAccess chunk, int x, int y, int z, int worldX, int worldZ) {
        BlockState current = chunk.getBlockState(new BlockPos(x, y, z));
        if (!current.isAir()) return;

        BlockState below = y > 0    ? chunk.getBlockState(new BlockPos(x, y - 1, z)) : null;
        BlockState above = y < 319  ? chunk.getBlockState(new BlockPos(x, y + 1, z)) : null;

        // Stalagmite: air with solid-opaque below and in a chamber
        if (below != null && !below.isAir() && below.canOcclude() &&
            isChamber(worldX, y, worldZ)) {
            if (y < 120 && SimplexNoise.noise(worldX * 0.1, worldZ * 0.1) > 0.6) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.POINTED_DRIPSTONE.defaultBlockState(), false);
                return;
            }
        }

        // Moss on cave-floor surface
        if (below != null && !below.isAir() && hasMoss(worldX, y, worldZ)) {
            chunk.setBlockState(new BlockPos(x, y - 1, z),
                Blocks.MOSS_BLOCK.defaultBlockState(), false);
        }

        // Deep lava pools (below Y=15)
        if (below != null && !below.isAir() && y < 15 && above != null && above.isAir()) {
            if (SimplexNoise.noise(worldX * 0.05, worldZ * 0.05) > 0.7) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.LAVA.defaultBlockState(), false);
            }
        }
    }
}