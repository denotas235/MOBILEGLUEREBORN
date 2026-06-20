package com.deno.maliworld.surface;

import com.deno.maliworld.biome.ClimateMapper;
import com.deno.maliworld.worldgen.ContinentGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Seleciona o bloco de superficie baseado em contexto:
 * altitude, inclinacao, temperatura, umidade.
 */
public final class SurfaceDecorator {

    private SurfaceDecorator() {}

    public static BlockState selectSurface(double worldX, double worldZ, int blockY,
                                           int seaLevel, double slopeGradient) {
        try {
            float temp = ClimateMapper.temperature(worldX, worldZ, blockY);
            float humidity = ClimateMapper.humidity(worldX, worldZ);
            boolean coast = ContinentGenerator.isCoast(worldX, worldZ);

            // Pico alto → neve
            if (blockY >= seaLevel + 100) return Blocks.SNOW_BLOCK.defaultBlockState();
            // Pico de montanha → pedra exposta
            if (slopeGradient > 0.7 || blockY >= seaLevel + 80)
                return Blocks.STONE.defaultBlockState();
            // Temperatura gelida → grama neve
            if (temp < 0.2) return Blocks.SNOW_BLOCK.defaultBlockState();
            // Costa / praia → areia
            if (coast || (blockY <= seaLevel + 4 && humidity < 0.5))
                return Blocks.SAND.defaultBlockState();
            // Deserto quente e seco → areia
            if (temp > 0.7 && humidity < 0.3) return Blocks.SAND.defaultBlockState();
            // Padrao → grama
            return Blocks.GRASS_BLOCK.defaultBlockState();
        } catch (Throwable t) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
    }

    public static BlockState selectSubsurface(double worldX, double worldZ, int blockY, int seaLevel) {
        try {
            float temp = ClimateMapper.temperature(worldX, worldZ, blockY);
            if (temp < 0.2) return Blocks.DIRT.defaultBlockState();
            boolean coast = ContinentGenerator.isCoast(worldX, worldZ);
            if (coast) return Blocks.SAND.defaultBlockState();
            return Blocks.DIRT.defaultBlockState();
        } catch (Throwable t) {
            return Blocks.DIRT.defaultBlockState();
        }
    }
}