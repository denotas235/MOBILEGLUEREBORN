package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * Injects MaliWorld contextual loot using Fabric API LootTableEvents v3.
 * Adds thematic loot pools to vanilla structure chests.
 *
 * Fabric API 0.141.4+1.21.11 uses net.fabricmc.fabric.api.loot.v3.
 * key.location() returns Identifier (MC 1.21.11 Mojang name for ResourceLocation).
 */
public final class LootTableInjector {

    private LootTableInjector() {}

    public static void inject() {
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (!source.isBuiltin()) return;

            Identifier id = key.location();
            String path   = id.getPath();

            // Dungeons → ruin-style supplement
            if (path.equals("chests/simple_dungeon")) {
                tableBuilder.pool(ContextualLoot.ruinPool());
            }
            // Jungle temple → temple loot
            else if (path.equals("chests/jungle_temple")) {
                tableBuilder.pool(ContextualLoot.templePool());
            }
            // Desert pyramid → temple loot
            else if (path.equals("chests/desert_pyramid")) {
                tableBuilder.pool(ContextualLoot.templePool());
            }
            // Ancient city → underground city loot
            else if (path.equals("chests/ancient_city")) {
                tableBuilder.pool(ContextualLoot.undergroundCityPool());
            }
            // Stronghold → fortress supplement
            else if (path.equals("chests/stronghold_crossing")) {
                tableBuilder.pool(ContextualLoot.fortressPool());
            }
            // Village armorer → fortress style
            else if (path.contains("chests/village") && path.contains("armorer")) {
                tableBuilder.pool(ContextualLoot.fortressPool());
            }
        });

        MaliWorldMod.LOGGER.info("[MaliWorld] Loot contextual injetado em tabelas vanilla.");
    }
}