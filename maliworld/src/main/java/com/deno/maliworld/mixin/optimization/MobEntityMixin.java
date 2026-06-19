package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Throttles mob tick rate by player distance:
 *   < MOB_NEAR_DISTANCE (32)  → normal tick every tick
 *   MOB_NEAR_DISTANCE to FAR (64) → tick every 2 ticks (50% rate)
 *   > MOB_FAR_DISTANCE (64)   → tick every 4 ticks (25% rate)
 *
 * This is a significant CPU saver: mobs out of render range
 * don't need full-speed AI updates.
 *
 * require=0: Mob.tick() is very stable but guarded for safety.
 */
@Mixin(value = Mob.class, remap = true)
public abstract class MobEntityMixin {

    @Inject(
        method = "tick",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void maliworld$throttleTick(CallbackInfo ci) {
        if (!MaliWorldConfig.MOB_TICK_THROTTLE) return;

        Mob self = (Mob)(Object)this;
        Level level = self.level();
        if (level.isClientSide()) return;

        long tick = level.getGameTime();
        double nearestPlayerDist = level.getNearestPlayer(self, -1.0) instanceof ServerPlayer sp
            ? self.distanceTo(sp) : Double.MAX_VALUE;

        if (nearestPlayerDist < MaliWorldConfig.MOB_NEAR_DISTANCE) {
            // Near: always tick — no throttle
        } else if (nearestPlayerDist < MaliWorldConfig.MOB_FAR_DISTANCE) {
            // Mid range: tick every 2
            if (tick % 2 != 0) ci.cancel();
        } else {
            // Far: tick every 4
            if (tick % 4 != 0) ci.cancel();
        }
    }
}