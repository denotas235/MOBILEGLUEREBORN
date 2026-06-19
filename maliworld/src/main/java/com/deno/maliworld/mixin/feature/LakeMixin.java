package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lake enhancement mixin (MC 1.21.11).
 * LakeFeature was removed in 1.18+; lakes are now biome terrain topology.
 * MaliWorld lake carving happens in NoiseChunkGeneratorMixin instead.
 * This class is kept as a no-op placeholder for API compatibility.
 *
 * Targets SpringFeature (water/lava springs still exist in 1.21.11).
 * require=0: degrades gracefully.
 */
@Mixin(value = SpringFeature.class, remap = true)
public abstract class LakeMixin {

    @Inject(
        method = "place",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$onSpringPlace(
            FeaturePlaceContext<SpringConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {
        // Lake carving is done in NoiseChunkGeneratorMixin — nothing here.
    }
}