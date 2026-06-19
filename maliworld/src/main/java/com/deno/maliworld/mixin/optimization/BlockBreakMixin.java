package com.deno.maliworld.mixin.optimization;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tracks block break count per tick to throttle chunk mesh rebuilds.
 * Accesses level via @Shadow (protected field in ServerPlayerGameMode).
 * require=0: safe fallback.
 */
@Mixin(value = ServerPlayerGameMode.class, remap = true)
public abstract class BlockBreakMixin {

    @Shadow protected ServerLevel level;

    private long  maliworld$lastTick        = -1L;
    private int   maliworld$rebuildsThisTick = 0;

    @Inject(
        method = "destroyBlock",
        at = @At("HEAD"),
        require = 0
    )
    private void maliworld$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (this.level == null) return;
        long tick = this.level.getGameTime();
        if (tick != maliworld$lastTick) {
            maliworld$lastTick        = tick;
            maliworld$rebuildsThisTick = 0;
        }
        maliworld$rebuildsThisTick++;
    }
}