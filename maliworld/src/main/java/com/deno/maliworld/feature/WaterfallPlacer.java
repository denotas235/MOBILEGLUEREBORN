package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Places waterfalls where rivers/streams reach cliff edges.
 * Detects steep Y-drops and fills the vertical space with water source blocks.
 */
public final class WaterfallPlacer {

    private static final double FALL_FREQ  = 0.004;

    private WaterfallPlacer() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] WaterfallPlacer pronto para seed={}", seed);
    }

    /**
     * Determines if a waterfall exists at this column.
     * A waterfall occurs when:
     * 1. This column has a significant drop (cliff)
     * 2. A river passes nearby uphill
     *
     * @param surfaceY  Y at this column
     * @param adjacentY Y of an adjacent higher column
     */
    public static boolean isWaterfallColumn(int x, int z, int surfaceY, int adjacentY) {
        if (adjacentY - surfaceY < 15) return false; // need significant cliff
        double n = SimplexNoise.noise(x * FALL_FREQ, z * FALL_FREQ);
        return n > 0.5; // only some cliff columns become waterfalls
    }

    /**
     * Fill a waterfall column from topY down to bottomY with water.
     *
     * @param chunk    chunk to modify
     * @param x        local X [0-15]
     * @param z        local Z [0-15]
     * @param topY     top of fall
     * @param bottomY  base of fall (pool)
     */
    public static void placeWaterfallColumn(ChunkAccess chunk, int x, int z,
                                             int topY, int bottomY) {
        // Water column
        for (int y = bottomY + 1; y <= topY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (chunk.getBlockState(pos).isAir()) {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
            }
        }
        // Pool at base: water + gravel floor
        if (bottomY >= 1) {
            chunk.setBlockState(new BlockPos(x, bottomY, z),
                Blocks.WATER.defaultBlockState(), false);
            chunk.setBlockState(new BlockPos(x, bottomY - 1, z),
                Blocks.GRAVEL.defaultBlockState(), false);
        }
        // Moss on surrounding blocks
        BlockPos sidePos = new BlockPos(x, topY - 2, z);
        if (!chunk.getBlockState(sidePos).isAir()) {
            chunk.setBlockState(sidePos, Blocks.MOSS_BLOCK.defaultBlockState(), false);
        }
    }
}