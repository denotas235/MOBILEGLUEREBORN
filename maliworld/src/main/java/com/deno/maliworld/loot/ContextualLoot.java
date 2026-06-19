package com.deno.maliworld.loot;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * Loot pool builders for each MaliWorld structure type.
 * Uses only vanilla MC classes and Loot API present in 1.21.11.
 */
public final class ContextualLoot {

    private ContextualLoot() {}

    public static LootPool ruinPool() {
        return LootPool.lootPool()
            .setRolls(UniformGenerator.between(1, 3))
            .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(3))
            .add(LootItem.lootTableItem(Items.BREAD).setWeight(5))
            .add(LootItem.lootTableItem(Items.COAL).setWeight(4))
            .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(3))
            .add(LootItem.lootTableItem(Items.FLINT).setWeight(4))
            .add(LootItem.lootTableItem(Items.BONE).setWeight(3))
            .add(LootItem.lootTableItem(Items.BOOK).setWeight(2))
            .build();
    }

    public static LootPool fortressPool() {
        return LootPool.lootPool()
            .setRolls(UniformGenerator.between(2, 5))
            .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(1))
            .add(LootItem.lootTableItem(Items.IRON_CHESTPLATE).setWeight(2))
            .add(LootItem.lootTableItem(Items.IRON_HELMET).setWeight(2))
            .add(LootItem.lootTableItem(Items.ARROW).setWeight(5))
            .add(LootItem.lootTableItem(Items.GUNPOWDER).setWeight(3))
            .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(2))
            .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(1))
            .build();
    }

    public static LootPool templePool() {
        return LootPool.lootPool()
            .setRolls(UniformGenerator.between(2, 4))
            .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(2))
            .add(LootItem.lootTableItem(Items.EMERALD).setWeight(2))
            .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(4))
            .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(2))
            .add(LootItem.lootTableItem(Items.NAME_TAG).setWeight(1))
            .add(LootItem.lootTableItem(Items.EXPERIENCE_BOTTLE).setWeight(3))
            .add(LootItem.lootTableItem(Items.GOLDEN_APPLE).setWeight(1))
            .add(LootItem.lootTableItem(Items.ENDER_PEARL).setWeight(2))
            .build();
    }

    public static LootPool undergroundCityPool() {
        return LootPool.lootPool()
            .setRolls(UniformGenerator.between(3, 6))
            .add(LootItem.lootTableItem(Items.NETHERITE_SCRAP).setWeight(1))
            .add(LootItem.lootTableItem(Items.DIAMOND_PICKAXE).setWeight(1))
            .add(LootItem.lootTableItem(Items.DIAMOND_SWORD).setWeight(1))
            .add(LootItem.lootTableItem(Items.ENCHANTED_GOLDEN_APPLE).setWeight(1))
            .add(LootItem.lootTableItem(Items.DIAMOND).setWeight(3))
            .add(LootItem.lootTableItem(Items.EMERALD).setWeight(3))
            .add(LootItem.lootTableItem(Items.ENCHANTED_BOOK).setWeight(3))
            .add(LootItem.lootTableItem(Items.ANCIENT_DEBRIS).setWeight(1))
            .build();
    }
}