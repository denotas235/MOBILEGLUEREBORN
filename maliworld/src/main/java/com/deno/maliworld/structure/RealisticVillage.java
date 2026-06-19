package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Adapts vanilla village generation to the terrain.
 * Houses are individually grounded, layout follows terrain contours,
 * materials match the biome.
 *
 * The actual injection happens in VillageStructureMixin.
 * This class provides the logic for:
 * - Finding the right Y for each building
 * - Selecting materials by climate
 * - Adding terrain-following foundations
 */
public final class RealisticVillage {

    private RealisticVillage() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] RealisticVillage pronto para seed={}", seed);
    }

    /**
     * Compute the ground Y at a village position, accounting for slope.
     * Returns the average Y in a 3x3 area around the building footprint.
     */
    public static int getGroundY(ServerLevel world, int x, int z, int radius) {
        int total = 0;
        int count = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos surface = world.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    new BlockPos(x+dx, 0, z+dz)
                );
                total += surface.getY();
                count++;
            }
        }
        return count > 0 ? total / count : 64;
    }

    /**
     * Build a stone foundation for a building footprint.
     * Fills from the lowest surface Y up to the building floor Y.
     *
     * @param world    server level
     * @param x1, z1   min corner of footprint
     * @param x2, z2   max corner of footprint
     * @param floorY   the Y the building floor should sit at
     */
    public static void buildFoundation(ServerLevel world, int x1, int z1, int x2, int z2,
                                        int floorY, BlockState foundationBlock) {
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                int surfaceY = world.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                    new BlockPos(x, 0, z)
                ).getY();
                // Fill from surface down to bedrock OR up to floor
                int minY = Math.min(surfaceY, floorY);
                int maxY = Math.max(surfaceY, floorY);
                for (int y = minY; y <= maxY; y++) {
                    world.setBlock(new BlockPos(x, y, z), foundationBlock, 3);
                }
            }
        }
    }

    /**
     * Select building material based on biome temperature/humidity.
     */
    public static BlockState getPrimaryMaterial(float temp, float humidity) {
        if (temp >= 2.0f) return Blocks.SANDSTONE.defaultBlockState();
        if (temp >= 1.0f && humidity < 0.3f) return Blocks.ACACIA_LOG.defaultBlockState();
        if (temp < 0.1f) return Blocks.SPRUCE_LOG.defaultBlockState();
        return Blocks.OAK_LOG.defaultBlockState();
    }

    /**
     * Clear floating blocks above a building location (air gap for proper placement).
     */
    public static void clearAbove(ServerLevel world, int x, int z, int fromY, int toY) {
        for (int y = fromY; y <= toY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.getBlockState(pos).isAir()) {
                world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }
}