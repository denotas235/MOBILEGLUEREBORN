// MobileGlues - SectionCompilerMixin.java
// Hooks SectionCompiler.compile() for greedy-mesh tracking and statistics
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.mixins;

import com.nexus.MobileGlues;
import com.nexus.optimization.GreedyMesher;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hooks into {@link SectionCompiler#compile} to record per-section timestamps
 * for the {@link GreedyMesher} and collect compile throughput statistics.
 */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {

    private static final AtomicLong COMPILATIONS = new AtomicLong();
    private static final AtomicLong TIME_NS       = new AtomicLong();

    @Inject(method = "compile", at = @At("HEAD"), require = 0)
    private void mg_compileHead(
            SectionPos pos,
            RenderSectionRegion region,
            VertexSorting sorting,
            SectionBufferBuilderPack pack,
            CallbackInfoReturnable<SectionCompiler.Results> cir) {
        GreedyMesher.notifyCompileStart(pos);
    }

    @Inject(method = "compile", at = @At("RETURN"), require = 0)
    private void mg_compileReturn(
            SectionPos pos,
            RenderSectionRegion region,
            VertexSorting sorting,
            SectionBufferBuilderPack pack,
            CallbackInfoReturnable<SectionCompiler.Results> cir) {
        double ms = GreedyMesher.elapsedMs(pos);
        if (ms >= 0) TIME_NS.addAndGet((long)(ms * 1_000_000));
        long n = COMPILATIONS.incrementAndGet();
        if (n % 500 == 0) {
            double avg = (TIME_NS.get() / 1_000_000.0) / n;
            MobileGlues.LOGGER.info(
                "[MG-Mesh] {} compilations | avg {:.2f} ms | total quads {} merged {}",
                n, avg, GreedyMesher.getTotalQuads(), GreedyMesher.getMergedQuads());
        }
    }
}
