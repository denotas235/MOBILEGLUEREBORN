package com.deno.maliworld.config;

import com.deno.maliworld.MaliWorldMod;

/**
 * Configuracao do MaliWorld.
 * Todos os flags podem ser desativados para debug sem recompilar.
 */
public final class MaliWorldConfig {

    // Terreno
    public static boolean ENHANCED_TERRAIN   = true;
    public static int     TERRAIN_OCTAVES    = 6;
    public static float   MOUNTAIN_SCALE     = 1.5f;
    public static float   CONTINENT_SCALE    = 1.0f;

    // Features
    public static boolean RIVERS_ENABLED     = true;
    public static boolean LAKES_ENABLED      = true;
    public static boolean WATERFALLS_ENABLED = true;
    public static boolean PATHS_ENABLED      = true;
    public static boolean CAVES_ENHANCED     = true;

    // Estruturas
    public static boolean REALISTIC_VILLAGES = true;
    public static boolean RUINS_ENABLED      = true;
    public static boolean FORTRESS_ENABLED   = true;
    public static boolean TEMPLES_ENABLED    = true;
    public static boolean UNDERGROUND_CITIES = false;

    // Otimizacoes
    public static boolean MOB_TICK_THROTTLE  = true;
    public static int     MOB_NEAR_DISTANCE  = 32;
    public static int     MOB_FAR_DISTANCE   = 64;
    public static boolean ASYNC_LIGHTING     = true;
    public static boolean ASYNC_PATHFINDING  = true;
    public static boolean ELYTRA_PREDICTOR   = true;
    public static boolean LIMIT_EXPLOSIONS   = true;
    public static boolean ENTITY_CULLING     = true;
    public static boolean GREEDY_MESHING     = true;

    private MaliWorldConfig() {}

    public static void load() {
        MaliWorldMod.LOGGER.info("[MaliWorld] Configuracao carregada (valores padrao).");
    }
}