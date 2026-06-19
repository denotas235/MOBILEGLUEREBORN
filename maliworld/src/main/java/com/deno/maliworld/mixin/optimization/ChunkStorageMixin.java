package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

/**
 * Hook para telemetria de carregamento de chunks.
 * Lógica real de pre-load de noise vizinhos vai em ChunkGenOptimizer.
 */
@Mixin(ServerChunkCache.class)
public abstract class ChunkStorageMixin {

    @Inject(method = "updateChunks", at = @At("HEAD"))
    private void onUpdateChunks(CallbackInfo ci) {
    }
}
