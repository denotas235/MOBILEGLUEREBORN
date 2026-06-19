package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.noise.DomainWarp;
import com.deno.maliworld.worldgen.noise.FractalNoise;

/**
 * Generates coherent mountain chains using ridged FBM.
 * Mountains form along "tectonic" lines, not scattered randomly.
 */
public final class MountainBuilder {

    private final FractalNoise noise;
    private final DomainWarp   warp;

    // Y-levels
    public static final int MOUNTAIN_BASE_Y  = 80;
    public static final int MOUNTAIN_PEAK_Y  = 240;
    public static final int SNOW_LINE_Y      = 180;

    public MountainBuilder(FractalNoise noise, DomainWarp warp) {
        this.noise = noise;
        this.warp  = warp;
    }

    /**
     * Mountain influence [0, 1] at position.
     * 0 = flat terrain, 1 = high mountain peak.
     */
    public double getMountainValue(double x, double z) {
        double[] w = warp.warp(x, z);
        double ridged = noise.sampleRidged(w[0], w[1]);
        // Apply scale from config
        ridged *= MaliWorldConfig.MOUNTAIN_SCALE;
        return Math.max(0.0, Math.min(1.0, ridged));
    }

    /**
     * Target Y height for mountain terrain at this position.
     * Returns value in [MOUNTAIN_BASE_Y, MOUNTAIN_PEAK_Y].
     */
    public int getMountainY(double x, double z) {
        double mv = getMountainValue(x, z);
        // Exponential curve: low values stay near base, high values spike
        double curved = Math.pow(mv, 1.5);
        return (int)(MOUNTAIN_BASE_Y + curved * (MOUNTAIN_PEAK_Y - MOUNTAIN_BASE_Y));
    }

    /**
     * Whether this position has snow (above snow line with enough mountain value).
     */
    public boolean hasSnow(double x, int y, double z) {
        return y >= SNOW_LINE_Y && getMountainValue(x, z) > 0.5;
    }

    /**
     * Whether terrain is a steep slope (rock exposed, no grass).
     */
    public boolean isSteepSlope(double x, double z, double dx, double dz) {
        double here  = getMountainValue(x, z);
        double right = getMountainValue(x + dx, z + dz);
        return Math.abs(here - right) > 0.15;
    }
}