package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.DomainWarp;
import com.deno.maliworld.worldgen.noise.FractalNoise;
import com.deno.maliworld.worldgen.terrain.ContinentGenerator;
import com.deno.maliworld.worldgen.terrain.MountainBuilder;
import com.deno.maliworld.worldgen.terrain.TerrainShaper;

public final class NoiseRegistry {

    public static FractalNoise        continentNoise;
    public static FractalNoise        mountainNoise;
    public static FractalNoise        detailNoise;
    public static DomainWarp          domainWarp;
    public static TerrainShaper       terrainShaper;
    public static ContinentGenerator  continentGenerator;
    public static MountainBuilder     mountainBuilder;

    private NoiseRegistry() {}

    public static void init() {
        continentNoise     = new FractalNoise(4, 0.5f, 2.0f, 0L);
        mountainNoise      = new FractalNoise(5, 0.5f, 2.1f, 1111L);
        detailNoise        = new FractalNoise(6, 0.5f, 2.2f, 2222L);
        domainWarp         = new DomainWarp(3333L, 80.0f);
        continentGenerator = new ContinentGenerator(4444L);
        mountainBuilder    = new MountainBuilder(5555L);
        terrainShaper      = new TerrainShaper(6666L, 1.5f);
        MaliWorldMod.LOGGER.info("[MaliWorld] NoiseRegistry inicializado.");
    }
}