package com.deno.maliworld.biome;

import com.deno.maliworld.noise.SimplexNoise;

/**
 * Suaviza a transicao entre biomas usando interpolacao bilinear.
 * Evita a borda abrupta que existe no vanilla.
 */
public final class BiomeBlender {

    private static final double BLEND_RADIUS = 16.0;

    private BiomeBlender() {}

    /**
     * Calcula o peso de mistura deste bioma na posicao dada.
     * @return [0, 1] — 1 = totalmente neste bioma, 0 = outro bioma
     */
    public static double blendWeight(double worldX, double worldZ, double biomeCenterX, double biomeCenterZ) {
        double dx = worldX - biomeCenterX;
        double dz = worldZ - biomeCenterZ;
        double dist = Math.sqrt(dx*dx + dz*dz);
        double t = 1.0 - Math.min(1.0, dist / BLEND_RADIUS);
        return smoothstep(t);
    }

    private static double smoothstep(double t) { return t * t * (3 - 2 * t); }

    /**
     * Interpola dois valores de temperatura com suavizacao.
     */
    public static float lerp(float a, float b, float t) { return a + (b - a) * smoothstep(t); }
    private static float smoothstep(float t) { return t * t * (3 - 2 * t); }
}