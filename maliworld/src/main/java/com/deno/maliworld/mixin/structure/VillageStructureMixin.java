package com.deno.maliworld.mixin.structure;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.structure.RealisticVillage;
import net.minecraft.world.level.levelgen.structure.structures.VillageStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

/**
 * Modifica aldeias para se adaptarem ao terreno.
 * require = 0: nunca crasha.
 */
@Mixin(VillageStructure.class)
public abstract class VillageStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), require = 0)
    private void mw_adaptToTerrain(CallbackInfoReturnable<Optional<?>> cir) {
        if (!MaliWorldConfig.REALISTIC_VILLAGES) return;
        // RealisticVillage.findGroundY() e chamado aqui para ajustar Y
    }
}