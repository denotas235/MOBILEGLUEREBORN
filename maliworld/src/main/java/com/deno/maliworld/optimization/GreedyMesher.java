package com.deno.maliworld.optimization;

/**
 * Stub do GreedyMesher — a integração real com o SectionRenderDispatcher
 * requer acesso a internals do Minecraft que variam por versão.
 * O hook está em ChunkBuilderMixin e será expandido aqui.
 */
public final class GreedyMesher {

    private GreedyMesher() {}

    public static boolean isEnabled() {
        return com.deno.maliworld.config.MaliWorldConfig.GREEDY_MESHING;
    }
}
