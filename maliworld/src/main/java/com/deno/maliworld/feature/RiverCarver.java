package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Carves sinuous rivers through terrain.
 * MC 1.21.11: ChunkAccess.setBlockState(BlockPos, BlockState, int) — third param is int flags.
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
     * Returns the river half-width (0 = no river).
     */
    public static int getRiverWidth(int x, int z) {
        // Quantize to river grid
        int gridX = (int)Math.round((double)x / RIVER_SPACING) * RIVER_SPACING;
        int gridZ = (int)Math.round((double)z / RIVER_SPACING) * RIVER_SPACING;

        // River centreline noise
        double cx = gridX + SimplexNoise.noise(gridX * 0.001, gridZ * 0.001) * 200;
        double cz = gridZ + SimplexNoise.noise(gridX * 0.001 + 100, gridZ * 0.001 + 100) * 200;

        double dist = Math.sqrt((x - cx)*(x - cx) + (z - cz)*(z - cz));

        // Width by noise
        double widthNoise = SimplexNoise.noise(x * MEANDER_FREQ, z * MEANDER_FREQ);
        int maxWidth = (int)(3 + widthNoise * 4); // 3-7 blocks
        if (dist < maxWidth) return maxWidth - (int)dist;
        return 0;
    }

    /**
     * Carve a river column into the chunk.
     * MC 1.21.11: third arg to setBlockState is int flags (0 = no updates).
     */
    public static void carveColumn(ChunkAccess chunk, int x, int z, int surfaceY, int width) {
        int riverY = Math.min(surfaceY, 63);
        int depth  = 2 + width;

        for (int dy = 0; dy < depth; dy++) {
            int y = riverY - dy;
            if (y < 1) break;
            BlockPos pos = new BlockPos(x & 15, y, z & 15);
            if (dy == 0) {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), 0);
            } else if (dy == depth - 1) {
                chunk.setBlockState(pos, Blocks.GRAVEL.defaultBlockState(), 0);
            } else {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), 0);
            }
        }

        BlockPos bankPos = new BlockPos(x & 15, riverY, z & 15);
        if (width > 1) {
            chunk.setBlockState(bankPos, Blocks.SAND.defaultBlockState(), 0);
        }
    }
}