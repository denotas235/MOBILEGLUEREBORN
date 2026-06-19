package com.deno.maliworld.feature;

import com.deno.maliworld.worldgen.noise.SimplexNoise;

/** Gera lagos naturais em depressões do terreno. */
public final class LakeGenerator {

    private LakeGenerator() {}

    public static boolean isLakePosition(int blockX, int blockZ, long seed) {
        if (!com.deno.maliworld.config.MaliWorldConfig.LAKES_ENABLED) return false;
        float v = SimplexNoise.noise(blockX / 200.0f + seed * 0.001f, blockZ / 200.0f);
        return v > 0.75f;
    }
}
