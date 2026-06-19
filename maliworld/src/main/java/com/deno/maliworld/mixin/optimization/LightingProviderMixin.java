package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncLightEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts LevelLightEngine.checkBlock() to route light updates through
 * the AsyncLightEngine, preventing main-thread stutter from large lighting events.
 *
 * If async fails (queue full), falls through to vanilla sync path.
 * require=0: degrades gracefully if API changes.
 */
@Mixin(value = LevelLightEngine.class, remap = true)
public abstract class LightingProviderMixin {

    @Inject(
        method = "checkBlock",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void maliworld$interceptCheckBlock(BlockPos blockPos, CallbackInfo ci) {
        if (!MaliWorldConfig.ASYNC_LIGHTING) return;
        // Schedule async; if it succeeds, cancel the synchronous check.
        // AsyncLightEngine queues the pos for re-processing next tick.
        if (AsyncLightEngine.scheduleCheck(blockPos)) {
            ci.cancel(); // defer to async
        }
        // If not scheduled (queue full), vanilla sync runs normally (ci not cancelled)
    }
}