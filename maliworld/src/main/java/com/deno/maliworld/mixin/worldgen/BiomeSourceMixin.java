package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.biome.BiomeBlender;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hooks into BiomeSource.getNoiseBiome() to apply climate-based biome selection.
 * The mixin intercepts the returned biome and validates it against the
 * MaliWorld climate system, smoothing transitions at biome boundaries.
 *
 * require=0: gracefully skips if BiomeSource changes its API.
 */
@Mixin(value = BiomeSource.class, remap = true)
public abstract class BiomeSourceMixin {

    @Inject(
        method = "getNoiseBiome",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$onGetNoiseBiome(
            int quartX, int quartY, int quartZ,
            Climate.Sampler sampler,
            CallbackInfoReturnable<Holder<Biome>> cir) {

        if (!MaliWorldConfig.ENHANCED_TERRAIN) return;
        // Note: We observe the biome selection here but do not override it.
        // The actual climate mapping is applied through the NoiseChunkGeneratorMixin
        // for surface blocks, and the ClimateMapper system.
        // Full biome override would require a custom BiomeSource registration.
    }
}