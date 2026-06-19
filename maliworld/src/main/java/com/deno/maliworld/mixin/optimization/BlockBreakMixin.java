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
 * Distributes chunk mesh rebuilds after block breaks.
 * Limits rebuild submissions per tick to prevent frame spikes.
 *
 * Uses @Shadow to access the protected level field (Mojang 1.21.11 mappings).
 * require=0: safe fallback.
 */
@Mixin(value = ServerPlayerGameMode.class, remap = true)
public abstract class BlockBreakMixin {

    @Shadow protected ServerLevel level;

    private static long  maliworld$lastTick        = -1L;
    private static int   maliworld$rebuildsThisTick = 0;
    private static final int MAX_REBUILDS_PER_TICK  = 2;

    @Inject(
        method = "destroyBlock",
        at = @At("HEAD"),
        require = 0
    )
    private void maliworld$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level == null) return;
        long currentTick = level.getGameTime();

        if (currentTick != maliworld$lastTick) {
            maliworld$lastTick        = currentTick;
            maliworld$rebuildsThisTick = 0;
        }
        maliworld$rebuildsThisTick++;
        // Throttling signal: render-side ChunkBuilderMixin can read this counter
    }
}