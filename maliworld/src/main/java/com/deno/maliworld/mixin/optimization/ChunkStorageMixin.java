package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.optimization.ChunkGenOptimizer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Hooks into ChunkMap to pre-warm the noise cache when a new chunk is requested.
 * This triggers neighbor pre-computation so that when the chunk generator runs,
 * adjacent chunk heights are already cached.
 *
 * Target: net.minecraft.server.level.ChunkMap
 * require=0: ChunkMap internals change frequently.
 */
@Mixin(value = ChunkMap.class, remap = true)
public abstract class ChunkStorageMixin {

    @Inject(
        method = "scheduleChunkLoad",
        at = @At("HEAD"),
        require = 0
    )
    private void maliworld$onChunkLoad(net.minecraft.world.level.ChunkPos chunkPos,
                                        CallbackInfoReturnable<CompletableFuture<?>> cir) {
        // Pre-warm noise cache for this chunk and neighbors
        ChunkGenOptimizer.precomputeNeighbors(chunkPos.x, chunkPos.z);
    }
}