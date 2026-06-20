package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncLightEngine;

/**
 * Inicializa os sistemas de otimizacao do MaliWorld.
 */
public final class OptimizationSystems {

    private OptimizationSystems() {}

    public static void init() {
        if (MaliWorldConfig.ASYNC_LIGHTING) {
            MaliWorldMod.LOGGER.info(
                "[MaliWorld] AsyncLightEngine activo (pending={}).",
                AsyncLightEngine.pendingCount());
        }
        // ChunkGenOptimizer e AsyncPathfinder sao inicializados estaticamente.
        MaliWorldMod.LOGGER.info("[MaliWorld] Sistemas de otimizacao inicializados.");
    }
}