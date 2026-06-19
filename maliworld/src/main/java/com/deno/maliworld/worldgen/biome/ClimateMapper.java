package com.deno.maliworld.worldgen.biome;

import com.deno.maliworld.worldgen.noise.FractalNoise;

/**
 * Define temperatura e umidade por posição/altitude para biomas realistas.
 */
public final class ClimateMapper {

    private final FractalNoise temperatureNoise;
    private final FractalNoise humidityNoise;

    public ClimateMapper(long seed) {
        this.temperatureNoise = new FractalNoise(3, 0.5f, 2.0f, seed ^ 0x74E3p);
        this.humidityNoise    = new FractalNoise(3, 0.5f, 2.0f, seed ^ 0x8B1Dp);
    }

    /** Returns temperature in [-1, 1]. High altitude = colder. */
    public float getTemperature(int blockX, int blockY, int blockZ) {
        float base = temperatureNoise.sample(blockX / 2000.0f, blockZ / 2000.0f);
        float altitudePenalty = Math.max(0, (blockY - 64) / 200.0f) * 0.5f;
        return base - altitudePenalty;
    }

    /** Returns humidity in [-1, 1]. */
    public float getHumidity(int blockX, int blockZ) {
        return humidityNoise.sample(blockX / 1500.0f, blockZ / 1500.0f);
    }
}
