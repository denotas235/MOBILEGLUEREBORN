// MobileGlues - MobileGlues.java
// JNI bridge singleton — shader sanitisation + GPU optimisation entry points
// Copyright (c) 2025-2026 MobileGL-Dev
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton that exposes the native GLES 3.0 sanitisation engine and
 * supplementary GPU optimisation entry points via JNI.
 *
 * <p>If the native library is unavailable (desktop / non-Android JVM),
 * {@link #isAvailable()} returns {@code false} and all Mixins gracefully
 * skip their native paths — zero crash guaranteed.
 */
public final class MobileGlues {

    public static final Logger LOGGER = LoggerFactory.getLogger("MobileGlues");
    public static final MobileGlues INSTANCE = new MobileGlues();

    private static final boolean NATIVE_AVAILABLE;
    static {
        boolean loaded = false;
        try {
            System.loadLibrary("mobileglues");
            loaded = true;
            LOGGER.info("[MobileGlues] Native library loaded.");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("[MobileGlues] Native library not found — all native paths disabled. ({})",
                    e.getMessage());
        }
        NATIVE_AVAILABLE = loaded;
    }

    private MobileGlues() {}

    /** @return true when the native library is present and JNI calls are safe. */
    public static boolean isAvailable() { return NATIVE_AVAILABLE; }

    // ── Shader sanitisation ───────────────────────────────────────────────────

    /**
     * Sanitises Desktop GLSL to GLES 3.0 compatible with Mali-G52.
     * Guarantees #version 300 es as first bytes, injects precision qualifiers,
     * removes problematic ASTC extension declarations.
     */
    public native String sanitizeShaderNative(String shaderSource);

    // ── Rendering pipeline ────────────────────────────────────────────────────

    /**
     * Updates the native sky / atmospheric state for the given time of day.
     * timeOfDay: 0.0 = midnight, 0.5 = noon, 1.0 = midnight (wraps).
     */
    public static native void updateSkyState(float timeOfDay);

    /**
     * Executes the godray (crepuscular-ray) post-processing pass.
     * @param sceneTexture GL texture ID of the rendered scene
     * @return true when the pass ran (GL_EXT_color_buffer_float available)
     */
    public static native boolean runGodrayPass(
            int sceneTexture,
            float sunScreenX, float sunScreenY,
            float screenW, float screenH);

    // ── Capability queries ────────────────────────────────────────────────────

    /**
     * @return true when the device supports ASTC texture compression.
     */
    public static native boolean hasAstcSupport();
}
