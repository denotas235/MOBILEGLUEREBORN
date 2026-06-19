package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.item.Items;

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
