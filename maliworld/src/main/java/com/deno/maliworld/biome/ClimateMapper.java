package com.deno.maliworld.biome;

import com.deno.maliworld.noise.FractalNoise;

/**
 * Define temperatura e umidade por posicao no mundo.
 * Usa latitude e altitude para calcular clima realista.
 */
public final class ClimateMapper {

    private static final FractalNoise TEMP_NOISE     = new FractalNoise(3, 0.5, 2.0, 0.002);
    private static final FractalNoise HUMIDITY_NOISE = new FractalNoise(3, 0.5, 2.0, 0.0025);

    private ClimateMapper() {}

    /** @return temperatura em [0,1]: 0=gelido, 1=tropical */
    public static float temperature(double worldX, double worldZ, int blockY) {
        double base = (TEMP_NOISE.sample(worldX, worldZ) + 1.0) * 0.5;
        double altitudePenalty = Math.max(0, (blockY - 80) / 120.0) * 0.3;
        return (float) Math.max(0, Math.min(1, base - altitudePenalty));
    }

    /** @return umidade em [0,1]: 0=arido, 1=umido */
    public static float humidity(double worldX, double worldZ) {
        return (float)((HUMIDITY_NOISE.sample(worldX, worldZ) + 1.0) * 0.5);
    }

    public enum Zone { TROPICAL, TEMPERATE, COLD, ARCTIC }

    public static Zone zone(double worldX, double worldZ, int blockY) {
        float t = temperature(worldX, worldZ, blockY);
        if (t > 0.7) return Zone.TROPICAL;
        if (t > 0.4) return Zone.TEMPERATE;
        if (t > 0.2) return Zone.COLD;
        return Zone.ARCTIC;
    }
}