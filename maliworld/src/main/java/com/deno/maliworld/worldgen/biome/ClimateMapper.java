package com.deno.maliworld.worldgen.biome;

import com.deno.maliworld.worldgen.noise.FractalNoise;

public final class ClimateMapper {

    private static final FractalNoise temperatureNoise = new FractalNoise(3, 0.5f, 2.0f, 0x74E3L);
    private static final FractalNoise humidityNoise    = new FractalNoise(3, 0.5f, 2.0f, 0x8B1DL);

    private ClimateMapper() {}

    public static float getTemperature(int blockX, int blockY, int blockZ) {
        float base = temperatureNoise.sample(blockX / 2000.0f, blockZ / 2000.0f);
        float altitudePenalty = Math.max(0, (blockY - 64) / 200.0f) * 0.5f;
        return base - altitudePenalty;
    }

    public static float getHumidity(int blockX, int blockZ) {
        return humidityNoise.sample(blockX / 1500.0f, blockZ / 1500.0f);
    }
}