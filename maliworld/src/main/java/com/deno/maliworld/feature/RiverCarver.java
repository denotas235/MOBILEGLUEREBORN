package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

/**
 * Carves sinuous rivers through terrain.
 * Rivers use a meander algorithm: each step deflects by a noise-driven angle.
 * Rivers flow from high altitude toward sea level, widening as they descend.
 */
public final class RiverCarver {

    private static long worldSeed;
    private static final double MEANDER_FREQ  = 0.006;
    private static final int    RIVER_SPACING = 800;

    private RiverCarver() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        worldSeed = seed;
        MaliWorldMod.LOGGER.debug("[MaliWorld] RiverCarver pronto para seed={}", seed);
    }

    /**
     * Determines if a river passes through position (x, z).
     * Uses gradient noise to find river centrelines.
     *
     * @return river width at this position (0 = no river, >0 = river of that width)
     */
    public static int getRiverWidth(int x, int z) {
        // Sample noise at this position and neighbors to find local minima (river centrelines)
        double n  = Math.abs(SimplexNoise.noise(x * MEANDER_FREQ, z * MEANDER_FREQ));
        double nx = Math.abs(SimplexNoise.noise((x+1) * MEANDER_FREQ, z * MEANDER_FREQ));
        double nz = Math.abs(SimplexNoise.noise(x * MEANDER_FREQ, (z+1) * MEANDER_FREQ));

        // River centre: value near 0, lower than neighbors
        if (n < 0.04 && n <= nx && n <= nz) {
            // Width scales with downstream distance (lower Y = wider)
            return 3 + (int)(n * 0); // base width 3
        }
        if (n < 0.07 && n <= nx && n <= nz) return 2;
        if (n < 0.10 && n <= nx && n <= nz) return 1;
        return 0;
    }

    /**
     * Carve a river column at (x, surfaceY, z).
     * Called from NoiseChunkGeneratorMixin after terrain is generated.
     *
     * @param chunk  chunk to modify
     * @param x      world X
     * @param z      world Z
     * @param surfaceY  surface Y at this column
     * @param width  river width in blocks (from getRiverWidth)
     */
    public static void carveColumn(net.minecraft.world.level.chunk.ChunkAccess chunk,
                                    int x, int z, int surfaceY, int width) {
        int riverY = Math.min(surfaceY, 63); // rivers at or below sea level
        int depth  = 2 + width;

        // Carve the river bed
        for (int dy = 0; dy < depth; dy++) {
            int y = riverY - dy;
            if (y < 1) break;
            BlockPos pos = new BlockPos(x & 15, y, z & 15);
            if (dy == 0) {
                // River surface: water
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
            } else if (dy == depth - 1) {
                // River bed: gravel
                chunk.setBlockState(pos, Blocks.GRAVEL.defaultBlockState(), false);
            } else {
                // River water
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), false);
            }
        }

        // Sand banks on edges
        BlockPos bankPos = new BlockPos(x & 15, riverY, z & 15);
        if (width > 1) {
            chunk.setBlockState(bankPos, Blocks.SAND.defaultBlockState(), false);
        }
    }
}