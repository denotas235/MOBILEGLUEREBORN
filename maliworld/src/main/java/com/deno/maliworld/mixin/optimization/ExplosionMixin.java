package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Limita o raio de explosão a máximo 6 blocos.
 * MC 1.21.11: Explosion é um record imutável. Interceptamos o argumento float radius
 * no método Level.explode() de 9 argumentos antes que o record seja criado.
 *
 * require=0: fallback gracioso se a assinatura mudar.
 */
@Mixin(Level.class)
public abstract class ExplosionMixin {

    @ModifyArg(
        method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V",
        at = @At("HEAD"),
        index = 6,
        require = 0
    )
    private float maliworld$limitExplosionRadius(float radius) {
        if (!MaliWorldConfig.LIMIT_EXPLOSIONS) return radius;
        return Math.min(radius, 6.0f);
    }
}
