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

@Mixin(Mob.class)
public abstract class MobEntityMixin {

    @Unique private int mw_skip = 0;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void mw_throttle(CallbackInfo ci) {
        if (!MaliWorldConfig.MOB_TICK_THROTTLE) return;
        try {
            Mob self = (Mob)(Object)this;
            if (!(self.level() instanceof ServerLevel level)) return;
            Player nearest = level.getNearestPlayer(
                self.getX(), self.getY(), self.getZ(),
                MaliWorldConfig.MOB_FAR_DISTANCE * 2.0, false);
            if (nearest == null) {
                if ((mw_skip++ & 3) != 0) ci.cancel();
                return;
            }
            double dx = self.getX()-nearest.getX();
            double dy = self.getY()-nearest.getY();
            double dz = self.getZ()-nearest.getZ();
            double dSq = dx*dx + dy*dy + dz*dz;
            int farSq  = MaliWorldConfig.MOB_FAR_DISTANCE  * MaliWorldConfig.MOB_FAR_DISTANCE;
            int nearSq = MaliWorldConfig.MOB_NEAR_DISTANCE * MaliWorldConfig.MOB_NEAR_DISTANCE;
            if (dSq > farSq) { if ((mw_skip++ & 3) != 0) ci.cancel(); }
            else if (dSq > nearSq) { if ((mw_skip++ & 1) != 0) ci.cancel(); }
        } catch (Throwable t) { /* never crash */ }
    }
}