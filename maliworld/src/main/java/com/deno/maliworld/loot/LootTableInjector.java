package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;

/** Stub seguro — sem dependencia da Fabric loot API. */
public final class LootTableInjector {
    private LootTableInjector() {}
    public static void register() {
        MaliWorldMod.LOGGER.info("[MaliWorld] LootTableInjector: stub activo.");
    }
}