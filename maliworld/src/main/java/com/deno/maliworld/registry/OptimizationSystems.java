package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.*;

/**
 * Initializes all optimization subsystems.
 * Each system manages its own thread pools and caches.
 */
public final class OptimizationSystems {

    private OptimizationSystems() {}

    public static void init() {
        if (MaliWorldConfig.ASYNC_LIGHTING)    AsyncLightEngine.init();
        if (MaliWorldConfig.ASYNC_PATHFINDING) AsyncPathfinder.init();
        if (MaliWorldConfig.GREEDY_MESHING)    GreedyMesher.init();
        if (MaliWorldConfig.ELYTRA_PREDICTOR)  ElytraPredictor.init();
        ChunkGenOptimizer.init();
        MaliWorldMod.LOGGER.info("[MaliWorld] Sistemas de otimizacao inicializados.");
    }
}