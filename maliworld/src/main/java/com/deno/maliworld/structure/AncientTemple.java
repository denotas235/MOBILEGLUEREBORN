package com.deno.maliworld.structure;

import com.deno.maliworld.noise.SimplexNoise;

/**
 * Placeholder: Templo antigo adaptado ao bioma.
 */
public final class AncientTemple {

    private AncientTemple() {}

    public static boolean shouldSpawn(int chunkX, int chunkZ) {
        return SimplexNoise.noise(chunkX * 0.07 + 600, chunkZ * 0.07 + 600) > 0.80;
    }
}