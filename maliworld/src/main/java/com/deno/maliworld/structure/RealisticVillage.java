package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;

/**
 * Helper para adaptar casas de aldeia ao terreno.
 * O mixin de aldeia chama este helper para ajustar o Y das estruturas.
 */
public final class RealisticVillage {

    private RealisticVillage() {}

    /** Calcula o Y ideal para colocar uma peca de aldeia. */
    public static int findGroundY(net.minecraft.world.level.LevelReader level,
                                  net.minecraft.core.BlockPos center, int radius) {
        try {
            int totalY = 0, count = 0;
            for (int dx = -radius; dx <= radius; dx += 2) {
                for (int dz = -radius; dz <= radius; dz += 2) {
                    totalY += level.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        center.getX() + dx, center.getZ() + dz);
                    count++;
                }
            }
            return count > 0 ? totalY / count : center.getY();
        } catch (Throwable t) { return center.getY(); }
    }
}