package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Noise-based lake generation.
 * MC 1.21.11: ChunkAccess.setBlockState(BlockPos, BlockState, int) — flags param is int.
 */
public final class LakeGenerator {

    private static final double LAKE_FREQ   = 0.007;
    private static final double LAKE_THRESH = 0.72;

    private LakeGenerator() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] LakeGenerator pronto para seed={}", seed);
    }

    /**
     * Returns lake category at (x,z): 0=none, 1=shallow, 2=deep.
     */
    public static int getLakeCategory(int x, int z) {
        double n = SimplexNoise.noise(x * LAKE_FREQ, z * LAKE_FREQ);
        if (n > LAKE_THRESH + 0.06) return 2; // deep
        if (n > LAKE_THRESH)         return 1; // shallow
        return 0;
    }

    /**
     * Carve a lake column. MC 1.21.11: setBlockState flags is int (0).
     */
    public static void carveColumn(ChunkAccess chunk, int x, int z,
                                    int surfaceY, int category) {
        int depth = category == 2 ? 6 : 3;
        int lakeY = Math.min(surfaceY, 63);

        for (int dy = 0; dy < depth; dy++) {
            int y = lakeY - dy;
            if (y < 1) break;
            BlockPos pos = new BlockPos(x & 15, y, z & 15);
            if (dy < depth - 1) {
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState(), 0);
            } else {
                chunk.setBlockState(pos, Blocks.CLAY.defaultBlockState(), 0);
            }
        }
        // Mud/clay rim
        chunk.setBlockState(new BlockPos(x & 15, lakeY, z & 15),
            Blocks.MUD.defaultBlockState(), 0);
    }
}