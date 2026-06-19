package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Injects MaliWorld contextual loot into vanilla loot tables.
 * Fabric API LootTableEvents v3 (0.141.4+1.21.11).
 *
 * Uses ResourceKey.equals() directly — avoids deprecated/removed location() call.
 * BuiltInLootTables fields are ResourceKey<LootTable> in 1.21.11.
 */
public final class LootTableInjector {

    private LootTableInjector() {}

    public static void inject() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;

            if (key.equals(BuiltInLootTables.SIMPLE_DUNGEON)) {
                tableBuilder.pool(ContextualLoot.ruinPool());
            } else if (key.equals(BuiltInLootTables.JUNGLE_TEMPLE)) {
                tableBuilder.pool(ContextualLoot.templePool());
            } else if (key.equals(BuiltInLootTables.DESERT_PYRAMID)) {
                tableBuilder.pool(ContextualLoot.templePool());
            } else if (key.equals(BuiltInLootTables.ANCIENT_CITY)) {
                tableBuilder.pool(ContextualLoot.undergroundCityPool());
            } else if (key.equals(BuiltInLootTables.STRONGHOLD_CROSSING)) {
                tableBuilder.pool(ContextualLoot.fortressPool());
            }
        });

        MaliWorldMod.LOGGER.info("[MaliWorld] Loot contextual injetado em tabelas vanilla.");
    }
}