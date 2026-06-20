package com.deno.maliworld.structure;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Gera ruinas naturais parcialmente destruidas espalhadas pelo mundo.
 */
public final class NaturalRuins {

    private NaturalRuins() {}

    public static boolean shouldSpawn(int chunkX, int chunkZ) {
        return SimplexNoise.noise(chunkX * 0.12 + 200, chunkZ * 0.12 + 200) > 0.72;
    }

    public static void generate(LevelAccessor level, int chunkX, int chunkZ) {
        if (!shouldSpawn(chunkX, chunkZ)) return;
        try {
            int cx = chunkX * 16 + 8, cz = chunkZ * 16 + 8;
            int groundY = level.getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, cx, cz);
            if (groundY < 64 || groundY > 180) return;

            // Paredes parciais de 3-4 blocos de altura
            int wallH = 2 + (int)(SimplexNoise.noise(chunkX, chunkZ) * 2);
            for (int dy = 0; dy <= wallH; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    // Parede norte
                    double decay = SimplexNoise.noise(dx * 0.5, dy * 0.5 + chunkX) * 0.5 + 0.5;
                    if (decay > 0.35) {
                        level.setBlock(new BlockPos(cx + dx, groundY + dy, cz - 3),
                            Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                        level.setBlock(new BlockPos(cx + dx, groundY + dy, cz + 3),
                            Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                    }
                }
                for (int dz = -3; dz <= 3; dz++) {
                    double decay = SimplexNoise.noise(dz * 0.5 + chunkZ, dy * 0.5) * 0.5 + 0.5;
                    if (decay > 0.35) {
                        level.setBlock(new BlockPos(cx - 3, groundY + dy, cz + dz),
                            Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                        level.setBlock(new BlockPos(cx + 3, groundY + dy, cz + dz),
                            Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 2);
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}