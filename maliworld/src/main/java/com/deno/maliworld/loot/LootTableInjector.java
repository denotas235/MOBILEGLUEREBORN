package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

/**
 * Injeta loot extra em estruturas do MaliWorld.
 */
public final class LootTableInjector {

    private LootTableInjector() {}

    public static void register() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
        });

        MaliWorldMod.LOGGER.info("[MaliWorld] LootTableInjector registado.");
    }
}
