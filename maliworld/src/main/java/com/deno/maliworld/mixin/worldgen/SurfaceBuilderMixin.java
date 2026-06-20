package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepta a fase de construcao de superficie para aplicar o SurfaceDecorator.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SurfaceBuilderMixin {

    @Inject(method = "buildSurface", at = @At("RETURN"), require = 0)
    private void mw_afterBuildSurface(CallbackInfo ci) {
        if (!MaliWorldConfig.ENHANCED_TERRAIN) return;
        // SurfaceDecorator.decorate(...) sera chamado aqui quando tivermos
        // acesso aos argumentos via @Shadow ou @Inject com args
    }
}