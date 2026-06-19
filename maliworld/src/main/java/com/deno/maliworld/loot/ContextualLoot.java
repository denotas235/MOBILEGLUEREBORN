package com.deno.maliworld.loot;

import net.minecraft.resources.Identifier;

/**
 * Define loot contextual para estruturas do MaliWorld.
 * MC 1.21.11: ResourceLocation foi renomeado para Identifier.
 */
public final class ContextualLoot {

    public static final Identifier RUINS_LOOT    = Identifier.fromNamespaceAndPath("maliworld", "chests/ruins");
    public static final Identifier FORTRESS_LOOT = Identifier.fromNamespaceAndPath("maliworld", "chests/fortress");
    public static final Identifier TEMPLE_LOOT   = Identifier.fromNamespaceAndPath("maliworld", "chests/temple");

    private ContextualLoot() {}
}
