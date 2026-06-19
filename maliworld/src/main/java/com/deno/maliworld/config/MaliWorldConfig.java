package com.deno.maliworld.config;

import com.deno.maliworld.MaliWorldMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class MaliWorldConfig {

    public static boolean ENHANCED_TERRAIN     = true;
    public static int     TERRAIN_OCTAVES      = 6;
    public static float   MOUNTAIN_SCALE       = 1.5f;
    public static float   CONTINENT_SCALE      = 1.0f;

    public static boolean RIVERS_ENABLED       = true;
    public static boolean LAKES_ENABLED        = true;
    public static boolean WATERFALLS_ENABLED   = true;
    public static boolean PATHS_ENABLED        = true;
    public static boolean CAVES_ENHANCED       = true;

    public static boolean REALISTIC_VILLAGES   = true;
    public static boolean RUINS_ENABLED        = true;
    public static boolean FORTRESS_ENABLED     = true;
    public static boolean TEMPLES_ENABLED      = true;
    public static boolean UNDERGROUND_CITIES   = false;

    public static boolean GREEDY_MESHING       = true;
    public static boolean ASYNC_LIGHTING       = true;
    public static boolean ASYNC_PATHFINDING    = true;
    public static boolean MOB_TICK_THROTTLE    = true;
    public static int     MOB_NEAR_DISTANCE    = 32;
    public static int     MOB_FAR_DISTANCE     = 64;
    public static boolean ELYTRA_PREDICTOR     = true;
    public static boolean LIMIT_EXPLOSIONS     = true;
    public static int     EXPLOSION_MAX_RAYS   = 256;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        Path cfgPath = FabricLoader.getInstance().getConfigDir().resolve("maliworld.json");
        if (Files.exists(cfgPath)) {
            try (Reader r = Files.newBufferedReader(cfgPath)) {
                ConfigData data = GSON.fromJson(r, ConfigData.class);
                if (data != null) applyData(data);
            } catch (IOException e) {
                MaliWorldMod.LOGGER.warn("[MaliWorld] Falha ao ler config: {}", e.getMessage());
            }
        } else {
            save();
        }
    }

    public static void save() {
        Path cfgPath = FabricLoader.getInstance().getConfigDir().resolve("maliworld.json");
        try (Writer w = Files.newBufferedWriter(cfgPath)) {
            GSON.toJson(buildData(), w);
        } catch (IOException e) {
            MaliWorldMod.LOGGER.warn("[MaliWorld] Falha ao salvar config: {}", e.getMessage());
        }
    }

    private static ConfigData buildData() {
        ConfigData d = new ConfigData();
        d.enhancedTerrain    = ENHANCED_TERRAIN;
        d.terrainOctaves     = TERRAIN_OCTAVES;
        d.mountainScale      = MOUNTAIN_SCALE;
        d.continentScale     = CONTINENT_SCALE;
        d.riversEnabled      = RIVERS_ENABLED;
        d.lakesEnabled       = LAKES_ENABLED;
        d.waterfallsEnabled  = WATERFALLS_ENABLED;
        d.pathsEnabled       = PATHS_ENABLED;
        d.cavesEnhanced      = CAVES_ENHANCED;
        d.realisticVillages  = REALISTIC_VILLAGES;
        d.ruinsEnabled       = RUINS_ENABLED;
        d.fortressEnabled    = FORTRESS_ENABLED;
        d.templesEnabled     = TEMPLES_ENABLED;
        d.undergroundCities  = UNDERGROUND_CITIES;
        d.greedyMeshing      = GREEDY_MESHING;
        d.asyncLighting      = ASYNC_LIGHTING;
        d.asyncPathfinding   = ASYNC_PATHFINDING;
        d.mobTickThrottle    = MOB_TICK_THROTTLE;
        d.mobNearDistance    = MOB_NEAR_DISTANCE;
        d.mobFarDistance     = MOB_FAR_DISTANCE;
        d.elytraPredictor    = ELYTRA_PREDICTOR;
        d.limitExplosions    = LIMIT_EXPLOSIONS;
        d.explosionMaxRays   = EXPLOSION_MAX_RAYS;
        return d;
    }

    private static void applyData(ConfigData d) {
        ENHANCED_TERRAIN    = d.enhancedTerrain;
        TERRAIN_OCTAVES     = d.terrainOctaves;
        MOUNTAIN_SCALE      = d.mountainScale;
        CONTINENT_SCALE     = d.continentScale;
        RIVERS_ENABLED      = d.riversEnabled;
        LAKES_ENABLED       = d.lakesEnabled;
        WATERFALLS_ENABLED  = d.waterfallsEnabled;
        PATHS_ENABLED       = d.pathsEnabled;
        CAVES_ENHANCED      = d.cavesEnhanced;
        REALISTIC_VILLAGES  = d.realisticVillages;
        RUINS_ENABLED       = d.ruinsEnabled;
        FORTRESS_ENABLED    = d.fortressEnabled;
        TEMPLES_ENABLED     = d.templesEnabled;
        UNDERGROUND_CITIES  = d.undergroundCities;
        GREEDY_MESHING      = d.greedyMeshing;
        ASYNC_LIGHTING      = d.asyncLighting;
        ASYNC_PATHFINDING   = d.asyncPathfinding;
        MOB_TICK_THROTTLE   = d.mobTickThrottle;
        MOB_NEAR_DISTANCE   = d.mobNearDistance;
        MOB_FAR_DISTANCE    = d.mobFarDistance;
        ELYTRA_PREDICTOR    = d.elytraPredictor;
        LIMIT_EXPLOSIONS    = d.limitExplosions;
        EXPLOSION_MAX_RAYS  = d.explosionMaxRays;
    }

    private static class ConfigData {
        boolean enhancedTerrain    = true;
        int     terrainOctaves     = 6;
        float   mountainScale      = 1.5f;
        float   continentScale     = 1.0f;
        boolean riversEnabled      = true;
        boolean lakesEnabled       = true;
        boolean waterfallsEnabled  = true;
        boolean pathsEnabled       = true;
        boolean cavesEnhanced      = true;
        boolean realisticVillages  = true;
        boolean ruinsEnabled       = true;
        boolean fortressEnabled    = true;
        boolean templesEnabled     = true;
        boolean undergroundCities  = false;
        boolean greedyMeshing      = true;
        boolean asyncLighting      = true;
        boolean asyncPathfinding   = true;
        boolean mobTickThrottle    = true;
        int     mobNearDistance    = 32;
        int     mobFarDistance     = 64;
        boolean elytraPredictor    = true;
        boolean limitExplosions    = true;
        int     explosionMaxRays   = 256;
    }
}
