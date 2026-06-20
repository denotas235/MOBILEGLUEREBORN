package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncPathfinder;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Rastreia requisicoes de pathfinding para o AsyncPathfinder.
 * require = 0: nunca crasha.
 */
@Mixin(PathfinderMob.class)
public abstract class PathAwareEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void mw_trackPathfinding(CallbackInfo ci) {
        if (!MaliWorldConfig.ASYNC_PATHFINDING) return;
        // O cache de pathfinding e verificado/actualizado pelo AsyncPathfinder
    }
}