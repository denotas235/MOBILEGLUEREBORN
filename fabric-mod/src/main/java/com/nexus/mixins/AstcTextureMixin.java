// MobileGlues - AstcTextureMixin.java
// MC 1.21.11 — targets string + Object param para maxima compatibilidade
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.mixins;

import com.nexus.MobileGlues;
import com.nexus.astcmod.NativeASTCLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Intercepta TextureAtlas.upload() para cache ASTC.
 * Usa reflexao — nao importa ResourceLocation nem SpriteLoader.Preparations
 * para garantir compatibilidade maxima com MC 1.21.11.
 * require=0: silenciosamente ignorado se upload() nao existir.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.TextureAtlas")
public abstract class AstcTextureMixin {

    @Inject(method = "upload", at = @At("HEAD"), require = 0)
    private void mg_tryAstcCache(Object prep, CallbackInfo ci) {
        if (!MobileGlues.isAvailable() || prep == null) return;
        try {
            String name = resolveAtlasName();
            int w = getDim(prep, "width",  256);
            int h = getDim(prep, "height", 256);
            String path = NativeASTCLoader.buildCachePath(name);
            if (new File(path).exists()) {
                MobileGlues.LOGGER.info("[MG-ASTC] Cache hit: {} ({}x{})", name, w, h);
                NativeASTCLoader.setNextAstcCache(path, w, h);
            }
        } catch (Throwable t) { /* never crash */ }
    }

    private String resolveAtlasName() {
        for (String fn : new String[]{"location", "id", "atlasLocation", "textureLocation"}) {
            try {
                Class<?> c = this.getClass();
                while (c != null && c != Object.class) {
                    try {
                        Field f = c.getDeclaredField(fn);
                        f.setAccessible(true);
                        Object v = f.get(this);
                        if (v != null) return v.toString();
                    } catch (NoSuchFieldException ignored) {}
                    c = c.getSuperclass();
                }
            } catch (Throwable ignored) {}
        }
        return this.toString();
    }

    private static int getDim(Object obj, String method, int def) {
        try {
            Method m = obj.getClass().getMethod(method);
            Object r = m.invoke(obj);
            if (r instanceof Integer iv && iv > 0) return iv;
        } catch (Throwable ignored) {}
        return def;
    }
}