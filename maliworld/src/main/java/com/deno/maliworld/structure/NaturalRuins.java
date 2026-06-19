package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates abandoned ruins: crumbled houses, towers, altars.
 * MC 1.21.11: ServerLevel.setBlock(BlockPos, BlockState, int) — flags is int.
 */
public final class NaturalRuins {

    private static final double RUIN_FREQ   = 0.003;
    private static final double RUIN_THRESH = 0.91;

    private NaturalRuins() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] NaturalRuins pronto, seed={}", seed);
    }

    public static boolean isRuinLocation(int x, int z, int surfaceY) {
        if (surfaceY < 65) return false;
        double n = SimplexNoise.noise(x * RUIN_FREQ, z * RUIN_FREQ);
        return n > RUIN_THRESH;
    }

    public static void placeRuin(ServerLevel world, int cx, int cz, int surfaceY) {
        try {
            double type = SimplexNoise.noise(cx * 0.01, cz * 0.01);
            if (type < 0.33)      placeHouse(world, cx, cz, surfaceY);
            else if (type < 0.66) placeTower(world, cx, cz, surfaceY);
            else                  placeAltar(world, cx, cz, surfaceY);
            MaliWorldMod.LOGGER.debug("[MaliWorld] Ruina colocada em ({},{})", cx, cz);
        } catch (Exception e) {
            MaliWorldMod.LOGGER.debug("[MaliWorld] Ruina erro: {}", e.getMessage());
        }
    }

    private static void placeHouse(ServerLevel world, int cx, int cz, int y) {
        BlockState stone = Blocks.COBBLESTONE.defaultBlockState();
        BlockState air   = Blocks.AIR.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = 0; dy < 4; dy++) {
                    BlockPos p = new BlockPos(cx+dx, y+dy, cz+dz);
                    boolean isWall = Math.abs(dx)==3 || Math.abs(dz)==3;
                    if (isWall && dy < 3) {
                        if (SimplexNoise.noise(cx+dx, cy(dy)) > -0.3) {
                            world.setBlock(p, stone, 3);
                        }
                    } else if (dy == 0) {
                        world.setBlock(p, stone, 3);
                    }
                }
            }
        }
    }

    private static double cy(int dy) { return dy * 7.3; }

    private static void placeTower(ServerLevel world, int cx, int cz, int y) {
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        for (int dy = 0; dy < 10; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx)==2 || Math.abs(dz)==2) {
                        if (SimplexNoise.noise(cx+dx+dy, cz+dz) > -0.4) {
                            world.setBlock(new BlockPos(cx+dx, y+dy, cz+dz), stone, 3);
                        }
                    }
                }
            }
        }
    }

    private static void placeAltar(ServerLevel world, int cx, int cz, int y) {
        BlockState stone = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.setBlock(new BlockPos(cx+dx, y, cz+dz), stone, 3);
                if (Math.abs(dx)+Math.abs(dz) <= 1) {
                    world.setBlock(new BlockPos(cx+dx, y+1, cz+dz), stone, 3);
                }
            }
        }
    }
}