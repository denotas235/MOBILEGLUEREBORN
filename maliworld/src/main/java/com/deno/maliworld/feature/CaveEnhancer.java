package com.deno.maliworld.feature;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Adiciona detalhes a cavernas vanilla: estalactites, camaras, musgo.
 * Nao regenera cavernas, melhora as existentes.
 */
public final class CaveEnhancer {

    private CaveEnhancer() {}

    public static void enhance(LevelAccessor level, BlockPos.MutableBlockPos mpos,
                               int x, int y, int z) {
        try {
            if (!level.getBlockState(mpos.set(x, y, z)).isAir()) return;
            double cave = SimplexNoise.noise(x * 0.1, y * 0.1, z * 0.1);
            // Estalactite
            if (cave > 0.7 && !level.getBlockState(mpos.set(x, y+1, z)).isAir()) {
                level.setBlock(mpos.set(x, y, z), Blocks.DRIPSTONE_BLOCK.defaultBlockState(), 2);
            }
            // Musgo em paredes umidas
            if (cave > 0.5 && y < 40) {
                if (!level.getBlockState(mpos.set(x+1, y, z)).isAir() ||
                    !level.getBlockState(mpos.set(x-1, y, z)).isAir()) {
                    // parede lateral → pode adicionar musgo
                }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}