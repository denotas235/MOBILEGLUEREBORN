package com.deno.maliworld.mixin.structure;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Hooks into JigsawStructure.findGenerationPoint() (replaces removed VillageStructure in 1.21.11).
 * All Jigsaw-based structures (village, pillager outpost, bastions) share this base.
 * We observe placement but do not override — terrain adaptation is handled via events.
 *
 * require=0: JigsawStructure is stable in 1.21.11.
 */
@Mixin(value = JigsawStructure.class, remap = true)
public abstract class VillageStructureMixin {

    @Inject(
        method = "findGenerationPoint",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$afterJigsawGeneration(
            Structure.GenerationContext context,
            CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {

        if (!MaliWorldConfig.REALISTIC_VILLAGES) return;
        // Jigsaw structure placed — MaliWorld terrain adaptation via world events.
    }
}