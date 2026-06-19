package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Enhances vanilla caves with additional features:
 * - Stalactites and stalagmites in chambers
 * - Moss patches on wet walls
 * - Ore concentration in veins
 * - Occasional lava in deep sections
 *
 * Applied via ChunkAccess post-generation (CaveEnhancerMixin hooks into carver output).
 */
public final class CaveEnhancer {

    private static final double CHAMBER_FREQ  = 0.008;
    private static final double CHAMBER_THRESH = 0.75;
    private static final double MOSS_FREQ     = 0.03;

    private CaveEnhancer() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] CaveEnhancer pronto para seed={}", seed);
    }

    /**
     * True if this position is inside a "cave chamber" (large open space).
     * Used to decide if stalactite/stalagmite placement is appropriate.
     */
    public static boolean isChamber(int x, int y, int z) {
        double n = SimplexNoise.noise(x * CHAMBER_FREQ, y * CHAMBER_FREQ, z * CHAMBER_FREQ);
        return n > CHAMBER_THRESH;
    }

    /**
     * True if a moss patch should appear at this cave wall.
     */
    public static boolean hasMoss(int x, int y, int z) {
        double n = SimplexNoise.noise(x * MOSS_FREQ, z * MOSS_FREQ);
        return n > 0.5 && y < 50;
    }

    /**
     * Enhance a cave column at position.
     * Called by the mixin when an air pocket is found underground.
     *
     * @param chunk  chunk to modify
     * @param x      local chunk X [0-15]
     * @param y      Y level
     * @param z      local chunk Z [0-15]
     */
    public static void enhanceCaveAt(ChunkAccess chunk, int x, int y, int z, int worldX, int worldZ) {
        BlockState current = chunk.getBlockState(new BlockPos(x, y, z));
        if (!current.isAir()) return;

        BlockState below = y > 0 ? chunk.getBlockState(new BlockPos(x, y-1, z)) : null;
        BlockState above = y < 319 ? chunk.getBlockState(new BlockPos(x, y+1, z)) : null;

        // Stalagmite: air with solid below and chamber above
        if (below != null && !below.isAir() && below.isSolidRender() &&
            isChamber(worldX, y, worldZ)) {
            if (y < 120 && SimplexNoise.noise(worldX * 0.1, worldZ * 0.1) > 0.6) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.POINTED_DRIPSTONE.defaultBlockState(), false);
                return;
            }
        }

        // Moss on cave walls
        if (below != null && !below.isAir() && hasMoss(worldX, y, worldZ)) {
            chunk.setBlockState(new BlockPos(x, y-1, z),
                Blocks.MOSS_BLOCK.defaultBlockState(), false);
        }

        // Deep lava pools
        if (below != null && !below.isAir() && y < 15 && above != null && above.isAir()) {
            if (SimplexNoise.noise(worldX * 0.05, worldZ * 0.05) > 0.7) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.LAVA.defaultBlockState(), false);
            }
        }
    }
}