package com.deno.maliworld.mixin.mesh;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.optimization.GreedyMesher;
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
 * Rastreia compilacoes de secao e aciona o GreedyMesher para estatisticas.
 * require = 0: nunca crasha se a assinatura mudar.
 */
@Mixin(SectionCompiler.class)
public abstract class ChunkBuilderMixin {

    private static final AtomicLong COMPILATIONS = new AtomicLong();
    private static final AtomicLong TIME_NS       = new AtomicLong();

    @Inject(method = "compile", at = @At("HEAD"), require = 0)
    private void mw_compileHead(SectionPos pos, RenderSectionRegion region,
                                  VertexSorting sorting, SectionBufferBuilderPack pack,
                                  CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (!MaliWorldConfig.GREEDY_MESHING) return;
        try { GreedyMesher.notifyCompileStart(pos); } catch (Throwable t) {}
    }

    @Inject(method = "compile", at = @At("RETURN"), require = 0)
    private void mw_compileReturn(SectionPos pos, RenderSectionRegion region,
                                    VertexSorting sorting, SectionBufferBuilderPack pack,
                                    CallbackInfoReturnable<SectionCompiler.Results> cir) {
        if (!MaliWorldConfig.GREEDY_MESHING) return;
        try {
            double ms = GreedyMesher.elapsedMs(pos);
            if (ms >= 0) TIME_NS.addAndGet((long)(ms * 1_000_000));
            long n = COMPILATIONS.incrementAndGet();
            if (n % 500 == 0) {
                double avg = (TIME_NS.get() / 1_000_000.0) / n;
                MaliWorldMod.LOGGER.info("[MW-Mesh] {} compilacoes | avg {:.2f}ms | quads={}/{}",
                    n, avg, GreedyMesher.getMergedQuads(), GreedyMesher.getTotalQuads());
            }
        } catch (Throwable t) {}
    }
}