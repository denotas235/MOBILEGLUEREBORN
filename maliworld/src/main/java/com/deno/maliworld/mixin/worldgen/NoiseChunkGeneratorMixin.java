package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

/**
 * Hook de telemetria no gerador de noise para diagnósticos.
 * A lógica de terrain melhorado vai ser adicionada aqui futuramente.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

    @Inject(method = "applyCarvers", at = @At("HEAD"))
    private void onApplyCarvers(CallbackInfo ci) {
    }
}
