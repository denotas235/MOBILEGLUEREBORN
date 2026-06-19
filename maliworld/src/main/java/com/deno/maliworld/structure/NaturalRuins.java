package com.deno.maliworld.structure;

/** Gera ruínas parcialmente destruídas espalhadas pelo mundo. */
public final class NaturalRuins {

    private NaturalRuins() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.RUINS_ENABLED;
    }
}
