package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.feature.LakeGenerator;
import com.deno.maliworld.feature.PathGenerator;
import com.deno.maliworld.feature.RiverCarver;
import com.deno.maliworld.optimization.ChunkGenOptimizer;
import com.deno.maliworld.registry.NoiseRegistry;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import com.deno.maliworld.worldgen.surface.SurfaceDecorator;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Hooks into NoiseBasedChunkGenerator.fillFromNoise() AFTER vanilla terrain,
 * applying MaliWorld enhancements (rivers, lakes, paths, surface decoration).
 * MC 1.21.11 Mojang mappings:
 *   NoiseBasedChunkGenerator → net.minecraft.world.level.levelgen
 *   Blender → net.minecraft.world.level.levelgen.blending
 *   StructureManager → net.minecraft.world.level
 */
@Mixin(value = NoiseBasedChunkGenerator.class, remap = true)
public abstract class NoiseChunkGeneratorMixin {

    @Inject(
        method = "fillFromNoise",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterFillFromNoise(
            Executor executor,
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk,
            CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {

        if (!MaliWorldConfig.ENHANCED_TERRAIN || NoiseRegistry.terrainShaper == null) return;

        try {
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            ChunkGenOptimizer.precomputeNeighbors(chunkX, chunkZ);

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int worldX  = (chunkX << 4) + lx;
                    int worldZ  = (chunkZ << 4) + lz;
                    int surfaceY = ChunkGenOptimizer.getHeight(chunkX, chunkZ, lx, lz);
                    float temp     = ClimateMapper.getTemperature(worldX, worldZ, surfaceY);
                    float humidity = ClimateMapper.getHumidity(worldX, worldZ);
                    double slope   = NoiseRegistry.terrainShaper.getSlopeAt(worldX, worldZ);

                    if (MaliWorldConfig.RIVERS_ENABLED) {
                        int rw = RiverCarver.getRiverWidth(worldX, worldZ);
                        if (rw > 0) { RiverCarver.carveColumn(chunk, worldX, worldZ, surfaceY, rw); continue; }
                    }
                    if (MaliWorldConfig.LAKES_ENABLED) {
                        int lc = LakeGenerator.getLakeCategory(worldX, worldZ);
                        if (lc > 0) { LakeGenerator.carveColumn(chunk, worldX, worldZ, surfaceY, lc); continue; }
                    }
                    if (MaliWorldConfig.PATHS_ENABLED && PathGenerator.isPath(worldX, worldZ, surfaceY)) {
                        PathGenerator.placePath(chunk, worldX, worldZ, surfaceY); continue;
                    }
                    SurfaceDecorator.decorate(chunk, worldX, worldZ, surfaceY, slope, temp, humidity);
                }
            }
        } catch (Exception ignored) {}
    }
}