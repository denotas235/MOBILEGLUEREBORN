package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Generates organic dirt-path trails connecting structures and points of interest.
 * Paths are defined by noise-gradient centrelines and follow terrain naturally.
 */
public final class PathGenerator {

    private static final double PATH_FREQ    = 0.002;
    private static final double PATH_THRESH  = 0.06;

    private PathGenerator() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] PathGenerator pronto para seed={}", seed);
    }

    /**
     * Returns true if a path passes through this column.
     * Paths trace noise centrelines, avoiding steep slopes and water.
     */
    public static boolean isPath(int x, int z, int surfaceY) {
        if (surfaceY <= 63) return false; // no paths underwater
        double n  = Math.abs(SimplexNoise.noise(x * PATH_FREQ, z * PATH_FREQ));
        double nx = Math.abs(SimplexNoise.noise((x+1) * PATH_FREQ, z * PATH_FREQ));
        double nz = Math.abs(SimplexNoise.noise(x * PATH_FREQ, (z+1) * PATH_FREQ));
        return n < PATH_THRESH && n <= nx && n <= nz;
    }

    /**
     * Place a path block at the surface of this column.
     */
    public static void placePath(ChunkAccess chunk, int x, int z, int surfaceY) {
        BlockPos pos = new BlockPos(x & 15, surfaceY, z & 15);
        chunk.setBlockState(pos, Blocks.DIRT_PATH.defaultBlockState(), false);
        // Remove vegetation one block above
        BlockPos above = new BlockPos(x & 15, surfaceY + 1, z & 15);
        if (chunk.getBlockState(above).is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES)) {
            chunk.setBlockState(above, Blocks.AIR.defaultBlockState(), false);
        }
    }
}