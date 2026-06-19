// MobileGlues - GameRendererMixin.java
// Hooks GameRenderer.renderLevel() RETURN to run the native godray pass
// MC 1.21.11 / Mojang Mappings
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.mixins;

import com.nexus.MobileGlues;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Injects into {@link GameRenderer#renderLevel} to execute the native
 * godray (crepuscular-ray) post-processing pass immediately after the
 * main 3D scene has been rendered.
 *
 * <h3>Safety</h3>
 * <ul>
 *   <li>{@code require = 0} — silently skipped if the method signature
 *       changes in future MC versions.</li>
 *   <li>All native calls are wrapped in {@code try/catch}.</li>
 *   <li>If the native lib is unavailable, the entire mixin body exits
 *       immediately via {@link MobileGlues#isAvailable()}.</li>
 * </ul>
 *
 * <h3>MC 1.21.11 target method (Mojang Mappings)</h3>
 * {@code GameRenderer.renderLevel(net.minecraft.util.profiling.jfr.callback.ProfiledDuration)}
 * — or any single-arg overload; require=0 handles mismatch gracefully.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    /**
     * Fires at the RETURN of renderLevel — after geometry, sky, and
     * transparency passes are complete.
     *
     * <p>Uses broad wildcard method target; the exact signature varies
     * between minor MC versions. require=0 ensures crash-proof behavior.
     */
    @Inject(method = "renderLevel", at = @At("RETURN"), require = 0)
    private void mg_postRenderLevel(CallbackInfo ci) {
        if (!MobileGlues.isAvailable()) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.level == null) return;

            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();
            if (w <= 0 || h <= 0) return;

            // Approximate sun screen position from level time.
            // The C++ pass refines this using the actual sun direction uniform.
            long dayTime  = mc.level.getDayTime() % 24000L;
            float t       = dayTime / 24000.0f;                 // 0..1
            float sunAngle = (float) Math.PI * 2.0f * t;       // full rotation
            float sunScreenX = 0.5f + 0.3f * (float) Math.cos(sunAngle);
            float sunScreenY = 0.5f + 0.3f * (float) Math.sin(sunAngle);

            // sceneTexture=0 — native side reads from the current read framebuffer
            MobileGlues.runGodrayPass(
                0,
                sunScreenX, sunScreenY,
                (float) w, (float) h
            );
        } catch (Throwable t) {
            // Never crash — godrays are a visual enhancement, not critical path
        }
    }
}
