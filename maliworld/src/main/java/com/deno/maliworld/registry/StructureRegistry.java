package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;

public final class StructureRegistry {

    private StructureRegistry() {}

    public static void register() {
        MaliWorldMod.LOGGER.info("[MaliWorld] StructureRegistry: estruturas ativas via data-driven JSON.");
    }
}