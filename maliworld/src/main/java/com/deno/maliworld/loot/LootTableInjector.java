package com.deno.maliworld.loot;

import com.deno.maliworld.MaliWorldMod;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.item.Items;

/**
 * Injeta itens extras nas tabelas de loot vanilla de forma contextual.
 */
public final class LootTableInjector {

    private LootTableInjector() {}

    public static void register() {
        try {
            LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
                String path = key.location().getPath();
                if (path.startsWith("chests/") || path.startsWith("gameplay/")) {
                    tableBuilder.pool(LootPool.lootPool()
                        .when(LootItemRandomChanceCondition.randomChance(0.15f))
                        .add(LootItem.lootTableItem(Items.COMPASS))
                        .build());
                }
            });
            MaliWorldMod.LOGGER.info("[MaliWorld] Loot tables registradas.");
        } catch (Throwable t) {
            MaliWorldMod.LOGGER.warn("[MaliWorld] Loot injection falhou (inofensivo): {}", t.getMessage());
        }
    }
}