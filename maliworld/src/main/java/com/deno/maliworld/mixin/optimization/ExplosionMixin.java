package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Limita o número de blocos afetados por explosão.
 * Em 1.21.11 a classe alvo é net.minecraft.world.level.Explosion (Mojang mappings).
 * NÃO usar class_1927 (intermediário Fabric) — em 1.21.x esse nome é uma interface.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {

    @ModifyVariable(
        method = "explode",
        at = @At("HEAD"),
        argsOnly = false,
        index = 0
    )
    private static float limitRadius(float radius) {
        if (!MaliWorldConfig.LIMIT_EXPLOSIONS) return radius;
        return Math.min(radius, 6.0f);
    }
}
