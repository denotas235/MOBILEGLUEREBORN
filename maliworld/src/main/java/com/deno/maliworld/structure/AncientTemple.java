package com.deno.maliworld.structure;

/** Gera templos de civilizações antigas em biomas específicos. */
public final class AncientTemple {

    private AncientTemple() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.TEMPLES_ENABLED;
    }
}
