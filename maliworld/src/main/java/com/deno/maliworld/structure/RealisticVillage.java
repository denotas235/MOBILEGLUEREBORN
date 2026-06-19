package com.deno.maliworld.structure;

/** Modifica aldeias vanilla para se adaptarem ao terreno. */
public final class RealisticVillage {

    private RealisticVillage() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.REALISTIC_VILLAGES;
    }
}
