package com.deno.maliworld.worldgen.terrain;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.noise.FractalNoise;

/**
 * Combines ContinentGenerator + MountainBuilder + detail FBM into
 * a final terrain height. This is the core terrain compositor.
 *
 * Height output is in Minecraft Y coordinates [0, 320].
 */
public final class TerrainShaper {

    // Standard MC Y levels
    public static final int SEA_LEVEL     = 63;
    public static final int DEEP_OCEAN_Y  = 30;
    public static final int BEACH_Y       = 64;
    public static final int PLAINS_BASE_Y = 68;
    public static final int HILLS_MAX_Y   = 110;

    private final ContinentGenerator continent;
    private final MountainBuilder    mountains;
    private final FractalNoise       detail;

    public TerrainShaper(ContinentGenerator continent, MountainBuilder mountains, FractalNoise detail) {
        this.continent = continent;
        this.mountains = mountains;
        this.detail    = detail;
    }

    /**
     * Compute final terrain height at world position (x, z).
     * Returns Y coordinate [0, 320].
     */
    public int getHeight(double x, double z) {
        double cv = continent.getContinentValue(x, z);
        double mv = mountains.getMountainValue(x, z);

        // Blend based on continent value
        if (cv < -0.1) {
            // Ocean floor
            double oceanDepth = continent.getOceanDepth(x, z);
            double baseY = SEA_LEVEL - 5 - oceanDepth * (SEA_LEVEL - DEEP_OCEAN_Y - 5);
            double detailY = detail.sampleMapped(x, z, -3, 3);
            return (int)Math.max(DEEP_OCEAN_Y, baseY + detailY);
        } else if (cv < 0.1) {
            // Coastal transition
            double t = (cv + 0.1) / 0.2;  // 0..1 coast blend
            double oceanY  = SEA_LEVEL - 3.0;
            double landY   = BEACH_Y + t * 4;
            double baseY   = lerp(oceanY, landY, smoothStep(t));
            double detailY = detail.sampleMapped(x, z, -2, 2);
            return (int)(baseY + detailY);
        } else {
            // Land: blend plains, hills, mountains
            double landT   = Math.min(1.0, (cv - 0.1) / 0.9);
            double plainY  = PLAINS_BASE_Y + detail.sampleMapped(x, z, 0, 6);
            double hillY   = PLAINS_BASE_Y + detail.sampleMapped(x, z, 5, 35);
            double mountY  = mountains.getMountainY(x, z);

            // Weight mountains by mv
            double baseY;
            if (mv < 0.2) {
                baseY = lerp(plainY, hillY, mv / 0.2);
            } else {
                baseY = lerp(hillY, mountY, (mv - 0.2) / 0.8);
            }

            // Inland elevation boost
            baseY += landT * 4.0;

            // Add config scale
            baseY = SEA_LEVEL + (baseY - SEA_LEVEL) * MaliWorldConfig.CONTINENT_SCALE;
            return (int)Math.min(MountainBuilder.MOUNTAIN_PEAK_Y, Math.max(BEACH_Y, baseY));
        }
    }

    /**
     * Get surface block context for SurfaceDecorator.
     * Returns slope steepness [0-1] from finite differences.
     */
    public double getSlopeAt(double x, double z) {
        double step = 4.0;
        double h0 = getHeight(x, z);
        double hx = getHeight(x + step, z);
        double hz = getHeight(x, z + step);
        double dx = Math.abs(hx - h0) / step;
        double dz = Math.abs(hz - h0) / step;
        return Math.min(1.0, Math.max(dx, dz) * 0.5);
    }

    private static double lerp(double a, double b, double t) { return a + (b-a)*t; }
    private static double smoothStep(double t){ return t*t*(3-2*t); }
}