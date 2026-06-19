package com.deno.maliworld.worldgen.noise;

/**
 * Fractional Brownian Motion (FBM) sobre SimplexNoise.
 * Empilha múltiplas oitavas para criar terreno orgânico.
 */
public final class FractalNoise {

    private final int   octaves;
    private final float persistence;
    private final float lacunarity;
    private final long  seed;

    public FractalNoise(int octaves, float persistence, float lacunarity, long seed) {
        this.octaves     = octaves;
        this.persistence = persistence;
        this.lacunarity  = lacunarity;
        this.seed        = seed;
    }

    public float sample(float x, float z) {
        float value    = 0;
        float amplitude = 1.0f;
        float frequency = 1.0f;
        float maxValue  = 0;

        float ox = (float)(seed ^ 0xDEADBEEFL) * 1e-9f;
        float oz = (float)(seed ^ 0xCAFEBABEL) * 1e-9f;

        for (int i = 0; i < octaves; i++) {
            value    += SimplexNoise.noise((x + ox) * frequency, (z + oz) * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return value / maxValue;
    }
}
