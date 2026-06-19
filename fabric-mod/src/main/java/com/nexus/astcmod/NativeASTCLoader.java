// MobileGlues - NativeASTCLoader.java
// JNI bridge for ASTC cache loading
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.astcmod;

import java.io.File;

/**
 * JNI bridge to the native ASTC cache loader.
 * Pre-compressed ASTC textures live in /sdcard/MG/cache/.
 * When available, the C++ lib uploads via glCompressedTexImage2D directly,
 * bypassing CPU decompression entirely.
 */
public final class NativeASTCLoader {

    public static final String CACHE_DIR = "/sdcard/MG/cache";

    private NativeASTCLoader() {}

    /**
     * Tells the C++ lib to replace the NEXT glTexImage2D with ASTC from {@code path}.
     * Call this right before the GL texture upload (e.g. at HEAD of TextureAtlas.upload).
     */
    public static native void setNextAstcCache(String path, int width, int height);

    /**
     * Direct on-demand ASTC upload to the currently bound GL_TEXTURE_2D.
     * Returns true on success.
     */
    public static native boolean uploadASTC(String path, int width, int height);

    /** Returns true if an ASTC cache file exists for the given texture name. */
    public static boolean hasCachedAstc(String textureName) {
        return new File(buildCachePath(textureName)).exists();
    }

    /**
     * Converts a Minecraft resource location string (e.g. "minecraft:block/stone")
     * to the absolute .astc cache path.
     */
    public static String buildCachePath(String textureName) {
        String cleaned = textureName
                .replace(":", "_")
                .replace("/", "_")
                .replace("\\", "_");
        return CACHE_DIR + "/" + cleaned + ".astc";
    }
}
