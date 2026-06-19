package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.feature.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

/**
 * Registers world-generation features (rivers, lakes, paths, caves, waterfalls).
 * Uses Fabric server-world events to hook into world loading.
 */
public final class FeatureRegistry {

    private FeatureRegistry() {}

    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            long seed = world.getSeed();
            if (MaliWorldConfig.RIVERS_ENABLED)    RiverCarver.onWorldLoad(world, seed);
            if (MaliWorldConfig.LAKES_ENABLED)     LakeGenerator.onWorldLoad(world, seed);
            if (MaliWorldConfig.PATHS_ENABLED)     PathGenerator.onWorldLoad(world, seed);
            if (MaliWorldConfig.CAVES_ENHANCED)    CaveEnhancer.onWorldLoad(world, seed);
            if (MaliWorldConfig.WATERFALLS_ENABLED) WaterfallPlacer.onWorldLoad(world, seed);
        });
        MaliWorldMod.LOGGER.info("[MaliWorld] Features registradas.");
    }
}