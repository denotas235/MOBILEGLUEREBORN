// MobileGlues - UnifiedCoreJNI.java
// JNI bridge for V-Sight culling, native chunk memory and light propagation
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.astcmod;

/**
 * JNI bridge to the MobileGlues unified native core (unified_core.cpp).
 */
public final class UnifiedCoreJNI {

    private UnifiedCoreJNI() {}

    /**
     * V-Sight foveal frustum culling (cylindrical, XZ plane).
     * Returns true if the entity is in the player's forward hemisphere.
     */
    public static native boolean isEntityInFocus(
            float px, float py, float pz,
            float lx, float ly, float lz,
            float ex, float ey, float ez);

    /** Allocate off-heap 16×256×16 block data at chunk (x, z). */
    public static native void allocateChunk(int x, int z);

    /** Free off-heap chunk block data at chunk (x, z). */
    public static native void freeChunk(int x, int z);

    /**
     * Triggers async native light propagation for chunk (chunkX, chunkZ).
     * Runs on a C++ thread pool — never blocks the main game thread.
     */
    public static native void propagateLightAsync(int chunkX, int chunkZ);

    /**
     * Applies basic Mali-G52 GLSL fixes: precision qualifiers + varying→in/out.
     */
    public static native String fixShaderForMali(String shaderSource);
}
