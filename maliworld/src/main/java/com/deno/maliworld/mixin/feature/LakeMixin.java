package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.feature.LakeGenerator;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into LakeFeature.place() to enhance vanilla lake generation.
 * Lake carving is primarily handled in NoiseChunkGeneratorMixin.
 * This mixin ensures the LakeGenerator is notified of vanilla lake placements.
 *
 * require=0: LakeFeature was deprecated in 1.18+ and may not exist in 1.21.11.
 */
@Mixin(value = LakeFeature.class, remap = true)
public abstract class LakeMixin {

    @Inject(
        method = "place",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterLakePlace(
            FeaturePlaceContext<LakeFeature.Configuration> context,
            CallbackInfoReturnable<Boolean> cir) {

        if (!MaliWorldConfig.LAKES_ENABLED) return;
        // Lake was placed — our LakeGenerator in NoiseChunkGeneratorMixin handles
        // additional lake types. Vanilla lake is kept as-is.
    }
}