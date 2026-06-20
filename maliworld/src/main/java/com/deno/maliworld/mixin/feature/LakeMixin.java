package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Ponto de injecao para o LakeGenerator do MaliWorld.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class LakeMixin {

    @Inject(method = "fillFromNoise", at = @At("RETURN"), require = 0)
    private void mw_generateLakes(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (!MaliWorldConfig.LAKES_ENABLED) return;
        // LakeGenerator sera chamado aqui
    }
}