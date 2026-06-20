package com.deno.maliworld.feature;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Cria caminhos organicos de terra batida.
 * Conecta pontos de interesse seguindo o terreno.
 */
public final class PathGenerator {

    private PathGenerator() {}

    public static void generatePath(LevelAccessor level, int chunkX, int chunkZ) {
        try {
            int ox = chunkX * 16, oz = chunkZ * 16;
            for (int x = ox; x < ox + 16; x++) {
                for (int z = oz; z < oz + 16; z++) {
                    double v = SimplexNoise.noise(x * 0.02, z * 0.02);
                    double dv = Math.abs(v);
                    if (dv < 0.03) {
                        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                        if (y > 62) {
                            BlockPos pos = new BlockPos(x, y - 1, z);
                            level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), 2);
                        }
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}