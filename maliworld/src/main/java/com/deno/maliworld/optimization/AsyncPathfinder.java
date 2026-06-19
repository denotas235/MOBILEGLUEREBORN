package com.deno.maliworld.optimization;

import net.minecraft.world.entity.PathfinderMob;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache de pathfinding assíncrono.
 * Guarda o resultado da última rota calculada por entidade para reutilização.
 */
public final class AsyncPathfinder {

    private static final int CACHE_TTL_TICKS = 20;

    private static final Map<UUID, CachedPath> cache = new ConcurrentHashMap<>();

    private AsyncPathfinder() {}

    public static boolean hasCachedPath(PathfinderMob entity) {
        CachedPath p = cache.get(entity.getUUID());
        return p != null && (entity.tickCount - p.cachedAtTick) < CACHE_TTL_TICKS;
    }

    public static void cachePath(PathfinderMob entity) {
        cache.put(entity.getUUID(), new CachedPath(entity.tickCount));
    }

    public static void evict(UUID id) {
        cache.remove(id);
    }

    private static final class CachedPath {
        final int cachedAtTick;
        CachedPath(int tick) { this.cachedAtTick = tick; }
    }
}
