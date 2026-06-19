package com.deno.maliworld.loot;

import net.minecraft.resources.ResourceLocation;

/**
 * Define loot contextual para estruturas do MaliWorld.
 */
public final class ContextualLoot {

    public static final ResourceLocation RUINS_LOOT    = ResourceLocation.fromNamespaceAndPath("maliworld", "chests/ruins");
    public static final ResourceLocation FORTRESS_LOOT = ResourceLocation.fromNamespaceAndPath("maliworld", "chests/fortress");
    public static final ResourceLocation TEMPLE_LOOT   = ResourceLocation.fromNamespaceAndPath("maliworld", "chests/temple");

    private ContextualLoot() {}
}
