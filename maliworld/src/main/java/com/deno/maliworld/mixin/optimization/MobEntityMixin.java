package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttle de tick de mobs por distancia do jogador.
 * < 32 blocos: tick normal
 * 32-64 blocos: tick a cada 2
 * > 64 blocos:  tick a cada 4
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Unique private int mw_skipCount = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void mw_throttle(CallbackInfo ci) {
        if (!MaliWorldConfig.MOB_TICK_THROTTLE) return;
        try {
            Mob self = (Mob)(Object)this;
            if (!(self.level() instanceof ServerLevel level)) return;

            Player nearest = level.getNearestPlayer(self, MaliWorldConfig.MOB_FAR_DISTANCE * 2.0);
            if (nearest == null) {
                // Sem jogador perto: tick a cada 4
                if ((mw_skipCount++ & 3) != 0) { ci.cancel(); return; }
                return;
            }

            double distSq = self.distanceToSqr(nearest);
            int farSq  = MaliWorldConfig.MOB_FAR_DISTANCE  * MaliWorldConfig.MOB_FAR_DISTANCE;
            int nearSq = MaliWorldConfig.MOB_NEAR_DISTANCE * MaliWorldConfig.MOB_NEAR_DISTANCE;

            if (distSq > farSq) {
                if ((mw_skipCount++ & 3) != 0) { ci.cancel(); }
            } else if (distSq > nearSq) {
                if ((mw_skipCount++ & 1) != 0) { ci.cancel(); }
            }
        } catch (Throwable t) { /* never crash */ }
    }
}