package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.worldgen.noise.FractalNoise;

/** Determina se uma posição é terra (>0) ou oceano (≤0). */
public final class ContinentGenerator {

    private final FractalNoise noise;

    public ContinentGenerator(long seed) {
        this.noise = new FractalNoise(4, 0.5f, 2.0f, seed ^ 0xC0C0C0CL);
    }

    public float getContinentValue(int blockX, int blockZ) {
        return noise.sample(blockX / 3000.0f, blockZ / 3000.0f);
    }

    public boolean isLand(int blockX, int blockZ) {
        return getContinentValue(blockX, blockZ) > -0.1f;
    }
}
