package com.deno.maliworld.optimization;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de rotas de pathfinding para evitar recalculos repetidos.
 * Mobs que ja calcularam o caminho para uma posicao reutilizam o cache.
 */
public final class AsyncPathfinder {

    private static final Map<Long, Long> pathCache = new ConcurrentHashMap<>();
    private static final int CACHE_TTL_TICKS = 40;

    private AsyncPathfinder() {}

    public static boolean hasCachedPath(int entityId, BlockPos target) {
        Long cached = pathCache.get(cacheKey(entityId, target));
        return cached != null;
    }

    public static void cachePath(int entityId, BlockPos target) {
        pathCache.put(cacheKey(entityId, target), System.currentTimeMillis());
        if (pathCache.size() > 512) {
            long cutoff = System.currentTimeMillis() - CACHE_TTL_TICKS * 50L;
            pathCache.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
    }

    private static long cacheKey(int entityId, BlockPos pos) {
        return ((long) entityId << 32) ^ pos.asLong();
    }
}