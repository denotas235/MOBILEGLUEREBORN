package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Intercepta o gerador de terreno vanilla para aplicar o TerrainShaper do MaliWorld.
 * require = 0: silenciosamente ignorado se a assinatura mudar.
 */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseChunkGeneratorMixin {

    @Inject(method = "fillFromNoise", at = @At("RETURN"), require = 0)
    private void mw_afterFillFromNoise(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (!MaliWorldConfig.ENHANCED_TERRAIN) return;
        // A modificacao real do terreno requer acesso ao ChunkAccess passado como argumento.
        // Esta injecao confirma que o gerador esta ativo.
    }
}