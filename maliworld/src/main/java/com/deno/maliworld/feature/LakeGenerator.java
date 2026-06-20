package com.deno.maliworld.feature;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Cria lagos naturais em depressoes do terreno.
 * Tamanho e forma variam por localizacao.
 */
public final class LakeGenerator {

    private LakeGenerator() {}

    public static boolean shouldHaveLake(int chunkX, int chunkZ) {
        double v = SimplexNoise.noise(chunkX * 0.08 + 100, chunkZ * 0.08 + 100);
        return v > 0.65;
    }

    public static void generate(LevelAccessor level, int chunkX, int chunkZ, int seaLevel) {
        if (!shouldHaveLake(chunkX, chunkZ)) return;
        try {
            int cx = chunkX * 16 + 8, cz = chunkZ * 16 + 8;
            int surfaceY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, cx, cz);
            if (surfaceY <= seaLevel + 5 || surfaceY >= seaLevel + 80) return;

            double radius = 4 + SimplexNoise.noise(chunkX * 0.3, chunkZ * 0.3) * 3;
            int lakeY = surfaceY - 1;
            for (int dx = -(int)radius - 1; dx <= (int)radius + 1; dx++) {
                for (int dz = -(int)radius - 1; dz <= (int)radius + 1; dz++) {
                    double noise = SimplexNoise.noise(dx * 0.3, dz * 0.3) * 1.5;
                    double dist = Math.sqrt(dx*dx + dz*dz) + noise;
                    if (dist <= radius) {
                        BlockPos pos = new BlockPos(cx + dx, lakeY, cz + dz);
                        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
                        level.setBlock(new BlockPos(cx+dx, lakeY-1, cz+dz), Blocks.SAND.defaultBlockState(), 2);
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}