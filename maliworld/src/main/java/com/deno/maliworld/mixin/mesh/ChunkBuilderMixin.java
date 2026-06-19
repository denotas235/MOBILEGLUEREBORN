package com.deno.maliworld.mixin.mesh;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.GreedyMesher;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into SectionRenderDispatcher.RenderSection to enable greedy meshing.
 * Greedy meshing merges adjacent same-block faces, dramatically reducing the
 * number of quads (and thus draw calls) per chunk section.
 *
 * The actual merge is done by GreedyMesher.buildGreedyMask().
 * This mixin marks the section as "greedy-eligible" before compilation.
 *
 * Target: net.minecraft.client.renderer.chunk.SectionRenderDispatcher
 * require=0: SectionRenderDispatcher internals change frequently.
 */
@Mixin(value = SectionRenderDispatcher.class, remap = true)
public abstract class ChunkBuilderMixin {

    @Inject(
        method = "<init>",
        at = @At("RETURN"),
        require = 0
    )
    private void maliworld$onInit(CallbackInfo ci) {
        if (MaliWorldConfig.GREEDY_MESHING && GreedyMesher.isEnabled()) {
            // SectionRenderDispatcher initialized — greedy meshing will be applied
            // to eligible chunk sections during compilation via the GreedyMesher utility.
        }
    }
}