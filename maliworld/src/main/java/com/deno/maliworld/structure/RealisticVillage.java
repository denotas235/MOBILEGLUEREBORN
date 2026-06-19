package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Terrain-adapting village generation helper.
 * MC 1.21.11: ServerLevel.setBlock(BlockPos, BlockState, int) flags is int.
 */
public final class RealisticVillage {

    private RealisticVillage() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] RealisticVillage pronto, seed={}", seed);
    }

    /**
     * Flatten terrain in a radius around village center.
     * Called after a village is placed to make it look natural.
     */
    public static void adaptTerrain(ServerLevel world, int cx, int cz, int radius) {
        try {
            // Compute average surface height
            int sumY = 0, count = 0;
            for (int dx = -radius; dx <= radius; dx += 4) {
                for (int dz = -radius; dz <= radius; dz += 4) {
                    BlockPos top = world.getHeightmapPos(
                        Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(cx+dx, 0, cz+dz));
                    sumY += top.getY(); count++;
                }
            }
            if (count == 0) return;
            int targetY = sumY / count;

            // Smooth terrain toward average
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.sqrt(dx*dx+dz*dz) > radius) continue;
                    BlockPos top = world.getHeightmapPos(
                        Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(cx+dx, 0, cz+dz));
                    int curY = top.getY();
                    if (curY > targetY) {
                        for (int y = targetY+1; y <= curY; y++) {
                            world.setBlock(new BlockPos(cx+dx, y, cz+dz),
                                Blocks.AIR.defaultBlockState(), 3);
                        }
                        world.setBlock(new BlockPos(cx+dx, targetY, cz+dz),
                            Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    } else if (curY < targetY) {
                        for (int y = curY+1; y <= targetY; y++) {
                            world.setBlock(new BlockPos(cx+dx, y, cz+dz),
                                Blocks.DIRT.defaultBlockState(), 3);
                        }
                        world.setBlock(new BlockPos(cx+dx, targetY, cz+dz),
                            Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                    }
                }
            }
        } catch (Exception e) {
            MaliWorldMod.LOGGER.debug("[MaliWorld] RealisticVillage adaptTerrain erro: {}", e.getMessage());
        }
    }
}