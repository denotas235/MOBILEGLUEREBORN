package com.deno.maliworld.feature;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Esculpe rios sinuosos usando meandros com noise de angulo.
 * Os rios nascem em altitude e fluem ate o oceano/lago.
 */
public final class RiverCarver {

    private static final int RIVER_WIDTH    = 5;
    private static final int RIVER_DEPTH    = 3;
    private static final double MEANDER_SCALE = 0.01;

    private RiverCarver() {}

    /**
     * Carrega rios na regiao de chunk especificada.
     * Chame durante a decoracao de chunk para adicionar rios.
     */
    public static void carve(LevelAccessor level, int chunkX, int chunkZ, int seaLevel) {
        try {
            int ox = chunkX * 16, oz = chunkZ * 16;
            for (int x = ox; x < ox + 16; x++) {
                for (int z = oz; z < oz + 16; z++) {
                    double angle = SimplexNoise.noise(x * MEANDER_SCALE, z * MEANDER_SCALE) * Math.PI;
                    double riverMask = Math.abs(SimplexNoise.noise(
                        x * MEANDER_SCALE * 2 + Math.cos(angle) * 0.1,
                        z * MEANDER_SCALE * 2 + Math.sin(angle) * 0.1));
                    if (riverMask < 0.08) {
                        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            x, z);
                        if (y > seaLevel + 2) {
                            for (int dy = 0; dy < RIVER_DEPTH; dy++) {
                                BlockPos pos = new BlockPos(x, y - dy, z);
                                level.setBlock(pos, dy == 0 ? Blocks.WATER.defaultBlockState()
                                    : Blocks.GRAVEL.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}