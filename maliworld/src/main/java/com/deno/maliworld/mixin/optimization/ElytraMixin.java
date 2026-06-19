package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.ElytraPredictor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hook no tick do jogador para pré-carregar chunks no caminho da elytra.
 */
@Mixin(Player.class)
public abstract class ElytraMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        if (!MaliWorldConfig.ELYTRA_PREDICTOR) return;

        Player self = (Player)(Object) this;
        if (self.level().isClientSide()) return;
        if (!self.isFallFlying()) return;

        ElytraPredictor.tick(self);
    }
}
