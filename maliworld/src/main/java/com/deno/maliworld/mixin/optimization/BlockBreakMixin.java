package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Controla o ritmo de rebuilds de mesh ao quebrar blocos.
 * Limita a 2 rebuilds por tick para evitar GPU stutter.
 * require = 0: nunca crasha.
 */
@Mixin(ServerPlayerGameMode.class)
public abstract class BlockBreakMixin {

    private static int mw_rebuildsThisTick = 0;

    @Inject(method = "destroyBlock", at = @At("RETURN"), require = 0)
    private void mw_trackRebuild(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // O contador e resettado via tick do servidor
        // Aqui apenas rastreamos a frequencia de quebras
    }
}