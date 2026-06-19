package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Generates organic dirt-path trails connecting structures and points of interest.
 * Paths trace noise centrelines and follow terrain naturally.
 * MC 1.21.11: replaced BlockTags.REPLACEABLE_BY_TREES with explicit air/plant checks.
 */
public final class PathGenerator {

    private static final double PATH_FREQ   = 0.002;
    private static final double PATH_THRESH = 0.06;

    private PathGenerator() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] PathGenerator pronto para seed={}", seed);
    }

    /** Returns true if a path passes through this column. */
    public static boolean isPath(int x, int z, int surfaceY) {
        if (surfaceY <= 63) return false;
        double n  = Math.abs(SimplexNoise.noise(x * PATH_FREQ, z * PATH_FREQ));
        double nx = Math.abs(SimplexNoise.noise((x + 1) * PATH_FREQ, z * PATH_FREQ));
        double nz = Math.abs(SimplexNoise.noise(x * PATH_FREQ, (z + 1) * PATH_FREQ));
        return n < PATH_THRESH && n <= nx && n <= nz;
    }

    /** Place a path block and clear vegetation above it. */
    public static void placePath(ChunkAccess chunk, int x, int z, int surfaceY) {
        BlockPos surfPos = new BlockPos(x & 15, surfaceY, z & 15);
        chunk.setBlockState(surfPos, Blocks.DIRT_PATH.defaultBlockState(), false);

        // Clear vegetation one block above (explicit block checks, no tag needed)
        BlockPos above = new BlockPos(x & 15, surfaceY + 1, z & 15);
        BlockState aboveState = chunk.getBlockState(above);
        if (aboveState.isAir()
            || aboveState.is(Blocks.GRASS)
            || aboveState.is(Blocks.TALL_GRASS)
            || aboveState.is(Blocks.FERN)
            || aboveState.is(Blocks.LARGE_FERN)
            || aboveState.is(Blocks.DEAD_BUSH)
            || aboveState.is(Blocks.POPPY)
            || aboveState.is(Blocks.DANDELION)
            || aboveState.is(Blocks.CORNFLOWER)
            || aboveState.is(Blocks.AZURE_BLUET)
            || aboveState.is(Blocks.OXEYE_DAISY)
            || aboveState.is(Blocks.ALLIUM)
            || aboveState.is(Blocks.BLUE_ORCHID)) {
            chunk.setBlockState(above, Blocks.AIR.defaultBlockState(), false);
        }
    }
}