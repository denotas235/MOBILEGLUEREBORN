package com.deno.maliworld.feature;

/** Deteta bordas de penhasco em rios e cria quedas d'água. */
public final class WaterfallPlacer {

    private WaterfallPlacer() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.WATERFALLS_ENABLED;
    }
}
