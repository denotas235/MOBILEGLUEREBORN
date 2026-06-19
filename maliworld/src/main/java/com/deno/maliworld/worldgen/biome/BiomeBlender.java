package com.deno.maliworld.worldgen.biome;

/** Interpolação bilinear de propriedades de bioma para transições suaves. */
public final class BiomeBlender {

    private BiomeBlender() {}

    /** Mistura dois valores de bioma pela distância (0=bioma A, 1=bioma B). */
    public static float blend(float valueA, float valueB, float t) {
        float smooth = t * t * (3.0f - 2.0f * t);
        return valueA + (valueB - valueA) * smooth;
    }
}
