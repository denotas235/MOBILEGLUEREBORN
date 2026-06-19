package com.deno.maliworld.worldgen.surface;

import com.deno.maliworld.worldgen.terrain.MountainBuilder;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Contextual surface block selection.
 * Chooses surface blocks based on: altitude, slope, moisture, biome zone.
 */
public final class SurfaceDecorator {

    private SurfaceDecorator() {}

    /**
     * Determine the appropriate surface block at position given terrain context.
     *
     * @param chunk   chunk being decorated
     * @param pos     block position (surface level)
     * @param y       surface Y level
     * @param slope   slope [0-1] from TerrainShaper
     * @param temp    climate temperature
     * @param humidity climate humidity
     */
    public static BlockState getSurfaceBlock(ChunkAccess chunk, BlockPos pos,
                                              int y, double slope,
                                              float temp, float humidity) {
        // Snow: above snow line with cold temperature
        if (y >= MountainBuilder.SNOW_LINE_Y && temp < 0.0f) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }

        // Steep slope: exposed stone
        if (slope > 0.6) {
            return Blocks.STONE.defaultBlockState();
        }
        if (slope > 0.4) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }

        // Beach/coastal area (near sea level)
        if (y <= TerrainShaper.BEACH_Y + 2 && y >= TerrainShaper.SEA_LEVEL) {
            return Blocks.SAND.defaultBlockState();
        }

        // Desert: hot and dry
        if (temp >= 2.0f && humidity < 0.3f) {
            return Blocks.SAND.defaultBlockState();
        }

        // Savanna: warm and dry
        if (temp >= 1.0f && humidity < 0.3f) {
            return slope > 0.2 ? Blocks.DIRT.defaultBlockState()
                               : Blocks.GRASS_BLOCK.defaultBlockState();
        }

        // Jungle floor
        if (temp >= 1.5f && humidity > 0.6f) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }

        // Taiga/cold forest: podzol-like
        if (temp < 0.3f && humidity > 0.4f) {
            return Blocks.PODZOL.defaultBlockState();
        }

        // Frozen tundra
        if (temp < -0.5f) {
            return y > TerrainShaper.SEA_LEVEL + 2
                ? Blocks.SNOW_BLOCK.defaultBlockState()
                : Blocks.ICE.defaultBlockState();
        }

        // Mild slopes: coarse dirt
        if (slope > 0.25) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }

        // Default: grass
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    /**
     * Subsurface block (1-3 layers below surface).
     */
    public static BlockState getSubsurfaceBlock(double slope, float temp) {
        if (slope > 0.5)  return Blocks.STONE.defaultBlockState();
        if (temp < -0.5f) return Blocks.DIRT.defaultBlockState();
        return Blocks.DIRT.defaultBlockState();
    }

    /**
     * Apply contextual surface decoration to a chunk column.
     * Called from SurfaceBuilderMixin after vanilla surface is set.
     *
     * @param chunk   target chunk
     * @param worldX  world X coordinate of column
     * @param worldZ  world Z coordinate of column
     * @param surfaceY Y level of the surface block
     * @param slope    terrain slope [0-1]
     * @param temp     temperature
     * @param humidity humidity
     */
    public static void decorate(ChunkAccess chunk, int worldX, int worldZ,
                                 int surfaceY, double slope, float temp, float humidity) {
        if (surfaceY <= 0) return;
        BlockPos pos = new BlockPos(worldX, surfaceY, worldZ);

        // Set surface block
        BlockState surface = getSurfaceBlock(chunk, pos, surfaceY, slope, temp, humidity);
        chunk.setBlockState(pos, surface, false);

        // Set subsurface layers (1-3 below)
        BlockState sub = getSubsurfaceBlock(slope, temp);
        for (int dy = 1; dy <= 3; dy++) {
            BlockPos below = pos.below(dy);
            BlockState current = chunk.getBlockState(below);
            // Only replace stone/grass with contextual subsurface
            if (!current.isAir() && current != Blocks.BEDROCK.defaultBlockState()) {
                chunk.setBlockState(below, sub, false);
            }
        }
    }
}