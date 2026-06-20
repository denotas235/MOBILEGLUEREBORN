package com.deno.maliworld.loot;

/**
 * Seleciona a loot table adequada ao contexto da estrutura.
 * Usa String em vez de ResourceLocation para compatibilidade maxima com MC 1.21.11.
 */
public final class ContextualLoot {

    private ContextualLoot() {}

    /**
     * @return identificador da loot table no formato "namespace:path"
     */
    public static String tableFor(String structureType, double distanceFromSpawn) {
        boolean rare = distanceFromSpawn > 2000;
        return switch (structureType) {
            case "ruins"    -> rare ? "minecraft:chests/stronghold_corridor"
                                    : "minecraft:chests/village_cartographer";
            case "fortress" -> "minecraft:chests/nether_bridge";
            case "temple"   -> rare ? "minecraft:chests/jungle_temple"
                                    : "minecraft:chests/desert_pyramid";
            case "dungeon"  -> "minecraft:chests/simple_dungeon";
            default         -> "minecraft:chests/simple_dungeon";
        };
    }
}