package com.deno.maliworld.mixin.worldgen;

import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook post-buildSurface para futuras decorações contextuais de superfície.
 * A lógica real de SurfaceDecorator está planeada para expansão futura.
 * require=0: fallback gracioso se a assinatura mudar.
 */
@Mixin(value = SurfaceSystem.class, remap = true)
public abstract class SurfaceBuilderMixin {

    @Inject(method = "buildSurface", at = @At("RETURN"), require = 0)
    private void maliworld$afterBuildSurface(CallbackInfo ci) {
    }
}