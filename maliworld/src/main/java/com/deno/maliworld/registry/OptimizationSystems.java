package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncLightEngine;
import com.deno.maliworld.optimization.ChunkGenOptimizer;

public final class OptimizationSystems {

    private OptimizationSystems() {}

    public static void init() {
        if (MaliWorldConfig.ASYNC_LIGHTING) AsyncLightEngine.start();
        ChunkGenOptimizer.init();
        MaliWorldMod.LOGGER.info("[MaliWorld] Sistemas de otimizacao inicializados.");
    }
}