package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLightEngine.class)
public abstract class LightingProviderMixin {

    @Inject(method = "checkBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void onCheckBlock(BlockPos pos, CallbackInfo ci) {
        if (!MaliWorldConfig.ASYNC_LIGHTING) return;
        if (AsyncLightEngine.tryDeferUpdate(pos)) {
            ci.cancel();
        }
    }
}