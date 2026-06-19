package com.deno.maliworld.structure;

/** Cidades subterrâneas em câmaras naturais profundas (desativado por padrão). */
public final class UndergroundCity {

    private UndergroundCity() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.UNDERGROUND_CITIES;
    }
}
