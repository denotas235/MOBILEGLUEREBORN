package com.deno.maliworld.mixin.mesh;

import com.deno.maliworld.config.MaliWorldConfig;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.callback.CallbackInfo;

/**
 * Hook no dispatcher de seções para telemetria / futura integração do GreedyMesher.
 */
@Mixin(SectionRenderDispatcher.class)
public abstract class ChunkBuilderMixin {

    @Inject(method = "rebuildSectionSync", at = @At("HEAD"))
    private void onRebuildSync(CallbackInfo ci) {
    }
}
