package com.deno.maliworld.worldgen;

import com.deno.maliworld.noise.DomainWarp;
import com.deno.maliworld.noise.FractalNoise;

/**
 * Combina continentes + montanhas + domain warp para gerar a altura final do terreno.
 * Valores retornados: -1.0 (oceano profundo) a 1.0 (pico de montanha).
 */
public final class TerrainShaper {

    private static final FractalNoise BASE    = new FractalNoise(6, 0.5, 2.0, 0.003);
    private static final FractalNoise DETAIL  = new FractalNoise(4, 0.4, 2.5, 0.015);
    private static final DomainWarp   WARP    = new DomainWarp(80.0, 0.004);

    private TerrainShaper() {}

    /**
     * @return valor em [-1, 1] representando a altura relativa
     */
    public static double shape(double worldX, double worldZ) {
        double[] warped = WARP.warp2(worldX, worldZ);
        double continent = ContinentGenerator.sample(warped[0], warped[1]);
        double mountain  = MountainBuilder.sample(warped[0], warped[1]);
        double base      = BASE.sample(warped[0], warped[1]);
        double detail    = DETAIL.sample(worldX, worldZ) * 0.15;

        double combined = continent * 0.5 + mountain * 0.35 + base * 0.15 + detail;
        return Math.max(-1.0, Math.min(1.0, combined));
    }

    /** Converte o valor shape para altura de bloco (Y). */
    public static int toBlockHeight(double shapeValue, int seaLevel, int minHeight, int maxHeight) {
        if (shapeValue < 0) {
            double t = (shapeValue + 1.0) * 0.5;
            return (int)(minHeight + t * seaLevel);
        } else {
            double t = shapeValue;
            return (int)(seaLevel + t * t * (maxHeight - seaLevel));
        }
    }
}