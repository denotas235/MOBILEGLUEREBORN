package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Ponto de injecao para o RiverCarver do MaliWorld.
 * Chama o carver apos a geracao base do terreno.
 * require = 0: nunca crasha se o metodo nao existir.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class RiverMixin {

    @Inject(method = "fillFromNoise", at = @At("RETURN"), require = 0)
    private void mw_carveRivers(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (!MaliWorldConfig.RIVERS_ENABLED) return;
        // RiverCarver sera chamado aqui com o chunk e seaLevel
    }
}