package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.worldgen.noise.DomainWarp;
import com.deno.maliworld.worldgen.noise.FractalNoise;

/**
 * Generates continental-scale landmasses.
 * Returns continent value in [-1, 1]:
 *   -1.0 to -0.1 = deep ocean
 *   -0.1 to  0.1 = coastal / shallow shelf
 *    0.1 to  1.0 = land (higher = more elevated)
 */
public final class ContinentGenerator {

    private final FractalNoise noise;
    private final DomainWarp   warp;

    public ContinentGenerator(FractalNoise noise, DomainWarp warp) {
        this.noise = noise;
        this.warp  = warp;
    }

    /**
     * Returns continent value at world position (x, z).
     * Positive = land, negative = ocean.
     */
    public double getContinentValue(double x, double z) {
        double[] w = warp.warpLight(x, z);
        double raw = noise.sample(w[0], w[1]);
        // Apply shaping curve: push extremes further to create clear continent/ocean divide
        return shapeCurve(raw);
    }

    /**
     * True if this position is above sea level (land).
     */
    public boolean isLand(double x, double z) {
        return getContinentValue(x, z) > 0.0;
    }

    /**
     * Ocean depth 0-1 (0 = shallow coastal, 1 = deep abyssal).
     */
    public double getOceanDepth(double x, double z) {
        double cv = getContinentValue(x, z);
        if (cv >= 0.0) return 0.0;
        return Math.min(1.0, -cv);
    }

    /**
     * Coastal proximity [0-1] (1 = coastline, 0 = far inland or deep ocean).
     */
    public double getCoastalValue(double x, double z) {
        double cv = Math.abs(getContinentValue(x, z));
        return Math.max(0, 1.0 - cv * 6.0);  // peaks at coast, falls off quickly
    }

    private double shapeCurve(double v) {
        // Cubic hermite shaping: pulls values toward extremes
        // while keeping the mid region (coast) narrow
        double sign = v >= 0 ? 1.0 : -1.0;
        double abs  = Math.abs(v);
        // s-curve: abs^1.5 makes coasts narrower and oceans/continents wider
        return sign * Math.pow(abs, 1.3);
    }
}