package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates massive underground cities at extreme depth (Y=-40 to -80).
 * MC 1.21.11: ServerLevel.setBlock(BlockPos, BlockState, int flags) — flags is int.
 * Disabled by default (MaliWorldConfig.UNDERGROUND_CITIES).
 */
public final class UndergroundCity {

    private static final double CITY_FREQ   = 0.0008;
    private static final double CITY_THRESH = 0.96;

    private UndergroundCity() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] UndergroundCity pronto, seed={}", seed);
    }

    public static boolean isCityLocation(int x, int z) {
        double n = SimplexNoise.noise(x * CITY_FREQ, z * CITY_FREQ);
        return n > CITY_THRESH;
    }

    /**
     * Carve and build city chamber. Flags int 3 = UPDATE_ALL for Level.setBlock().
     */
    public static void buildCity(ServerLevel world, int cx, int cz) {
        try {
            int baseY = -60;
            carveChamber(world, cx, cz, baseY);
            buildFloor(world, cx, cz, baseY);
            buildPillars(world, cx, cz, baseY);
            MaliWorldMod.LOGGER.debug("[MaliWorld] Cidade subterranea em ({}, {})", cx, cz);
        } catch (Exception e) {
            MaliWorldMod.LOGGER.debug("[MaliWorld] Cidade erro: {}", e.getMessage());
        }
    }

    private static void carveChamber(ServerLevel world, int cx, int cz, int baseY) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                double r = Math.sqrt(dx*dx + dz*dz);
                if (r > 20) continue;
                for (int dy = 0; dy < 12; dy++) {
                    BlockPos pos = new BlockPos(cx+dx, baseY+dy, cz+dz);
                    BlockState curr = world.getBlockState(pos);
                    if (!curr.is(Blocks.BEDROCK)) {
                        world.setBlock(pos, air, 3);
                    }
                }
            }
        }
    }

    private static void buildFloor(ServerLevel world, int cx, int cz, int baseY) {
        BlockState deepslate = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        for (int dx = -20; dx <= 20; dx++) {
            for (int dz = -20; dz <= 20; dz++) {
                if (Math.sqrt(dx*dx + dz*dz) <= 20) {
                    world.setBlock(new BlockPos(cx+dx, baseY-1, cz+dz), deepslate, 3);
                }
            }
        }
    }

    private static void buildPillars(ServerLevel world, int cx, int cz, int baseY) {
        BlockState pillar = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        int[][] positions = {{-10,-10},{10,-10},{-10,10},{10,10},{0,-15},{0,15},{-15,0},{15,0}};
        for (int[] p : positions) {
            for (int dy = 0; dy < 12; dy++) {
                world.setBlock(new BlockPos(cx+p[0], baseY+dy, cz+p[1]), pillar, 3);
            }
        }
    }
}