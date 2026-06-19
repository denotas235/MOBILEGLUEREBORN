package com.deno.maliworld.structure;

/** Gera fortalezas no topo de montanhas acima de Y=140. */
public final class MountainFortress {

    private MountainFortress() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.FORTRESS_ENABLED;
    }
}
