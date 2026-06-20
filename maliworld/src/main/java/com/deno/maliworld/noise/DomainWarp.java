package com.deno.maliworld.noise;

/**
 * Domain Warping (tecnica de Inigo Quilez).
 * Distorce coordenadas de entrada antes de calcular o noise,
 * criando formas mais organicas e unicas.
 */
public final class DomainWarp {

    private final FractalNoise warpNoise;
    private final double warpStrength;

    public DomainWarp(double warpStrength, double scale) {
        this.warpNoise   = new FractalNoise(4, 0.5, 2.0, scale * 0.7);
        this.warpStrength = warpStrength;
    }

    public double[] warp(double x, double z) {
        double wx = warpNoise.sample(x + 1.7, z + 9.2);
        double wz = warpNoise.sample(x + 8.3, z + 2.8);
        return new double[]{ x + wx * warpStrength, z + wz * warpStrength };
    }

    public double[] warp2(double x, double z) {
        double[] first = warp(x, z);
        double wx = warpNoise.sample(first[0] + 3.4, first[1] + 5.6);
        double wz = warpNoise.sample(first[0] + 7.1, first[1] + 1.3);
        return new double[]{ first[0] + wx * warpStrength * 0.5,
                             first[1] + wz * warpStrength * 0.5 };
    }
}