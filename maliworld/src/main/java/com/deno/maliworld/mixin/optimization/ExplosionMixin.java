package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Limita o raio de explosão a máximo 6 blocos.
 * MC 1.21.11 Mojang mappings: net.minecraft.world.level.Explosion
 *
 * Usa @Mutable + @Shadow para remover o final do campo radius em bytecode,
 * permitindo modificação antes de explode() calcular os blocos afectados.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @Mutable
    @Shadow @Final private float radius;

    @Inject(method = "explode", at = @At("HEAD"))
    private void maliworld$limitRadius(CallbackInfo ci) {
        if (!MaliWorldConfig.LIMIT_EXPLOSIONS) return;
        if (this.radius > 6.0f) {
            this.radius = 6.0f;
        }
    }
}
