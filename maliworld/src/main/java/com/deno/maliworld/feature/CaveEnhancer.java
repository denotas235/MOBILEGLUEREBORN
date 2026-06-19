package com.deno.maliworld.feature;

/** Adiciona câmaras grandes e estalactites às cavernas vanilla. */
public final class CaveEnhancer {

    private CaveEnhancer() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.CAVES_ENHANCED;
    }
}
