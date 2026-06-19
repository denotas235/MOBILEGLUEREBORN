package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.structure.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Registers structure generation hooks (villages, ruins, fortresses, temples, underground cities).
 */
public final class StructureRegistry {

    private StructureRegistry() {}

    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            long seed = world.getSeed();
            if (MaliWorldConfig.REALISTIC_VILLAGES) RealisticVillage.onWorldLoad(world, seed);
            if (MaliWorldConfig.RUINS_ENABLED)       NaturalRuins.onWorldLoad(world, seed);
            if (MaliWorldConfig.FORTRESS_ENABLED)    MountainFortress.onWorldLoad(world, seed);
            if (MaliWorldConfig.TEMPLES_ENABLED)     AncientTemple.onWorldLoad(world, seed);
            if (MaliWorldConfig.UNDERGROUND_CITIES)  UndergroundCity.onWorldLoad(world, seed);
        });
        MaliWorldMod.LOGGER.info("[MaliWorld] Estruturas registradas.");
    }
}