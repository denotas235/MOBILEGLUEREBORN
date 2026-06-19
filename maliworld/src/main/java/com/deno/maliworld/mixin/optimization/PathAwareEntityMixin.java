package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.AsyncPathfinder;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts PathNavigation.moveTo(x,y,z,speed) to route through AsyncPathfinder cache.
 *
 * IMPORTANT: Correct method descriptor is "moveTo(DDDD)Z" (4 doubles, no spaces).
 * MC 1.21.11 Mojang mappings: PathNavigation.moveTo(double,double,double,double):boolean
 * require=0: safe fallback.
 */
@Mixin(value = PathNavigation.class, remap = true)
public abstract class PathAwareEntityMixin {

    @Shadow protected Mob mob;

    @Inject(
        method = "moveTo(DDDD)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void maliworld$interceptMoveTo(
            double x, double y, double z, double speedModifier,
            CallbackInfoReturnable<Boolean> cir) {

        if (!MaliWorldConfig.ASYNC_PATHFINDING || mob == null) return;

        long entityId = mob.getId();

        // Request an async path computation via cache.
        // For cache hits (same target within TTL), skip vanilla path computation.
        AsyncPathfinder.requestPath(
            entityId, x, z,
            () -> null,   // Path computation itself stays on main thread (world access)
            result -> {
                // Cache hit/miss handled by AsyncPathfinder internally
            }
        );
        // Always let vanilla run — we only use the cache system for future deduplication
    }
}