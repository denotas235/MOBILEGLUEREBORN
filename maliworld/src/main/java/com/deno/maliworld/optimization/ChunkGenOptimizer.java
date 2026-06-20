package com.deno.maliworld.optimization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Otimiza a geracao de chunks controlando o numero de chunks
 * que sao gerados simultaneamente para evitar stutter.
 */
public final class ChunkGenOptimizer {

    private static final int MAX_GEN_PER_TICK = 2;
    private static final AtomicInteger genThisTick = new AtomicInteger(0);
    private static final ConcurrentHashMap<Long, Long> genTimestamps = new ConcurrentHashMap<>();

    private ChunkGenOptimizer() {}

    public static boolean canGenerate(int chunkX, int chunkZ) {
        if (genThisTick.getAndIncrement() >= MAX_GEN_PER_TICK) return false;
        long key = (long) chunkX << 32 | (chunkZ & 0xFFFFFFFFL);
        Long last = genTimestamps.get(key);
        if (last != null && System.currentTimeMillis() - last < 50) return false;
        genTimestamps.put(key, System.currentTimeMillis());
        return true;
    }

    public static void resetTick() { genThisTick.set(0); }
}