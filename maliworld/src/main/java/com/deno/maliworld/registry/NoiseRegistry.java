package com.deno.maliworld.registry;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.noise.DomainWarp;
import com.deno.maliworld.noise.FractalNoise;

/**
 * Registo e inicializacao das instancias de noise do MaliWorld.
 * Os geradores de terreno (ContinentGenerator, MountainBuilder, TerrainShaper)
 * sao estaticos — nao precisam de ser instanciados aqui.
 */
public final class NoiseRegistry {

    public static FractalNoise continentNoise;
    public static FractalNoise mountainNoise;
    public static FractalNoise detailNoise;
    public static DomainWarp   domainWarp;

    private NoiseRegistry() {}

    public static void init() {
        // Construtores: (int octaves, double persistence, double lacunarity, double scale)
        continentNoise = new FractalNoise(4, 0.55, 2.0,  0.0008);
        mountainNoise  = new FractalNoise(5, 0.45, 2.2,  0.004);
        detailNoise    = new FractalNoise(6, 0.5,  2.0,  0.015);
        // DomainWarp construtor: (double warpStrength, double scale)
        domainWarp     = new DomainWarp(80.0, 0.004);
        MaliWorldMod.LOGGER.info("[MaliWorld] NoiseRegistry inicializado: {} noise generators.", 4);
    }
}