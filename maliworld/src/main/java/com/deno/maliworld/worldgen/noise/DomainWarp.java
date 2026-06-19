package com.deno.maliworld.worldgen.noise;

/**
 * Domain Warping — distorce as coordenadas de entrada do noise antes de samplear.
 * Técnica de Inigo Quilez. Elimina repetição e cria formas únicas de terreno.
 */
public final class DomainWarp {

    private final FractalNoise noiseX;
    private final FractalNoise noiseZ;
    private final float strength;

    public DomainWarp(long seed, float strength) {
        this.noiseX   = new FractalNoise(4, 0.5f, 2.0f, seed ^ 0x1234567L);
        this.noiseZ   = new FractalNoise(4, 0.5f, 2.0f, seed ^ 0x89ABCDEFL);
        this.strength = strength;
    }

    public float[] warp(float x, float z) {
        float wx = x + strength * noiseX.sample(x, z);
        float wz = z + strength * noiseZ.sample(x + 5.2f, z + 1.3f);
        return new float[]{wx, wz};
    }
}
