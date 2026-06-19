package com.deno.maliworld.worldgen.noise;

/**
 * Fractional Brownian Motion (FBM) built on top of SimplexNoise.
 * Stacks multiple octaves for natural-looking fractal terrain.
 */
public final class FractalNoise {

    private final int    octaves;
    private final double frequency;
    private final double persistence;  // amplitude multiplier per octave
    private final double lacunarity;   // frequency multiplier per octave
    private final double offsetX;
    private final double offsetZ;

    /**
     * @param octaves      number of noise layers (4-8 typical)
     * @param frequency    base sampling frequency
     * @param persistence  amplitude decay per octave (0.5 typical)
     * @param lacunarity   frequency growth per octave (2.0 typical)
     * @param seed         random seed for offset
     */
    public FractalNoise(int octaves, double frequency, double persistence,
                        double lacunarity, long seed) {
        this.octaves     = Math.max(1, octaves);
        this.frequency   = frequency;
        this.persistence = persistence;
        this.lacunarity  = lacunarity;
        // Derive offsets from seed so different seeds produce different worlds
        java.util.Random rng = new java.util.Random(seed);
        this.offsetX = rng.nextDouble() * 100000.0;
        this.offsetZ = rng.nextDouble() * 100000.0;
    }

    /**
     * Sample FBM at world position (x, z). Returns [-1, 1] approximately.
     */
    public double sample(double x, double z) {
        double value     = 0.0;
        double amplitude = 1.0;
        double freq      = frequency;
        double maxAmp    = 0.0;

        for (int i = 0; i < octaves; i++) {
            value  += SimplexNoise.noise((x + offsetX) * freq, (z + offsetZ) * freq) * amplitude;
            maxAmp += amplitude;
            amplitude *= persistence;
            freq      *= lacunarity;
        }
        return value / maxAmp;  // normalize to [-1, 1]
    }

    /**
     * Sample FBM with a 3rd dimension (e.g. altitude influence).
     */
    public double sample(double x, double y, double z) {
        double value     = 0.0;
        double amplitude = 1.0;
        double freq      = frequency;
        double maxAmp    = 0.0;

        for (int i = 0; i < octaves; i++) {
            value  += SimplexNoise.noise((x + offsetX) * freq, y * freq, (z + offsetZ) * freq) * amplitude;
            maxAmp += amplitude;
            amplitude *= persistence;
            freq      *= lacunarity;
        }
        return value / maxAmp;
    }

    /**
     * Sample and remap output to [min, max].
     */
    public double sampleMapped(double x, double z, double min, double max) {
        double raw = sample(x, z);
        return min + (raw * 0.5 + 0.5) * (max - min);
    }

    /**
     * Ridged FBM - produces sharp mountain ridges.
     */
    public double sampleRidged(double x, double z) {
        double value     = 0.0;
        double amplitude = 1.0;
        double freq      = frequency;
        double maxAmp    = 0.0;
        double weight    = 1.0;

        for (int i = 0; i < octaves; i++) {
            double n = SimplexNoise.noise((x + offsetX) * freq, (z + offsetZ) * freq);
            n = 1.0 - Math.abs(n);  // ridged transform
            n = n * n * weight;
            weight = Math.max(0, Math.min(1, n));
            value  += n * amplitude;
            maxAmp += amplitude;
            amplitude *= persistence;
            freq      *= lacunarity;
        }
        return value / maxAmp;
    }
}