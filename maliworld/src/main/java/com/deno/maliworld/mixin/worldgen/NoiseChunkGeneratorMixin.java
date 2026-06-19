package com.deno.maliworld.mixin.worldgen;

import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

    @Inject(method = "applyCarvers", at = @At("HEAD"), require = 0)
    private void onApplyCarvers(CallbackInfo ci) {
    }
}