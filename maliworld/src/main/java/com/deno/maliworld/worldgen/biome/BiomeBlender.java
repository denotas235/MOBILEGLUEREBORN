package com.deno.maliworld.worldgen.biome;

/**
 * Biome transition blender.
 * Computes a blend weight for a given position based on its
 * distance to a biome boundary. Used by the BiomeSourceMixin
 * to interpolate properties across biome borders.
 */
public final class BiomeBlender {

    // Blend zone width in blocks
    private static final int BLEND_RADIUS = 24;

    private BiomeBlender() {}

    /**
     * Computes blend factor [0, 1] at a position for a biome boundary.
     * 1.0 = fully inside the biome (far from boundary)
     * 0.0 = at the boundary
     *
     * @param distToBoundary approximate distance to nearest biome boundary in blocks
     */
    public static float getBlendWeight(double distToBoundary) {
        if (distToBoundary >= BLEND_RADIUS) return 1.0f;
        double t = distToBoundary / BLEND_RADIUS;
        return (float)smoothstep(t);
    }

    /**
     * Lerp between two float values with smooth blend.
     */
    public static float blendFloat(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * Temperature at a blended boundary between two climate zones.
     * Prevents hard climate jumps.
     */
    public static float blendedTemperature(float tempA, float tempB, double distToBoundary) {
        float w = getBlendWeight(distToBoundary);
        return blendFloat(tempB, tempA, w);
    }

    /**
     * Humidity at a blended boundary.
     */
    public static float blendedHumidity(float humA, float humB, double distToBoundary) {
        float w = getBlendWeight(distToBoundary);
        return blendFloat(humB, humA, w);
    }

    /**
     * Approximates biome boundary distance from climate gradient.
     * Uses the magnitude of the temperature/humidity gradient.
     *
     * @param dTemp  |ΔT| per block
     * @param dHum   |ΔH| per block
     */
    public static double estimateBoundaryDist(float dTemp, float dHum) {
        double gradient = Math.sqrt(dTemp * dTemp + dHum * dHum);
        if (gradient < 0.001) return BLEND_RADIUS;
        return Math.min(BLEND_RADIUS, 1.0 / (gradient * 80.0));
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }
}