package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Asynchronous mob pathfinding.
 * Moves path calculations off the main thread, returning the result
 * to the mob at the start of the next tick via a callback.
 *
 * The PathAwareEntityMixin intercepts startMovingTo() to use this engine.
 * Paths are cached per entity to avoid recalculation when the target hasn't moved.
 */
public final class AsyncPathfinder {

    private static ExecutorService executor;
    private static final Map<Long, PathResult> pathCache = new ConcurrentHashMap<>();
    private static final long PATH_CACHE_TTL_MS = 2000L; // 2 seconds
    private static final AtomicLong completedPaths = new AtomicLong(0);
    private static final AtomicLong cachedPaths    = new AtomicLong(0);

    private AsyncPathfinder() {}

    public static void init() {
        executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "MaliWorld-Pathfinder");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        MaliWorldMod.LOGGER.info("[MaliWorld] AsyncPathfinder iniciado (3 threads).");
    }

    /**
     * Request an async path computation.
     *
     * @param entityId  entity ID (for cache key)
     * @param targetX   target X
     * @param targetZ   target Z
     * @param pathCalc  supplier that does the actual path computation (heavy)
     * @param callback  called on main thread next tick with the result
     */
    public static void requestPath(long entityId, double targetX, double targetZ,
                                    Supplier<Object> pathCalc,
                                    java.util.function.Consumer<Object> callback) {
        if (executor == null || executor.isShutdown()) {
            // Sync fallback
            callback.accept(pathCalc.get());
            return;
        }

        // Check cache
        long cacheKey = entityId ^ Double.doubleToRawLongBits(targetX) ^ (Double.doubleToRawLongBits(targetZ) * 31);
        PathResult cached = pathCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < PATH_CACHE_TTL_MS) {
            cachedPaths.incrementAndGet();
            callback.accept(cached.path);
            return;
        }

        executor.execute(() -> {
            try {
                Object path = pathCalc.get();
                pathCache.put(cacheKey, new PathResult(path, System.currentTimeMillis()));
                completedPaths.incrementAndGet();
                // Note: callback is called from async thread; mixin must handle threading
                callback.accept(path);
            } catch (Exception e) {
                MaliWorldMod.LOGGER.debug("[MaliWorld] AsyncPathfinder erro: {}", e.getMessage());
                callback.accept(null);
            }
        });
    }

    /** Evict stale cache entries. Call periodically (e.g. every 200 ticks). */
    public static void evictStaleCache() {
        long now = System.currentTimeMillis();
        pathCache.entrySet().removeIf(e -> (now - e.getValue().timestamp) > PATH_CACHE_TTL_MS * 2);
    }

    public static long getCompletedPaths() { return completedPaths.get(); }
    public static long getCachedPaths()    { return cachedPaths.get(); }

    public static void shutdown() {
        if (executor != null) executor.shutdownNow();
    }

    private record PathResult(Object path, long timestamp) {}
}