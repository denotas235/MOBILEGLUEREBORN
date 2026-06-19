package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

/**
 * Throttle de tick de mobs por distância ao jogador mais próximo.
 * < 32 blocos → tick normal
 * 32-64 blocos → tick a cada 2
 * > 64 blocos  → tick a cada 4
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(CallbackInfo ci) {
        if (!MaliWorldConfig.MOB_TICK_THROTTLE) return;

        Mob self = (Mob)(Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        Player nearest = level.getNearestPlayer(self, MaliWorldConfig.MOB_FAR_DISTANCE);
        if (nearest == null) {
            if ((self.tickCount % 4) != 0) ci.cancel();
            return;
        }

        double dist = self.distanceTo(nearest);
        if (dist > MaliWorldConfig.MOB_FAR_DISTANCE) {
            if ((self.tickCount % 4) != 0) ci.cancel();
        } else if (dist > MaliWorldConfig.MOB_NEAR_DISTANCE) {
            if ((self.tickCount % 2) != 0) ci.cancel();
        }
    }
}
