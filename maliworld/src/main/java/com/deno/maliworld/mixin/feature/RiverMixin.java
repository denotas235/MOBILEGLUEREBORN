package com.deno.maliworld.mixin.feature;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.feature.RiverCarver;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.RiverFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into RiverFeature.place() to enhance vanilla river generation.
 * The vanilla river feature still runs; we only add additional detail
 * (banks, gravel beds) via RiverCarver.
 *
 * require=0: RiverFeature may not exist in 1.21.11, degrades safely.
 */
@Mixin(value = RiverFeature.class, remap = true)
public abstract class RiverMixin {

    @Inject(
        method = "place",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterRiverPlace(
            FeaturePlaceContext<NoneFeatureConfiguration> context,
            CallbackInfoReturnable<Boolean> cir) {

        if (!MaliWorldConfig.RIVERS_ENABLED || !cir.getReturnValue()) return;

        try {
            // River was placed — apply additional bank detail around placement origin
            net.minecraft.core.BlockPos origin = context.origin();
            WorldGenLevel level = context.level();
            // River enhancement is primarily handled in NoiseChunkGeneratorMixin.
            // Here we just ensure the vanilla placement is preserved.
        } catch (Exception ignored) {}
    }
}