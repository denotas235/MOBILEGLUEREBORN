package com.deno.maliworld.feature;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Cave enhancements: dripstone stalactites, moss patches, lava pools.
 * MC 1.21.11 fixes:
 *   - isSolidRender() removed → canOcclude()
 *   - ChunkAccess.setBlockState(BlockPos, BlockState, int) — int flags (0 = no updates)
 */
public final class CaveEnhancer {

    private static final double CHAMBER_FREQ   = 0.008;
    private static final double CHAMBER_THRESH = 0.75;
    private static final double MOSS_FREQ      = 0.03;

    private CaveEnhancer() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] CaveEnhancer pronto, seed={}", seed);
    }

    public static boolean isChamber(int x, int y, int z) {
        return SimplexNoise.noise(x * CHAMBER_FREQ, y * CHAMBER_FREQ, z * CHAMBER_FREQ) > CHAMBER_THRESH;
    }

    public static boolean hasMoss(int x, int y, int z) {
        return SimplexNoise.noise(x * MOSS_FREQ, z * MOSS_FREQ) > 0.5 && y < 50;
    }

    /**
     * Enhance a cave position.
     * isSolidRender() removed in 1.21.11 → canOcclude().
     * setBlockState flags: int 0 = suppress updates.
     */
    public static void enhanceCaveAt(ChunkAccess chunk, int x, int y, int z, int worldX, int worldZ) {
        BlockState current = chunk.getBlockState(new BlockPos(x, y, z));
        if (!current.isAir()) return;

        BlockState below = y > 0   ? chunk.getBlockState(new BlockPos(x, y - 1, z)) : null;
        BlockState above = y < 319 ? chunk.getBlockState(new BlockPos(x, y + 1, z)) : null;

        // Stalactite: air with canOcclude() floor below, inside a chamber
        if (below != null && !below.isAir() && below.canOcclude() &&
            isChamber(worldX, y, worldZ)) {
            if (y < 120 && SimplexNoise.noise(worldX * 0.1, worldZ * 0.1) > 0.6) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.POINTED_DRIPSTONE.defaultBlockState(), 0);
                return;
            }
        }

        // Moss on cave floor
        if (below != null && !below.isAir() && hasMoss(worldX, y, worldZ)) {
            chunk.setBlockState(new BlockPos(x, y - 1, z),
                Blocks.MOSS_BLOCK.defaultBlockState(), 0);
        }

        // Lava pools below Y=15
        if (below != null && !below.isAir() && y < 15 && above != null && above.isAir()) {
            if (SimplexNoise.noise(worldX * 0.05, worldZ * 0.05) > 0.7) {
                chunk.setBlockState(new BlockPos(x, y, z),
                    Blocks.LAVA.defaultBlockState(), 0);
            }
        }
    }
}