package com.deno.maliworld.worldgen.noise;

/**
 * Domain Warping - Inigo Quilez technique.
 * Distorts input coordinates before sampling noise, creating
 * organic, non-repetitive terrain that looks geologically real.
 */
public final class DomainWarp {

    private final double frequency;
    private final double strength;

    /**
     * @param frequency  how large the warp patterns are (smaller = larger warps)
     * @param strength   how far coordinates are displaced (in world units)
     */
    public DomainWarp(double frequency, double strength) {
        this.frequency = frequency;
        this.strength  = strength;
    }

    /**
     * Warp the input coordinates and return the displacement.
     * Use warpedX/warpedZ as inputs to subsequent noise calls.
     *
     * Standard Quilez formula:
     *   q = fbm(pos)
     *   r = fbm(pos + q)
     *   result = fbm(pos + r)
     */
    public double[] warp(double x, double z) {
        // First level warp displacement
        double qx = SimplexNoise.noise(x * frequency,           z * frequency + 0.0);
        double qz = SimplexNoise.noise(x * frequency + 5.2,     z * frequency + 1.3);

        // Second level warp (more complex distortion)
        double rx = SimplexNoise.noise((x + strength*qx) * frequency + 1.7,
                                       (z + strength*qz) * frequency + 9.2);
        double rz = SimplexNoise.noise((x + strength*qx) * frequency + 8.3,
                                       (z + strength*qz) * frequency + 2.8);

        // Return warped coordinates
        return new double[]{x + strength*rx, z + strength*rz};
    }

    /**
     * Single-level warp (faster, good for ocean/continent masks).
     */
    public double[] warpLight(double x, double z) {
        double qx = SimplexNoise.noise(x * frequency,       z * frequency);
        double qz = SimplexNoise.noise(x * frequency + 3.7, z * frequency + 7.1);
        return new double[]{x + (strength*0.4)*qx, z + (strength*0.4)*qz};
    }

    /**
     * Sample a FractalNoise at warped coordinates.
     */
    public double warpedSample(FractalNoise noise, double x, double z) {
        double[] w = warp(x, z);
        return noise.sample(w[0], w[1]);
    }
}