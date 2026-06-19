package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.DomainWarp;
import com.deno.maliworld.worldgen.noise.FractalNoise;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import com.deno.maliworld.worldgen.terrain.ContinentGenerator;
import com.deno.maliworld.worldgen.terrain.MountainBuilder;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;

/**
 * Initializes and holds the noise and terrain generation singletons.
 * Called once during mod initialization.
 */
public final class NoiseRegistry {

    public static FractalNoise   continentNoise;
    public static FractalNoise   mountainNoise;
    public static FractalNoise   detailNoise;
    public static DomainWarp     domainWarp;
    public static TerrainShaper  terrainShaper;
    public static ContinentGenerator continentGenerator;
    public static MountainBuilder    mountainBuilder;

    private NoiseRegistry() {}

    public static void init() {
        // Continent: low frequency, 4 octaves
        continentNoise   = new FractalNoise(4, 0.004, 0.5, 2.0, 0L);
        // Mountain ridges: medium frequency, 5 octaves
        mountainNoise    = new FractalNoise(5, 0.008, 0.5, 2.1, 1111L);
        // Detail: high frequency, 6 octaves
        detailNoise      = new FractalNoise(6, 0.025, 0.5, 2.2, 2222L);
        // Domain warp for all terrain
        domainWarp       = new DomainWarp(0.012, 80.0);
        // High-level terrain composers
        continentGenerator = new ContinentGenerator(continentNoise, domainWarp);
        mountainBuilder    = new MountainBuilder(mountainNoise, domainWarp);
        terrainShaper      = new TerrainShaper(continentGenerator, mountainBuilder, detailNoise);

        MaliWorldMod.LOGGER.info("[MaliWorld] Sistema de noise inicializado (SimplexNoise FBM + DomainWarp).");
    }
}