package com.deno.maliworld;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.loot.LootTableInjector;
import com.deno.maliworld.registry.FeatureRegistry;
import com.deno.maliworld.registry.NoiseRegistry;
import com.deno.maliworld.registry.OptimizationSystems;
import com.deno.maliworld.registry.StructureRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliWorldMod implements ModInitializer {

    public static final String MOD_ID = "maliworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MaliWorld] Iniciando - MC 1.21.11 Mojang Mappings");
        MaliWorldConfig.load();
        NoiseRegistry.init();
        FeatureRegistry.register();
        StructureRegistry.register();
        LootTableInjector.inject();
        OptimizationSystems.init();
        LOGGER.info("[MaliWorld] Pronto. Terreno aprimorado, estruturas e otimizacoes ativas.");
    }
}