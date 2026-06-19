package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;

public final class FeatureRegistry {

    private FeatureRegistry() {}

    public static void register() {
        MaliWorldMod.LOGGER.info("[MaliWorld] FeatureRegistry: features ativas via worldgen mixins.");
    }
}