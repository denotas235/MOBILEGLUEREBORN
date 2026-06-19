package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.registry.NoiseRegistry;
import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chunk generation optimizer.
 * Pre-caches noise values for neighboring chunks to avoid redundant sampling.
 * Uses a moving window cache keyed by chunk coordinates.
 *
 * Called from ChunkStorageMixin when a new chunk is loaded/generated.
 */
public final class ChunkGenOptimizer {

    // Cache: chunkKey → height array [16*16]
    private static final Map<Long, int[]> heightCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 256; // max chunks cached

    private ChunkGenOptimizer() {}

    public static void init() {
        MaliWorldMod.LOGGER.info("[MaliWorld] ChunkGenOptimizer inicializado.");
    }

    /**
     * Get (or compute) the height map for a chunk.
     * Returned array is [16*16] with Y values.
     */
    public static int[] getHeightMap(int chunkX, int chunkZ) {
        long key = (long)chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        return heightCache.computeIfAbsent(key, k -> computeHeightMap(chunkX, chunkZ));
    }

    /**
     * Pre-compute heights for a chunk and its 8 neighbors.
     * Call when a chunk starts generating to warm the cache.
     */
    public static void precomputeNeighbors(int chunkX, int chunkZ) {
        if (NoiseRegistry.terrainShaper == null) return;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long key = (long)(chunkX+dx) << 32 | ((chunkZ+dz) & 0xFFFFFFFFL);
                heightCache.computeIfAbsent(key, k -> computeHeightMap(chunkX+dx, chunkZ+dz));
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

    /** Retrieve pre-computed height at local chunk column. */
    public static int getHeight(int chunkX, int chunkZ, int localX, int localZ) {
        int[] heights = getHeightMap(chunkX, chunkZ);
        int idx = Math.max(0, Math.min(255, localX * 16 + localZ));
        return heights[idx];
    }

    /** Invalidate a specific chunk from the cache. */
    public static void invalidate(int chunkX, int chunkZ) {
        long key = (long)chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        heightCache.remove(key);
    }

    private static void evictIfNeeded() {
        if (heightCache.size() > CACHE_SIZE) {
            // Evict random entries to stay within limit
            int toRemove = heightCache.size() - CACHE_SIZE;
            var it = heightCache.keySet().iterator();
            while (it.hasNext() && toRemove-- > 0) {
                it.next(); it.remove();
            }
        }
    }
}