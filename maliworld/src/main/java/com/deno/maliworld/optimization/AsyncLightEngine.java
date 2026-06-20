package com.deno.maliworld.optimization;

import net.minecraft.core.BlockPos;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rastreia atualizacoes de luz e as enfileira para processamento assincrono.
 * Na pratica, as atualizacoes ainda sao processadas pelo engine vanilla —
 * esta classe coleta estatisticas e sinaliza quando ha atividade de luz.
 */
public final class AsyncLightEngine {

    private static final ConcurrentLinkedQueue<Long> pending = new ConcurrentLinkedQueue<>();
    private static final AtomicLong totalUpdates = new AtomicLong();

    private AsyncLightEngine() {}

    public static void notifyBlockUpdate(BlockPos pos) {
        pending.offer(pos.asLong());
        totalUpdates.incrementAndGet();
        // Limita a fila para evitar acumulo de memoria
        while (pending.size() > 256) pending.poll();
    }

    public static int pendingCount() { return pending.size(); }
    public static long totalCount()  { return totalUpdates.get(); }
}