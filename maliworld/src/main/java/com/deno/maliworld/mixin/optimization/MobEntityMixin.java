package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttle de tick de mobs distantes para reduzir carga do servidor.
 * require=0: fallback gracioso se a assinatura do Mob.tick() mudar.
 */
@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
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
