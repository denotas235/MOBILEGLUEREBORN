package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Organic dirt-path trails through terrain.
 * MC 1.21.11 fixes:
 *   - Blocks.GRASS removed → Blocks.SHORT_GRASS
 *   - ChunkAccess.setBlockState(pos, state, int) — int flags (0 = no updates)
 */
public final class PathGenerator {

    private static final double PATH_FREQ   = 0.002;
    private static final double PATH_THRESH = 0.06;

    private PathGenerator() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] PathGenerator pronto para seed={}", seed);
    }

    public static boolean isPath(int x, int z, int surfaceY) {
        if (surfaceY <= 63) return false;
        double n  = Math.abs(SimplexNoise.noise(x * PATH_FREQ, z * PATH_FREQ));
        double nx = Math.abs(SimplexNoise.noise((x + 1) * PATH_FREQ, z * PATH_FREQ));
        double nz = Math.abs(SimplexNoise.noise(x * PATH_FREQ, (z + 1) * PATH_FREQ));
        return n < PATH_THRESH && n <= nx && n <= nz;
    }

    /** Place path block and clear vegetation above. */
    public static void placePath(ChunkAccess chunk, int x, int z, int surfaceY) {
        int lx = x & 15;
        int lz = z & 15;
        chunk.setBlockState(new BlockPos(lx, surfaceY, lz),
            Blocks.DIRT_PATH.defaultBlockState(), 0);

        // Clear vegetation using explicit block checks.
        // MC 1.20.3+: Blocks.GRASS renamed to Blocks.SHORT_GRASS
        BlockPos above = new BlockPos(lx, surfaceY + 1, lz);
        BlockState aboveState = chunk.getBlockState(above);
        if (isVegetation(aboveState)) {
            chunk.setBlockState(above, Blocks.AIR.defaultBlockState(), 0);
        }
    }

    private static boolean isVegetation(BlockState state) {
        return state.isAir()
            || state.is(Blocks.SHORT_GRASS)
            || state.is(Blocks.TALL_GRASS)
            || state.is(Blocks.FERN)
            || state.is(Blocks.LARGE_FERN)
            || state.is(Blocks.DEAD_BUSH)
            || state.is(Blocks.POPPY)
            || state.is(Blocks.DANDELION)
            || state.is(Blocks.CORNFLOWER)
            || state.is(Blocks.AZURE_BLUET)
            || state.is(Blocks.OXEYE_DAISY)
            || state.is(Blocks.ALLIUM)
            || state.is(Blocks.BLUE_ORCHID);
    }
}