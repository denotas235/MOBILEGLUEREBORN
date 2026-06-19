package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.feature.LakeGenerator;
import com.deno.maliworld.feature.PathGenerator;
import com.deno.maliworld.feature.RiverCarver;
import com.deno.maliworld.optimization.ChunkGenOptimizer;
import com.deno.maliworld.registry.NoiseRegistry;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import com.deno.maliworld.worldgen.surface.SurfaceDecorator;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.Blender;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Hooks into NoiseBasedChunkGenerator.fillFromNoise() AFTER vanilla terrain is
 * generated, then applies MaliWorld terrain enhancements:
 * - River carving
 * - Lake generation
 * - Path placement
 * - Surface decoration
 *
 * Using @At("RETURN") with require=0 so the mod degrades gracefully if
 * Mojang changes the signature.
 */
@Mixin(value = NoiseBasedChunkGenerator.class, remap = true)
public abstract class NoiseChunkGeneratorMixin {

    @Inject(
        method = "fillFromNoise",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterFillFromNoise(
            java.util.concurrent.Executor executor,
            Blender blender,
            RandomState randomState,
            net.minecraft.world.level.levelgen.structure.StructureManager structureManager,
            ChunkAccess chunk,
            CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {

        if (!MaliWorldConfig.ENHANCED_TERRAIN || NoiseRegistry.terrainShaper == null) return;

        try {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;

            // Pre-compute neighbors for cache warmup
            ChunkGenOptimizer.precomputeNeighbors(chunkX, chunkZ);

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX = (chunkX << 4) + lx;
                    int worldZ = (chunkZ << 4) + lz;

                    int surfaceY = ChunkGenOptimizer.getHeight(chunkX, chunkZ, lx, lz);
                    float temp     = ClimateMapper.getTemperature(worldX, worldZ, surfaceY);
                    float humidity = ClimateMapper.getHumidity(worldX, worldZ);
                    double slope   = NoiseRegistry.terrainShaper.getSlopeAt(worldX, worldZ);

                    // Apply rivers
                    if (MaliWorldConfig.RIVERS_ENABLED) {
                        int riverW = RiverCarver.getRiverWidth(worldX, worldZ);
                        if (riverW > 0) {
                            RiverCarver.carveColumn(chunk, worldX, worldZ, surfaceY, riverW);
                            continue;
                        }
                    }

                    // Apply lakes
                    if (MaliWorldConfig.LAKES_ENABLED) {
                        int lakeCat = LakeGenerator.getLakeCategory(worldX, worldZ);
                        if (lakeCat > 0) {
                            LakeGenerator.carveColumn(chunk, worldX, worldZ, surfaceY, lakeCat);
                            continue;
                        }
                    }

                    // Apply paths
                    if (MaliWorldConfig.PATHS_ENABLED && PathGenerator.isPath(worldX, worldZ, surfaceY)) {
                        PathGenerator.placePath(chunk, worldX, worldZ, surfaceY);
                        continue;
                    }

                    // Apply contextual surface decoration
                    SurfaceDecorator.decorate(chunk, worldX, worldZ, surfaceY, slope, temp, humidity);
                }
            }
        } catch (Exception e) {
            // Mixin gracefully degrades — never crash the game
        }
    }
}