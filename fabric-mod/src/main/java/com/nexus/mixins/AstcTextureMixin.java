// MobileGlues - AstcTextureMixin.java
// Intercepts TextureAtlas.upload() -> redirects to ASTC cache when available
// MC 1.21.11 Mojang Mappings: ResourceLocation was renamed to Identifier
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.mixins;

import com.nexus.MobileGlues;
import com.nexus.astcmod.NativeASTCLoader;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;

/**
 * Intercepts {@link TextureAtlas#upload} to check whether a pre-compressed
 * ASTC version of this atlas exists in /sdcard/MG/cache/. If found, informs
 * the native lib via setNextAstcCache so that the subsequent GL texture upload
 * becomes a zero-copy glCompressedTexImage2D call.
 *
 * <p>MC 1.21.11 / Mojang Mappings note:
 * Mojang renamed ResourceLocation to Identifier in MC 1.21.11.
 * The correct import is net.minecraft.resources.Identifier.
 */
@Mixin(TextureAtlas.class)
public abstract class AstcTextureMixin {

    @Shadow private Identifier location;
    @Shadow private int width;
    @Shadow private int height;

    @Inject(method = "upload", at = @At("HEAD"), require = 0)
    private void mg_tryAstcCache(SpriteLoader.Preparations prep, CallbackInfo ci) {
        if (!MobileGlues.isAvailable()) return;
        try {
            if (this.location == null) return;
            String name = this.location.toString();
            String path = NativeASTCLoader.buildCachePath(name);
            if (new File(path).exists()) {
                int w = this.width  > 0 ? this.width  : prep.width();
                int h = this.height > 0 ? this.height : prep.height();
                MobileGlues.LOGGER.info("[MG-ASTC] Cache hit: {} ({}x{})", name, w, h);
                NativeASTCLoader.setNextAstcCache(path, w, h);
            }
        } catch (Throwable t) {
            MobileGlues.LOGGER.warn("[MG-ASTC] Cache check error: {}", t.getMessage());
        }
    }
}
