package com.deno.maliworld.optimization;

/**
 * Stub do otimizador de geração de chunks.
 * Pré-calcula noise de chunks vizinhos em background.
 */
public final class ChunkGenOptimizer {

    private ChunkGenOptimizer() {}

    public static void init() {
        if (com.deno.maliworld.config.MaliWorldConfig.ENHANCED_TERRAIN) {
            com.deno.maliworld.MaliWorldMod.LOGGER.info("[MaliWorld] ChunkGenOptimizer pronto.");
        }
    }
}
