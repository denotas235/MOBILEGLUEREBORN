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

/**
 * JSON configuration for MaliWorld.
 * File: config/maliworld.json
 * All fields are public static so systems can read them directly.
 */
public class MaliWorldConfig {

    // --- Terrain ---
    public static boolean ENHANCED_TERRAIN    = true;
    public static int     TERRAIN_OCTAVES     = 6;
    public static float   MOUNTAIN_SCALE      = 1.5f;
    public static float   CONTINENT_SCALE     = 1.0f;
    public static float   DOMAIN_WARP_STRENGTH= 0.8f;

    // --- Features ---
    public static boolean RIVERS_ENABLED      = true;
    public static boolean LAKES_ENABLED       = true;
    public static boolean WATERFALLS_ENABLED  = true;
    public static boolean PATHS_ENABLED       = true;
    public static boolean CAVES_ENHANCED      = true;

    // --- Structures ---
    public static boolean REALISTIC_VILLAGES  = true;
    public static boolean RUINS_ENABLED       = true;
    public static boolean FORTRESS_ENABLED    = true;
    public static boolean TEMPLES_ENABLED     = true;
    public static boolean UNDERGROUND_CITIES  = false; // heavy, off by default

    // --- Optimizations ---
    public static boolean GREEDY_MESHING      = true;
    public static boolean ASYNC_LIGHTING      = true;
    public static boolean ASYNC_PATHFINDING   = true;
    public static boolean MOB_TICK_THROTTLE   = true;
    public static int     MOB_NEAR_DISTANCE   = 32;
    public static int     MOB_FAR_DISTANCE    = 64;
    public static boolean ELYTRA_PREDICTOR    = true;
    public static boolean LIMIT_EXPLOSIONS    = true;
    public static int     ENTITY_CULL_MOB_DIST= 48;
    public static int     ENTITY_CULL_ITEM_DIST= 24;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("maliworld.json");
        if (Files.exists(configPath)) {
            try (Reader r = Files.newBufferedReader(configPath)) {
                MaliWorldConfig loaded = GSON.fromJson(r, MaliWorldConfig.class);
                if (loaded != null) applyFrom(loaded);
                MaliWorldMod.LOGGER.info("[MaliWorld] Config carregada de {}", configPath);
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                MaliWorldMod.LOGGER.warn("[MaliWorld] Falha ao ler config, usando padroes: {}", e.getMessage());
            }
        } else {
            save();
            MaliWorldMod.LOGGER.info("[MaliWorld] Config criada em {}", configPath);
        }
    }

    public static void save() {
        if (configPath == null) return;
        try (Writer w = Files.newBufferedWriter(configPath)) {
            GSON.toJson(new MaliWorldConfig(), w);
        } catch (IOException e) {
            MaliWorldMod.LOGGER.warn("[MaliWorld] Falha ao salvar config: {}", e.getMessage());
        }
    }

    private static void applyFrom(MaliWorldConfig src) {
        ENHANCED_TERRAIN    = src.ENHANCED_TERRAIN;
        TERRAIN_OCTAVES     = src.TERRAIN_OCTAVES;
        MOUNTAIN_SCALE      = src.MOUNTAIN_SCALE;
        CONTINENT_SCALE     = src.CONTINENT_SCALE;
        DOMAIN_WARP_STRENGTH= src.DOMAIN_WARP_STRENGTH;
        RIVERS_ENABLED      = src.RIVERS_ENABLED;
        LAKES_ENABLED       = src.LAKES_ENABLED;
        WATERFALLS_ENABLED  = src.WATERFALLS_ENABLED;
        PATHS_ENABLED       = src.PATHS_ENABLED;
        CAVES_ENHANCED      = src.CAVES_ENHANCED;
        REALISTIC_VILLAGES  = src.REALISTIC_VILLAGES;
        RUINS_ENABLED       = src.RUINS_ENABLED;
        FORTRESS_ENABLED    = src.FORTRESS_ENABLED;
        TEMPLES_ENABLED     = src.TEMPLES_ENABLED;
        UNDERGROUND_CITIES  = src.UNDERGROUND_CITIES;
        GREEDY_MESHING      = src.GREEDY_MESHING;
        ASYNC_LIGHTING      = src.ASYNC_LIGHTING;
        ASYNC_PATHFINDING   = src.ASYNC_PATHFINDING;
        MOB_TICK_THROTTLE   = src.MOB_TICK_THROTTLE;
        MOB_NEAR_DISTANCE   = src.MOB_NEAR_DISTANCE;
        MOB_FAR_DISTANCE    = src.MOB_FAR_DISTANCE;
        ELYTRA_PREDICTOR    = src.ELYTRA_PREDICTOR;
        LIMIT_EXPLOSIONS    = src.LIMIT_EXPLOSIONS;
        ENTITY_CULL_MOB_DIST= src.ENTITY_CULL_MOB_DIST;
        ENTITY_CULL_ITEM_DIST=src.ENTITY_CULL_ITEM_DIST;
    }
}