// MobileGlues - AstcTextureMixin.java
// Intercepta TextureAtlas.upload() para usar cache ASTC quando disponivel
// MC 1.21.11 Mojang Mappings: usa ResourceLocation (nao Identifier)
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.mixins;

import com.nexus.MobileGlues;
import com.nexus.astcmod.NativeASTCLoader;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;

/**
 * Intercepta {@link TextureAtlas#upload} para verificar se existe
 * uma versao ASTC pre-comprimida do atlas em /sdcard/MG/cache/.
 *
 * Correcao MC 1.21.11 Mojang Mappings:
 *   - ResourceLocation (Mojang) vs Identifier (Yarn) — usa ResourceLocation
 *   - Dimensoes obtidas de SpriteLoader.Preparations (record com width()/height())
 *   - Removido @Shadow de width/height que nao existem em TextureAtlas 1.21.11
 */
@Mixin(TextureAtlas.class)
public abstract class AstcTextureMixin {

    @Shadow private ResourceLocation location;

    @Inject(method = "upload", at = @At("HEAD"), require = 0)
    private void mg_tryAstcCache(SpriteLoader.Preparations prep, CallbackInfo ci) {
        if (!MobileGlues.isAvailable()) return;
        try {
            if (this.location == null || prep == null) return;
            String name = this.location.toString();
            String path = NativeASTCLoader.buildCachePath(name);
            if (new File(path).exists()) {
                int w = prep.width()  > 0 ? prep.width()  : 256;
                int h = prep.height() > 0 ? prep.height() : 256;
                MobileGlues.LOGGER.info("[MG-ASTC] Cache hit: {} ({}x{})", name, w, h);
                NativeASTCLoader.setNextAstcCache(path, w, h);
            }
        } catch (Throwable t) {
            MobileGlues.LOGGER.warn("[MG-ASTC] Erro ao verificar cache: {}", t.getMessage());
        }
    }
}