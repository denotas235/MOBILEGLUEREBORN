package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * River enhancement mixin (MC 1.21.11).
 * RiverFeature was removed in 1.18+; rivers are now biome-level topology.
 * MaliWorld river carving happens in NoiseChunkGeneratorMixin instead.
 * This class is kept for future expansion; injection is a no-op.
 *
 * Targets ReplaceBlockFeature as a safe, always-present hook.
 * require=0: degrades gracefully.
 */
@Mixin(value = ReplaceBlockFeature.class, remap = true)
public abstract class RiverMixin {

    @Inject(
        method = "place",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$onReplaceBlock(
            FeaturePlaceContext<ReplaceBlockConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {
        // River carving is done in NoiseChunkGeneratorMixin — nothing here.
    }
}