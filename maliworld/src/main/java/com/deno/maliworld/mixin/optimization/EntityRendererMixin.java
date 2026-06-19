package com.deno.maliworld.mixin.optimization;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void onShouldRender(T entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player) return;

        double dx = entity.getX() - x;
        double dy = entity.getY() - y;
        double dz = entity.getZ() - z;
        double distSq = dx * dx + dy * dy + dz * dz;

        if (entity instanceof ItemEntity && distSq > 24.0 * 24.0) {
            cir.setReturnValue(false);
            return;
        }
        if (entity instanceof LivingEntity && !(entity instanceof Player) && distSq > 48.0 * 48.0) {
            cir.setReturnValue(false);
        }
    }
}