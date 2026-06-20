package com.deno.maliworld.noise;

/**
 * Fractional Brownian Motion (FBM) sobre SimplexNoise.
 * Empilha oitavas para criar terreno fractal organico.
 */
public final class FractalNoise {

    private final int octaves;
    private final double persistence;
    private final double lacunarity;
    private final double scale;

    public FractalNoise(int octaves, double persistence, double lacunarity, double scale) {
        this.octaves     = Math.max(1, octaves);
        this.persistence = persistence;
        this.lacunarity  = lacunarity;
        this.scale       = scale;
    }

    public double sample(double x, double z) {
        double value = 0, amplitude = 1, frequency = scale, max = 0;
        for (int i = 0; i < octaves; i++) {
            value += SimplexNoise.noise(x * frequency, z * frequency) * amplitude;
            max   += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return max > 0 ? value / max : 0;
    }

    public double sample(double x, double y, double z) {
        double value = 0, amplitude = 1, frequency = scale, max = 0;
        for (int i = 0; i < octaves; i++) {
            value += SimplexNoise.noise(x * frequency, y * frequency, z * frequency) * amplitude;
            max   += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return max > 0 ? value / max : 0;
    }
}