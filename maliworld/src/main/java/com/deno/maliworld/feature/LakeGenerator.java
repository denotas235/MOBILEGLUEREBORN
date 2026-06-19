package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Generates natural lakes in terrain depressions and valleys.
 * Lakes are identified by closed low-noise areas and carved organically.
 */
public final class LakeGenerator {

    private static final double LAKE_FREQ   = 0.003;
    private static final double LAKE_THRESH = 0.15;

    private LakeGenerator() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] LakeGenerator pronto para seed={}", seed);
    }

    /** Returns lake size category [0=none, 1-3] at position. */
    public static int getLakeCategory(int x, int z) {
        double n = SimplexNoise.noise(x * LAKE_FREQ, z * LAKE_FREQ) * 0.5 + 0.5;
        if (n > 1.0 - LAKE_THRESH * 0.5)  return 3;
        if (n > 1.0 - LAKE_THRESH)         return 2;
        if (n > 1.0 - LAKE_THRESH * 1.5)   return 1;
        return 0;
    }

    /** Carve a lake column. */
    public static void carveColumn(ChunkAccess chunk, int x, int z, int surfaceY, int category) {
        int lakeY = Math.min(surfaceY - 1, 63);
        int depth = 3 + category * 2;
        for (int dy = 0; dy < depth; dy++) {
            int y = lakeY - dy;
            if (y < 1) break;
            BlockPos pos = new BlockPos(x & 15, y, z & 15);
            if (dy < depth - 1) {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
            } else {
                BlockState bed = category >= 3 ? Blocks.GRAVEL.defaultBlockState()
                                              : Blocks.SAND.defaultBlockState();
                chunk.setBlockState(pos, bed, false);
            }
        }
    }
}