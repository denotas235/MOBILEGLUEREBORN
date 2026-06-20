package com.deno.maliworld.feature;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Detecta penhasco com rio/agua acima e cria cascata.
 */
public final class WaterfallPlacer {

    private WaterfallPlacer() {}

    public static void place(LevelAccessor level, int chunkX, int chunkZ, int seaLevel) {
        try {
            int ox = chunkX * 16, oz = chunkZ * 16;
            for (int x = ox + 2; x < ox + 14; x += 4) {
                for (int z = oz + 2; z < oz + 14; z += 4) {
                    double v = SimplexNoise.noise(x * 0.05, z * 0.05);
                    if (v < 0.6) continue;
                    int topY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    if (topY < seaLevel + 20) continue;
                    BlockPos waterCheck = new BlockPos(x, topY, z);
                    if (!level.getBlockState(waterCheck).is(net.minecraft.world.level.block.Blocks.WATER)) {
                        waterCheck = new BlockPos(x, topY + 1, z);
                    }
                    // Penhasco: grande queda rapida
                    int belowY = level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z + 5);
                    if (topY - belowY >= 8) {
                        for (int fy = belowY; fy <= topY; fy++) {
                            level.setBlock(new BlockPos(x, fy, z), Blocks.WATER.defaultBlockState(), 2);
                        }
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}