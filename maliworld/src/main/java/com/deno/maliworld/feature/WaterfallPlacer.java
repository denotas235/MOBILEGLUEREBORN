package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Places waterfalls where rivers reach cliff edges.
 * MC 1.21.11: ChunkAccess.setBlockState takes int flags (0 = no propagation).
 */
public final class WaterfallPlacer {

    private static final double FALL_FREQ = 0.004;

    private WaterfallPlacer() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] WaterfallPlacer pronto para seed={}", seed);
    }

    /**
     * Determines if a waterfall exists at this column.
     */
    public static boolean isWaterfall(int x, int z, int surfaceY, int adjacentY) {
        int drop = surfaceY - adjacentY;
        if (drop < 8) return false;
        double n = SimplexNoise.noise(x * FALL_FREQ, z * FALL_FREQ);
        return n > 0.6;
    }

    /**
     * Place waterfall water column. MC 1.21.11: setBlockState int flags.
     *
     * @param topY    top of fall
     * @param bottomY base of fall (pool)
     */
    public static void placeWaterfallColumn(ChunkAccess chunk, int x, int z,
                                             int topY, int bottomY) {
        for (int y = bottomY + 1; y <= topY; y++) {
            BlockPos pos = new BlockPos(x & 15, y, z & 15);
            if (chunk.getBlockState(pos).isAir()) {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), 0);
            }
        }
        if (bottomY >= 1) {
            chunk.setBlockState(new BlockPos(x & 15, bottomY, z & 15),
                Blocks.WATER.defaultBlockState(), 0);
            chunk.setBlockState(new BlockPos(x & 15, bottomY - 1, z & 15),
                Blocks.GRAVEL.defaultBlockState(), 0);
        }
        BlockPos sidePos = new BlockPos(x & 15, topY - 2, z & 15);
        if (!chunk.getBlockState(sidePos).isAir()) {
            chunk.setBlockState(sidePos, Blocks.MOSS_BLOCK.defaultBlockState(), 0);
        }
    }
}