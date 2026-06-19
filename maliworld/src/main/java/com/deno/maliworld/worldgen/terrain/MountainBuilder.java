package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.worldgen.noise.FractalNoise;

/** Gera cadeias de montanhas coerentes. */
public final class MountainBuilder {

    private final FractalNoise ridgeNoise;

    public MountainBuilder(long seed) {
        this.ridgeNoise = new FractalNoise(5, 0.5f, 2.0f, seed ^ 0xBADC0DEEL);
    }

    /** Returns 0.0-1.0 mountain intensity at position. */
    public float getMountainFactor(int blockX, int blockZ) {
        float raw = ridgeNoise.sample(blockX / 800.0f, blockZ / 800.0f);
        float ridge = 1.0f - Math.abs(raw);
        return Math.max(0, ridge * ridge - 0.1f);
    }
}
