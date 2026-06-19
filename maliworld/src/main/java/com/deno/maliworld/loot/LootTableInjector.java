package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Injects MaliWorld loot into vanilla loot tables using Fabric LootTableEvents.
 * Adds contextual loot pools to dungeon, village, mineshaft and bastion chests.
 */
public final class LootTableInjector {

    private LootTableInjector() {}

    public static void inject() {
        // Add ruin-style loot to dungeon chests
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;

            Identifier id = key.location();

            // Dungeons → ruin-style loot supplement
            if (id.equals(BuiltInLootTables.SIMPLE_DUNGEON.location())) {
                tableBuilder.pool(ContextualLoot.ruinPool());
            }
            // Village armorer → fortress-style loot
            else if (id.toString().contains("village") && id.toString().contains("armorer")) {
                tableBuilder.pool(ContextualLoot.fortressPool());
            }
            // Jungle temple → temple loot
            else if (id.equals(BuiltInLootTables.JUNGLE_TEMPLE.location())) {
                tableBuilder.pool(ContextualLoot.templePool());
            }
            // Desert pyramid → temple loot
            else if (id.equals(BuiltInLootTables.DESERT_PYRAMID.location())) {
                tableBuilder.pool(ContextualLoot.templePool());
            }
            // Ancient city → underground city loot
            else if (id.equals(BuiltInLootTables.ANCIENT_CITY.location())) {
                tableBuilder.pool(ContextualLoot.undergroundCityPool());
            }
            // Stronghold → fortress loot
            else if (id.equals(BuiltInLootTables.STRONGHOLD_CROSSING.location())) {
                tableBuilder.pool(ContextualLoot.fortressPool());
            }
        });

        MaliWorldMod.LOGGER.info("[MaliWorld] Loot contextual injetado em tabelas vanilla.");
    }
}