package com.deno.maliworld.mixin.optimization;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Distance-based entity culling.
 * Stops rendering entities that are too far away, based on entity type:
 *   Mob    > ENTITY_CULL_MOB_DIST  (48)  → skip render
 *   Item   > ENTITY_CULL_ITEM_DIST (24)  → skip render
 *   XP orb > 16 blocks             → skip render
 *
 * require=0: EntityRenderer API is client-only and may shift.
 */
@Mixin(value = EntityRenderer.class, remap = true)
public abstract class EntityRendererMixin<T extends Entity> {

    @Inject(
        method = "shouldRender",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void maliworld$cullDistantEntities(
            T entity,
            net.minecraft.client.renderer.culling.Frustum frustum,
            double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir) {

        if (entity == null) return;

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distSq = dx*dx + dy*dy + dz*dz;

        int mobDist  = MaliWorldConfig.ENTITY_CULL_MOB_DIST;
        int itemDist = MaliWorldConfig.ENTITY_CULL_ITEM_DIST;

        if (entity instanceof ItemEntity && distSq > itemDist * itemDist) {
            cir.setReturnValue(false); return;
        }
        if (entity instanceof ExperienceOrb && distSq > 16.0 * 16.0) {
            cir.setReturnValue(false); return;
        }
        if ((entity instanceof Monster || entity instanceof Animal) && distSq > mobDist * mobDist) {
            cir.setReturnValue(false);
        }
    }
}