package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Culling de entidades distantes para reduzir draw calls GPU.
 * Tabela de corte:
 *   Mob > 48 blocos    → nao renderiza
 *   Item > 24 blocos   → nao renderiza
 * require = 0: nunca crasha.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void mw_cull(T entity, Frustum frustum, double camX, double camY, double camZ,
                          CallbackInfoReturnable<Boolean> cir) {
        if (!MaliWorldConfig.ENTITY_CULLING) return;
        try {
            double dx = entity.getX() - camX;
            double dy = entity.getY() - camY;
            double dz = entity.getZ() - camZ;
            double distSq = dx*dx + dy*dy + dz*dz;

            if (entity instanceof Mob && distSq > 48.0 * 48.0) {
                cir.setReturnValue(false);
            } else if (entity instanceof ItemEntity && distSq > 24.0 * 24.0) {
                cir.setReturnValue(false);
            }
        } catch (Throwable t) { /* never crash */ }
    }
}