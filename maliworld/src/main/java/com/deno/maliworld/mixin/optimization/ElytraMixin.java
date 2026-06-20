package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.ElytraPredictor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Detecta voo de elytra e aciona o ElytraPredictor para
 * pre-carregar chunks na direcao do voo.
 * require = 0: nunca crasha.
 */
@Mixin(Player.class)
public abstract class ElytraMixin {

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void mw_elytraPredict(CallbackInfo ci) {
        if (!MaliWorldConfig.ELYTRA_PREDICTOR) return;
        try {
            Player self = (Player)(Object)this;
            if (self.isFallFlying()) {
                ElytraPredictor.tick(self);
            }
        } catch (Throwable t) { /* never crash */ }
    }
}