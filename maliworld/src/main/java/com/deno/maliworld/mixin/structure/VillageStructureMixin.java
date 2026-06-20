package com.deno.maliworld.mixin.structure;

import com.deno.maliworld.config.MaliWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

/**
 * Adapta aldeias ao terreno gerado pelo MaliWorld.
 * Usa targets string para evitar import directo de VillageStructure
 * que pode nao existir em MC 1.21.11.
 * require=0: silenciosamente ignorado se o metodo nao existir.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.VillageStructure")
public abstract class VillageStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), require = 0)
    private void mw_adaptToTerrain(CallbackInfoReturnable<Optional<?>> cir) {
        if (!MaliWorldConfig.REALISTIC_VILLAGES) return;
        // RealisticVillage.findGroundY() sera aplicado aqui numa versao futura.
    }
}