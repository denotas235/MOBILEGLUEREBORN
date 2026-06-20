package com.deno.maliworld;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.loot.LootTableInjector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MaliWorldMod implements ModInitializer {

    public static final String MOD_ID = "maliworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MaliWorld] Inicializando (MC 1.21.11, Mojang Mappings)...");
        try {
            MaliWorldConfig.load();
            LootTableInjector.register();
        } catch (Throwable t) {
            LOGGER.error("[MaliWorld] Falha na inicializacao: {}", t.getMessage());
        }
        LOGGER.info("[MaliWorld] Pronto. Terreno={} Rios={} Otimizacoes={}",
            MaliWorldConfig.ENHANCED_TERRAIN,
            MaliWorldConfig.RIVERS_ENABLED,
            MaliWorldConfig.MOB_TICK_THROTTLE);
    }
}