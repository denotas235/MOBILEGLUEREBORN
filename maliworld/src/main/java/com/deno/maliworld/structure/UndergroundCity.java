package com.deno.maliworld.structure;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.noise.SimplexNoise;

/**
 * Placeholder: Cidade subterranea (pesada, desativada por padrao).
 */
public final class UndergroundCity {

    private UndergroundCity() {}

    public static boolean shouldSpawn(int chunkX, int chunkZ) {
        return MaliWorldConfig.UNDERGROUND_CITIES &&
               SimplexNoise.noise(chunkX * 0.03 + 800, chunkZ * 0.03 + 800) > 0.85;
    }
}