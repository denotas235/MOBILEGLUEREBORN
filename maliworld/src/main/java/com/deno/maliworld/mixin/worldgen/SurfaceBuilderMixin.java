package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import com.deno.maliworld.worldgen.surface.SurfaceDecorator;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into SurfaceSystem.buildSurface() to apply MaliWorld contextual surfaces.
 * Runs AFTER vanilla surface is applied so we enhance rather than replace.
 *
 * Target: net.minecraft.world.level.levelgen.SurfaceSystem
 * Method: buildSurface(...) — multiple signatures exist, require=0 handles this safely.
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
                    int worldX = (chunkX << 4) + lx;
                    int worldZ = (chunkZ << 4) + lz;

                    // Find surface Y via heightmap
                    int surfaceY = chunk.getHeight(
                        net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                        lx, lz
                    );
                    if (surfaceY <= 0) continue;

                    float temp     = ClimateMapper.getTemperature(worldX, worldZ, surfaceY);
                    float humidity = ClimateMapper.getHumidity(worldX, worldZ);
                    double slope   = computeLocalSlope(chunk, lx, lz);

                    SurfaceDecorator.decorate(chunk, worldX, worldZ, surfaceY, slope, temp, humidity);
                }
            }
        } catch (Exception ignored) {
            // Never crash
        }
    }

    private static double computeLocalSlope(ChunkAccess chunk, int lx, int lz) {
        int h0 = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, lx, lz);
        int hx = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                                  Math.min(15, lx+1), lz);
        int hz = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                                  lx, Math.min(15, lz+1));
        double dx = Math.abs(hx - h0);
        double dz = Math.abs(hz - h0);
        return Math.min(1.0, Math.max(dx, dz) * 0.25);
    }
}