package com.deno.maliworld.worldgen;

import com.deno.maliworld.noise.FractalNoise;
import com.deno.maliworld.noise.SimplexNoise;

/**
 * Gera cadeias de montanhas coerentes usando ridge noise.
 * Cria picos e vales dramaticos com formas naturais.
 */
public final class MountainBuilder {

    private static final FractalNoise RIDGE    = new FractalNoise(5, 0.45, 2.2, 0.004);
    private static final FractalNoise LOCATION = new FractalNoise(2, 0.6,  2.0, 0.001);

    private MountainBuilder() {}

    public static double sample(double x, double z) {
        double locationStrength = (LOCATION.sample(x, z) + 1.0) * 0.5;
        locationStrength = Math.pow(Math.max(0, locationStrength - 0.3) / 0.7, 2.0);
        if (locationStrength < 0.01) return 0;

        double ridgeRaw = RIDGE.sample(x, z);
        double ridge = 1.0 - Math.abs(ridgeRaw);
        ridge = Math.max(0, ridge - 0.2) / 0.8;
        ridge = ridge * ridge * ridge;

        return ridge * locationStrength;
    }
}