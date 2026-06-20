package com.deno.maliworld.worldgen;

import com.deno.maliworld.noise.FractalNoise;
import com.deno.maliworld.noise.SimplexNoise;

/**
 * Gera o mapa continental: onde ha terra vs oceano.
 * Valores < -0.2 = oceano, > 0.2 = terra, entre = costa.
 */
public final class ContinentGenerator {

    private static final FractalNoise CONTINENT = new FractalNoise(4, 0.55, 2.0, 0.0008);
    private static final double LAND_BIAS = 0.05;

    private ContinentGenerator() {}

    public static double sample(double x, double z) {
        double v = CONTINENT.sample(x, z) + LAND_BIAS;
        // Suaviza a transicao terra-oceano
        double shaped = Math.tanh(v * 3.0);
        return shaped;
    }

    public static boolean isOcean(double x, double z) {
        return sample(x, z) < -0.15;
    }

    public static boolean isCoast(double x, double z) {
        double v = sample(x, z);
        return v >= -0.25 && v <= 0.25;
    }
}