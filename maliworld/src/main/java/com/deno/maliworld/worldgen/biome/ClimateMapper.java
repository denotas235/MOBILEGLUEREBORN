package com.deno.maliworld.worldgen.biome;

import com.deno.maliworld.worldgen.noise.SimplexNoise;

/**
 * Maps geographic position and altitude to climate values.
 * Mimics real-world climate distribution:
 *   - Temperature decreases with latitude (distance from origin) and altitude
 *   - Humidity influenced by proximity to ocean and local noise
 */
public final class ClimateMapper {

    // Scale factor: how far from origin = one full climate zone shift
    private static final double LATITUDE_SCALE   = 1.0 / 4000.0;
    private static final double HUMIDITY_FREQ    = 0.0008;
    private static final double TEMP_NOISE_FREQ  = 0.0006;

    public static final float TEMP_HOT    = 2.0f;
    public static final float TEMP_WARM   = 1.0f;
    public static final float TEMP_COOL   = 0.3f;
    public static final float TEMP_COLD   = -0.5f;
    public static final float TEMP_FROZEN = -1.0f;

    private ClimateMapper() {}

    /**
     * Temperature at position [-1, 2].
     * Decreases with distance from origin (north) and altitude.
     */
    public static float getTemperature(double x, double z, int y) {
        // Base latitude temperature (positive z = warmer equator)
        double latT = 1.0 - Math.abs(z) * LATITUDE_SCALE;
        latT = Math.max(-1.0, Math.min(2.0, latT));

        // Local variation from noise
        double noise = SimplexNoise.noise(x * TEMP_NOISE_FREQ, z * TEMP_NOISE_FREQ) * 0.4;

        // Altitude cooling: above Y=80, each 10 blocks = -0.05 temp
        double altCool = Math.max(0, (y - 80)) * 0.005;

        return (float)Math.max(TEMP_FROZEN, latT + noise - altCool);
    }

    /**
     * Humidity at position [0, 1].
     * Higher near oceans, lower inland and near deserts.
     */
    public static float getHumidity(double x, double z) {
        double base = SimplexNoise.noise(x * HUMIDITY_FREQ, z * HUMIDITY_FREQ) * 0.5 + 0.5;
        // Add large-scale variation
        double large = SimplexNoise.noise(x * HUMIDITY_FREQ * 0.3, z * HUMIDITY_FREQ * 0.3) * 0.3;
        return (float)Math.max(0, Math.min(1.0, base + large));
    }

    /**
     * Climate zone string for logging/debug.
     */
    public static String getZoneName(float temp, float humidity) {
        if (temp >= TEMP_HOT)   return humidity > 0.5 ? "jungle"   : "desert";
        if (temp >= TEMP_WARM)  return humidity > 0.6 ? "forest"   : humidity > 0.3 ? "plains" : "savanna";
        if (temp >= TEMP_COOL)  return humidity > 0.5 ? "birch_forest" : "plains";
        if (temp >= TEMP_COLD)  return humidity > 0.4 ? "taiga"    : "tundra";
        return "frozen";
    }
}