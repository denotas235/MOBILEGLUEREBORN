package com.deno.maliworld.structure;

import com.deno.maliworld.noise.SimplexNoise;
import net.minecraft.world.level.LevelAccessor;

/**
 * Placeholder: Fortaleza em montanha. Gerada quando o terreno e alto o suficiente.
 */
public final class MountainFortress {

    private MountainFortress() {}

    public static boolean shouldSpawn(int chunkX, int chunkZ, int highestY) {
        return highestY >= 140 &&
               SimplexNoise.noise(chunkX * 0.05 + 400, chunkZ * 0.05 + 400) > 0.78;
    }
}