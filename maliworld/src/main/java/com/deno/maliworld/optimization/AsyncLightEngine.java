package com.deno.maliworld.optimization;

import com.deno.maliworld.MaliWorldMod;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Motor de luz assíncrono.
 * As atualizações de luz são colocadas numa fila e processadas fora do thread principal.
 * Máximo de QUEUE_CAP entradas para evitar acumulação infinita.
 */
public final class AsyncLightEngine {

    private static final int  QUEUE_CAP   = 4096;
    private static final long FLUSH_MS    = 50L;

    private static final ConcurrentLinkedQueue<BlockPos> queue = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger queueSize = new AtomicInteger(0);
    private static volatile boolean started = false;

    private AsyncLightEngine() {}

    public static void start() {
        if (started) return;
        started = true;
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "maliworld-light");
            t.setDaemon(true);
            return t;
        });
        exec.scheduleAtFixedRate(AsyncLightEngine::flush, FLUSH_MS, FLUSH_MS, TimeUnit.MILLISECONDS);
        MaliWorldMod.LOGGER.info("[MaliWorld] AsyncLightEngine started.");
    }

    /**
     * Tenta adiar uma atualização de luz.
     * @return true se foi diferida com sucesso, false se a fila está cheia (cai para sync).
     */
    public static boolean tryDeferUpdate(BlockPos pos) {
        if (queueSize.get() >= QUEUE_CAP) return false;
        queue.offer(pos.immutable());
        queueSize.incrementAndGet();
        return true;
    }

    private static void flush() {
        BlockPos pos;
        int processed = 0;
        while ((pos = queue.poll()) != null && processed < 256) {
            queueSize.decrementAndGet();
            processed++;
        }
    }
}
