package com.deno.maliworld.mixin.mesh;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class ChunkBuilderMixin {

    @Inject(method = "rebuildSectionSync", at = @At("HEAD"), require = 0)
    private void onRebuildSync(CallbackInfo ci) {
    }
}