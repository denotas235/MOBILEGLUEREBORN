package com.deno.maliworld.loot;

import net.minecraft.resources.ResourceLocation;

/**
 * Define qual tabela de loot usar baseado no contexto da estrutura.
 */
public final class ContextualLoot {

    private ContextualLoot() {}

    public static ResourceLocation tableFor(String structureType, double distance) {
        boolean isRare = distance > 2000;
        return switch (structureType) {
            case "ruins"    -> ResourceLocation.withDefaultNamespace(isRare ? "chests/stronghold_corridor"   : "chests/village_cartographer");
            case "fortress" -> ResourceLocation.withDefaultNamespace("chests/nether_bridge");
            case "temple"   -> ResourceLocation.withDefaultNamespace(isRare ? "chests/jungle_temple"        : "chests/desert_pyramid");
            default         -> ResourceLocation.withDefaultNamespace("chests/simple_dungeon");
        };
    }
}