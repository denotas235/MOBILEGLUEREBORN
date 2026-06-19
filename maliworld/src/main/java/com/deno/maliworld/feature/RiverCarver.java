package com.deno.maliworld.feature;

import com.deno.maliworld.worldgen.noise.FractalNoise;

public final class RiverCarver {

    private final FractalNoise riverNoise;

    public RiverCarver(long seed) {
        this.riverNoise = new FractalNoise(3, 0.5f, 2.0f, seed ^ 0xB1EBL);
    }

    public int getRiverDepth(int blockX, int blockZ) {
        if (!com.deno.maliworld.config.MaliWorldConfig.RIVERS_ENABLED) return 0;
        float v = Math.abs(riverNoise.sample(blockX / 600.0f, blockZ / 600.0f));
        if (v < 0.05f) return 4 + (int)((0.05f - v) / 0.05f * 3);
        return 0;
    }
}