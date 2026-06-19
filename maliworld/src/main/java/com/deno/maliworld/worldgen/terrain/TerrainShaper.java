package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.worldgen.noise.DomainWarp;
import com.deno.maliworld.worldgen.noise.FractalNoise;

/**
 * Combina domain warp + FBM para produzir altura de terreno realista.
 */
public final class TerrainShaper {

    private final FractalNoise  continentNoise;
    private final FractalNoise  detailNoise;
    private final DomainWarp    domainWarp;
    private final float         mountainScale;

    public TerrainShaper(long seed, float mountainScale) {
        this.mountainScale  = mountainScale;
        this.continentNoise = new FractalNoise(3, 0.5f, 2.0f, seed);
        this.detailNoise    = new FractalNoise(6, 0.45f, 2.1f, seed ^ 0xFACEFACEL);
        this.domainWarp     = new DomainWarp(seed, 80.0f);
    }

    /** Returns a height multiplier in [-1, 1]. Add to base Y to get final height. */
    public float getHeightFactor(int blockX, int blockZ) {
        float[] warped = domainWarp.warp(blockX / 512.0f, blockZ / 512.0f);
        float continent = continentNoise.sample(warped[0], warped[1]);
        float detail    = detailNoise.sample(blockX / 128.0f, blockZ / 128.0f);

        float base = continent * 0.7f + detail * 0.3f;

        if (base > 0.4f) {
            float mountain = (base - 0.4f) / 0.6f;
            base += mountain * mountain * mountainScale * 0.5f;
        }

        return Math.max(-1.0f, Math.min(1.0f, base));
    }
}
