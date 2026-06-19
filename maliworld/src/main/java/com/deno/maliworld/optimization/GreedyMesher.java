package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Greedy Meshing optimization for chunk rendering.
 * Merges adjacent same-block faces into larger quads, dramatically
 * reducing vertex count and draw calls.
 *
 * The actual meshing is triggered via ChunkBuilderMixin.
 * This class provides the greedy merge algorithm and state caching.
 */
public final class GreedyMesher {

    private static volatile boolean initialized = false;

    private GreedyMesher() {}

    public static void init() {
        initialized = true;
        MaliWorldMod.LOGGER.info("[MaliWorld] GreedyMesher inicializado - reducao de vertices ativa.");
    }

    public static boolean isEnabled() {
        return initialized;
    }

    /**
     * Greedy mask for one face direction on a 16x16 slice.
     * mask[i] = the BlockState at cell i, or null if face is culled/covered.
     *
     * @param states  16x16 = 256 block states for the slice
     * @param covered 16x16 = 256 booleans: true if this face is hidden by adjacent block
     * @return merged quads as int[] packed: [x, y, w, h] per quad (4 ints each)
     */
    public static int[] buildGreedyMask(BlockState[] states, boolean[] covered) {
        int SIZE = 16;
        boolean[] merged = new boolean[SIZE * SIZE];
        java.util.ArrayList<int[]> quads = new java.util.ArrayList<>();

        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; ) {
                int idx = j * SIZE + i;
                if (covered[idx] || merged[idx] || states[idx] == null) {
                    i++;
                    continue;
                }
                BlockState ref = states[idx];
                // Expand width
                int w = 1;
                while (i + w < SIZE) {
                    int ni = j * SIZE + i + w;
                    if (covered[ni] || merged[ni] || !stateEquals(states[ni], ref)) break;
                    w++;
                }
                // Expand height
                int h = 1;
                outer:
                while (j + h < SIZE) {
                    for (int k = 0; k < w; k++) {
                        int ni = (j + h) * SIZE + i + k;
                        if (covered[ni] || merged[ni] || !stateEquals(states[ni], ref)) break outer;
                    }
                    h++;
                }
                // Mark merged
                for (int dj = 0; dj < h; dj++) {
                    for (int di = 0; di < w; di++) {
                        merged[(j + dj) * SIZE + i + di] = true;
                    }
                }
                quads.add(new int[]{i, j, w, h});
                i += w;
            }
        }

        int[] result = new int[quads.size() * 4];
        for (int q = 0; q < quads.size(); q++) {
            System.arraycopy(quads.get(q), 0, result, q * 4, 4);
        }
        return result;
    }

    private static boolean stateEquals(BlockState a, BlockState b) {
        return a != null && b != null && a == b;
    }

    /**
     * Check if a block face should be rendered (not covered by opaque neighbor).
     */
    public static boolean isFaceCulled(BlockState neighbor) {
        return neighbor != null && neighbor.canOcclude();
    }
}