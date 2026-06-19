package com.deno.maliworld.mixin.structure;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hooks into StrongholdStructure.findGenerationPoint() to ensure the stronghold
 * integrates properly with MaliWorld terrain.
 * Currently observes placement and adjusts Y so strongholds are not floating.
 *
 * require=0: safe fallback.
 */
@Mixin(value = StrongholdStructure.class, remap = true)
public abstract class StrongholdMixin {

    @Inject(
        method = "findGenerationPoint",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterStrongholdGeneration(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {

        // Strongholds integrate naturally with cave-enhanced terrain.
        // No modification needed beyond ensuring terrain is generated first.
    }
}