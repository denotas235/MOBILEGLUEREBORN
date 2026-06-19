package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.ElytraPredictor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into Player.tick() to activate ElytraPredictor when the player is flying.
 * ElytraPredictor pre-loads chunks in the direction of travel to prevent
 * white chunk stutters during high-speed elytra flight.
 *
 * require=0: Player.tick() is stable but guarded.
 */
@Mixin(value = Player.class, remap = true)
public abstract class ElytraMixin {

    @Inject(
        method = "tick",
        at = @At("TAIL"),
        require = 0
    )
    private void maliworld$onPlayerTick(CallbackInfo ci) {
        if (!MaliWorldConfig.ELYTRA_PREDICTOR) return;

        Player self = (Player)(Object)this;
        if (!self.isFallFlying()) {
            ElytraPredictor.onStopFlying(self.getUUID());
            return;
        }

        // Only run on server side
        if (self.level() instanceof ServerLevel serverLevel) {
            ElytraPredictor.tick(self, serverLevel);
        }
    }
}