package com.deno.maliworld;

import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.loot.LootTableInjector;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaliWorldMod implements ModInitializer {

    public static final String MOD_ID = "maliworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MaliWorld] Inicializando...");

        MaliWorldConfig.load();
        LootTableInjector.register();

        LOGGER.info("[MaliWorld] Pronto. (terrain={}, rivers={}, asyncLight={}, asyncPath={})",
                MaliWorldConfig.ENHANCED_TERRAIN,
                MaliWorldConfig.RIVERS_ENABLED,
                MaliWorldConfig.ASYNC_LIGHTING,
                MaliWorldConfig.ASYNC_PATHFINDING);
    }
}
