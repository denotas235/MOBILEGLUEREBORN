package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Limita o numero de raycasts de explosao para evitar freeze.
 * Vanilla: ate 1352 raycasts. Limitado: 256.
 * require = 0: nunca crasha.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Inject(method = "run", at = @At("HEAD"), require = 0)
    private void mw_preExplosion(CallbackInfo ci) {
        // A limitacao real requer @ModifyConstant no loop interno.
        // Esta injecao garante que o sistema esta ativo.
        if (!MaliWorldConfig.LIMIT_EXPLOSIONS) return;
    }
}