package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import com.deno.maliworld.worldgen.surface.SurfaceDecorator;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into SurfaceSystem.buildSurface() post-vanilla to apply contextual surface blocks.
 * MC 1.21.11: SurfaceSystem at net.minecraft.world.level.levelgen.SurfaceSystem.
 * require=0: graceful fallback.
 */
@Mixin(value = SurfaceSystem.class, remap = true)
public abstract class SurfaceBuilderMixin {

    @Inject(
        method = "buildSurface",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterBuildSurface(
            RandomState randomState,
            BiomeManager biomeManager,
            net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes,
            boolean legacyRandomSource,
            WorldGenerationContext worldGenerationContext,
            ChunkAccess chunk,
            NoiseChunk noiseChunk,
            SurfaceRules.RuleSource ruleSource,
            CallbackInfo ci) {

        if (!MaliWorldConfig.ENHANCED_TERRAIN) return;
        try {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX   = (chunkX << 4) + lx;
                    int worldZ   = (chunkZ << 4) + lz;
                    int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
                    if (surfaceY <= 0) continue;
                    float temp     = ClimateMapper.getTemperature(worldX, worldZ, surfaceY);
                    float humidity = ClimateMapper.getHumidity(worldX, worldZ);
                    int h0 = surfaceY;
                    int hx = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, Math.min(15, lx+1), lz);
                    int hz = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, lx, Math.min(15, lz+1));
                    double slope = Math.min(1.0, Math.max(Math.abs(hx-h0), Math.abs(hz-h0)) * 0.25);
                    SurfaceDecorator.decorate(chunk, worldX, worldZ, surfaceY, slope, temp, humidity);
                }
            }
        } catch (Exception ignored) {}
    }
}