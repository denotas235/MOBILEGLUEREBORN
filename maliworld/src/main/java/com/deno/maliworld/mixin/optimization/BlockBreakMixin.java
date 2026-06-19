package com.deno.maliworld.mixin.optimization;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class BlockBreakMixin {

    private int mw_rebuildsThisTick = 0;

    @Inject(method = "destroyBlock", at = @At("HEAD"), require = 0)
    private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        mw_rebuildsThisTick = 0;
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 0)
    private void afterDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        mw_rebuildsThisTick++;
    }
}