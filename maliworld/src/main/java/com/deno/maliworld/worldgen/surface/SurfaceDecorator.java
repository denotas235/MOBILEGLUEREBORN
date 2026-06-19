package com.deno.maliworld.worldgen.surface;

import com.deno.maliworld.worldgen.terrain.MountainBuilder;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Contextual surface block placement.
 * Chooses blocks based on altitude, slope, temperature, humidity.
 * MC 1.21.11: ChunkAccess.setBlockState(BlockPos, BlockState, int) — 0 = no-update flags.
 */
public final class SurfaceDecorator {

    private SurfaceDecorator() {}

    /**
     * Determine the appropriate surface block at this column.
     */
    public static BlockState getSurfaceBlock(ChunkAccess chunk, BlockPos pos,
                                              int y, double slope,
                                              float temp, float humidity) {
        // High slope → stone/cobblestone (cliff)
        if (slope > 0.75) {
            return Blocks.STONE.defaultBlockState();
        }
        if (slope > 0.5) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }

        // High altitude → snow / stone
        if (y >= 160) {
            return temp < 0.2f ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.STONE.defaultBlockState();
        }
        if (y >= 120) {
            return slope > 0.3 ? Blocks.GRAVEL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        }

        // Below sea level → sand/gravel
        if (y < 63) {
            return Blocks.GRAVEL.defaultBlockState();
        }

        // Dry/hot (desert-ish)
        if (temp > 0.75f && humidity < 0.3f) {
            return Blocks.SAND.defaultBlockState();
        }

        // Wet/cold (swamp/tundra)
        if (temp < 0.2f) {
            return humidity > 0.6f ? Blocks.PODZOL.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
        }

        // Normal → grass
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /**
     * Choose subsurface block (1-3 blocks below surface).
     */
    public static BlockState getSubsurfaceBlock(double slope, float temp) {
        if (slope > 0.5) return Blocks.STONE.defaultBlockState();
        if (temp > 0.75f) return Blocks.SANDSTONE.defaultBlockState();
        return Blocks.DIRT.defaultBlockState();
    }

    /**
     * Apply surface decoration at this column.
     * MC 1.21.11: setBlockState flags parameter is int (0 = suppress updates).
     */
    public static void decorate(ChunkAccess chunk, int worldX, int worldZ,
                                 int surfaceY, double slope, float temp, float humidity) {
        if (surfaceY <= 0) return;
        BlockPos pos = new BlockPos(worldX & 15, surfaceY, worldZ & 15);

        BlockState surface = getSurfaceBlock(chunk, pos, surfaceY, slope, temp, humidity);
        chunk.setBlockState(pos, surface, 0);

        BlockState sub = getSubsurfaceBlock(slope, temp);
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos below = new BlockPos(worldX & 15, surfaceY - dy, worldZ & 15);
            BlockState current = chunk.getBlockState(below);
            if (!current.isAir() && !current.is(Blocks.BEDROCK)) {
                chunk.setBlockState(below, sub, 0);
            }
        }
    }
}