package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

/**
 * Skip pathfinding para entidades longe de qualquer jogador.
 */
@Mixin(PathfinderMob.class)
public abstract class PathAwareEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void onTick(CallbackInfo ci) {
    }

    @Inject(method = "customServerAiStep", at = @At("HEAD"), cancellable = true)
    private void skipFarPathfinding(CallbackInfo ci) {
        if (!MaliWorldConfig.ASYNC_PATHFINDING) return;

        PathfinderMob self = (PathfinderMob)(Object) this;
        Level level = self.level();
        if (level.isClientSide()) return;

        Player nearest = level.getNearestPlayer(self, MaliWorldConfig.MOB_FAR_DISTANCE * 1.5);
        if (nearest == null) {
            ci.cancel();
        }
    }
}
