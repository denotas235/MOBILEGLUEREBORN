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
 * Rastreia atualizacoes de luz para o AsyncLightEngine.
 * require = 0: nunca crasha.
 */
@Mixin(LevelLightEngine.class)
public abstract class LightingProviderMixin {

    @Inject(method = "checkBlock", at = @At("HEAD"), require = 0)
    private void mw_trackLightUpdate(BlockPos pos, CallbackInfo ci) {
        if (!MaliWorldConfig.ASYNC_LIGHTING) return;
        try {
            AsyncLightEngine.notifyBlockUpdate(pos);
        } catch (Throwable t) { /* never crash */ }
    }
}