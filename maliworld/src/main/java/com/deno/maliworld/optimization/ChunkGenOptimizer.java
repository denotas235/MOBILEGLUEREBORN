package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.registry.NoiseRegistry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk generation optimizer: noise height cache with neighbor pre-computation.
 * JAVA NOTE: Lambda captures require effectively final variables.
 *   Loop variables (dx, dz) are reassigned, so we must copy to final locals before use in lambdas.
 */
public final class ChunkGenOptimizer {

    private static final Map<Long, int[]> heightCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 256;

    private ChunkGenOptimizer() {}

    public static void init() {
        MaliWorldMod.LOGGER.info("[MaliWorld] ChunkGenOptimizer inicializado.");
    }

    /** Get (or compute) the 16×16 height map for a chunk. */
    public static int[] getHeightMap(int chunkX, int chunkZ) {
        long key = (long)chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        return heightCache.computeIfAbsent(key, k -> computeHeightMap(chunkX, chunkZ));
    }

    /**
     * Pre-warm cache for a chunk and its 8 neighbors.
     * Fix: loop vars dx/dz are NOT effectively final — copy to fdx/fdz before lambda capture.
     */
    public static void precomputeNeighbors(int chunkX, int chunkZ) {
        if (NoiseRegistry.terrainShaper == null) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final int fdx = dx;          // effectively final copy for lambda
                final int fdz = dz;          // effectively final copy for lambda
                final int nx  = chunkX + fdx;
                final int nz  = chunkZ + fdz;
                long key = (long)nx << 32 | (nz & 0xFFFFFFFFL);
                heightCache.computeIfAbsent(key, k -> computeHeightMap(nx, nz));
            }
        }
        evictIfNeeded();
    }

    private static int[] computeHeightMap(int chunkX, int chunkZ) {
        if (NoiseRegistry.terrainShaper == null) return new int[256];
        int[] heights = new int[256];
        int worldX = chunkX << 4;
        int worldZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                heights[x * 16 + z] = NoiseRegistry.terrainShaper.getHeight(worldX + x, worldZ + z);
            }
        }
        return heights;
    }

    public static int getHeight(int chunkX, int chunkZ, int localX, int localZ) {
        int[] heights = getHeightMap(chunkX, chunkZ);
        int idx = Math.max(0, Math.min(255, localX * 16 + localZ));
        return heights[idx];
    }

    public static void invalidate(int chunkX, int chunkZ) {
        long key = (long)chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        heightCache.remove(key);
    }

    private static void evictIfNeeded() {
        if (heightCache.size() > CACHE_SIZE) {
            int toRemove = heightCache.size() - CACHE_SIZE;
            var it = heightCache.keySet().iterator();
            while (it.hasNext() && toRemove-- > 0) { it.next(); it.remove(); }
        }
    }
}