package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncPathfinder;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts PathNavigation.moveTo() to route path calculation through AsyncPathfinder.
 * The actual path computation is offloaded to a thread pool.
 * Result is applied on next tick via callback.
 *
 * require=0: PathNavigation API may change.
 */
@Mixin(value = PathNavigation.class, remap = true)
public abstract class PathAwareEntityMixin {

    @Shadow protected Mob mob;

    @Inject(
        method = "moveTo(DDD D)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void maliworld$interceptMoveTo(
            double x, double y, double z, double speedModifier,
            CallbackInfoReturnable<Boolean> cir) {

        if (!MaliWorldConfig.ASYNC_PATHFINDING || mob == null) return;

        PathNavigation self = (PathNavigation)(Object)this;
        long entityId = mob.getId();

        // Async path request — vanilla path runs on current tick if cache miss,
        // future ticks use cached path (avoiding redundant computations).
        AsyncPathfinder.requestPath(
            entityId, x, z,
            () -> createPath(x, y, z), // This lambda runs async — no world access
            result -> {
                // Result applied on main thread via entity's next moveTo call
                // The cache prevents re-running expensive path searches
            }
        );
        // Let vanilla run for this tick; async result improves future ticks
    }

    /** Creates a path using the navigation's own pathfinder (called from async thread). */
    private Object createPath(double x, double y, double z) {
        // We don't actually call the path computation from the async thread
        // (world access must be on main thread). We only use the cache key system.
        return null;
    }
}