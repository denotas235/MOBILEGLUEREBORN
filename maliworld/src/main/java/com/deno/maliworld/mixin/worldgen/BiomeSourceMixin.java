package com.deno.maliworld.mixin.worldgen;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepta a selecao de bioma para aplicar o ClimateMapper do MaliWorld.
 * require = 0: silenciosamente ignorado se a assinatura mudar.
 */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class BiomeSourceMixin {

    @Inject(method = "getNoiseBiome", at = @At("HEAD"), require = 0)
    private void mw_beforeGetBiome(CallbackInfoReturnable<?> cir) {
        // Placeholder: modificar retorno requer @ModifyReturnValue ou acesso ao BiomeResolver
    }
}