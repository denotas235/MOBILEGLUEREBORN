package com.deno.maliworld.feature;

/** Cria caminhos orgânicos de terra batida entre estruturas. */
public final class PathGenerator {

    private PathGenerator() {}

    public static boolean isPath(int blockX, int blockZ, long seed) {
        if (!com.deno.maliworld.config.MaliWorldConfig.PATHS_ENABLED) return false;
        float v = com.deno.maliworld.worldgen.noise.SimplexNoise.noise(
                blockX / 80.0f + seed * 0.0005f, blockZ / 80.0f);
        return Math.abs(v) < 0.03f;
    }
}
