package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.biome.ClimateMapper;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generates MaliWorld Mountain Fortresses: walled compounds with towers
 * and a small underground dungeon. Placed on mid-to-high elevation terrain.
 */
public final class MountainFortress {

    private static final double FORT_FREQ   = 0.0015;
    private static final double FORT_THRESH = 0.93;

    private MountainFortress() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] MountainFortress pronto, seed={}", seed);
    }

    /** Returns true if a fortress should be placed near this coordinate. */
    public static boolean isFortressLocation(int x, int z, int surfaceY) {
        if (surfaceY < 100) return false;
        double n = SimplexNoise.noise(x * FORT_FREQ, z * FORT_FREQ);
        return n > FORT_THRESH;
    }

    /** Place a walled fortress centered at (cx, surfaceY, cz) in the given level. */
    public static void place(ServerLevel world, int cx, int cz, int surfaceY) {
        try {
            buildWalls(world, cx, cz, surfaceY);
            buildTowers(world, cx, cz, surfaceY);
            buildDungeon(world, cx, cz, surfaceY);
            MaliWorldMod.LOGGER.debug("[MaliWorld] Fortaleza colocada em ({}, {}, {})", cx, surfaceY, cz);
        } catch (Exception e) {
            MaliWorldMod.LOGGER.debug("[MaliWorld] Fortaleza erro: {}", e.getMessage());
        }
    }

    private static void buildWalls(ServerLevel world, int cx, int cz, int y) {
        BlockState wall      = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState battlement = Blocks.STONE_BRICK_SLAB.defaultBlockState();
        int r = 12;
        for (int i = -r; i <= r; i++) {
            for (int h = 0; h < 5; h++) {
                world.setBlock(new BlockPos(cx + r, y + h, cz + i), wall, 2);
                world.setBlock(new BlockPos(cx - r, y + h, cz + i), wall, 2);
                world.setBlock(new BlockPos(cx + i, y + h, cz + r), wall, 2);
                world.setBlock(new BlockPos(cx + i, y + h, cz - r), wall, 2);
            }
            world.setBlock(new BlockPos(cx + r, y + 5, cz + i), battlement, 2);
            world.setBlock(new BlockPos(cx - r, y + 5, cz + i), battlement, 2);
            world.setBlock(new BlockPos(cx + i, y + 5, cz + r), battlement, 2);
            world.setBlock(new BlockPos(cx + i, y + 5, cz - r), battlement, 2);
        }
    }

    private static void buildTowers(ServerLevel world, int cx, int cz, int y) {
        BlockState stone  = Blocks.COBBLESTONE.defaultBlockState();
        int r = 12;
        int[][] corners = {{cx+r, cz+r}, {cx+r, cz-r}, {cx-r, cz+r}, {cx-r, cz-r}};
        for (int[] c : corners) {
            for (int dy = 0; dy < 8; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            world.setBlock(new BlockPos(c[0]+dx, y+dy, c[1]+dz), stone, 2);
                        }
                    }
                }
            }
        }
    }

    private static void buildDungeon(ServerLevel world, int cx, int cz, int y) {
        BlockState air   = Blocks.AIR.defaultBlockState();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        int startY = y - 8;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                for (int dy = 0; dy < 5; dy++) {
                    BlockPos p = new BlockPos(cx+dx, startY+dy, cz+dz);
                    if (Math.abs(dx) == 4 || Math.abs(dz) == 4 || dy == 0) {
                        world.setBlock(p, stone, 2);
                    } else {
                        world.setBlock(p, air, 2);
                    }
                }
            }
        }
    }
}