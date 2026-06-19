package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ancient temple generation: biome-appropriate structures.
 * MC 1.21.11: ServerLevel.setBlock(BlockPos, BlockState, int flags) — flags is int.
 */
public final class AncientTemple {

    private static final double TEMPLE_FREQ   = 0.0012;
    private static final double TEMPLE_THRESH = 0.94;

    private AncientTemple() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] AncientTemple pronto, seed={}", seed);
    }

    public static boolean isTempleLocation(int x, int z, int surfaceY) {
        if (surfaceY < 60) return false;
        return SimplexNoise.noise(x * TEMPLE_FREQ, z * TEMPLE_FREQ) > TEMPLE_THRESH;
    }

    public static void place(ServerLevel world, int cx, int cz, int surfaceY) {
        try {
            buildBase(world, cx, cz, surfaceY);
            buildColumns(world, cx, cz, surfaceY);
            buildPyramidTop(world, cx, cz, surfaceY);
            MaliWorldMod.LOGGER.debug("[MaliWorld] Templo em ({}, {})", cx, cz);
        } catch (Exception e) {
            MaliWorldMod.LOGGER.debug("[MaliWorld] Templo erro: {}", e.getMessage());
        }
    }

    private static void buildBase(ServerLevel world, int cx, int cz, int y) {
        BlockState stone = Blocks.SANDSTONE.defaultBlockState();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                for (int dy = -2; dy <= 0; dy++) {
                    world.setBlock(new BlockPos(cx+dx, y+dy, cz+dz), stone, 3);
                }
            }
        }
    }

    private static void buildColumns(ServerLevel world, int cx, int cz, int y) {
        BlockState col = Blocks.CHISELED_SANDSTONE.defaultBlockState();
        int[][] corners = {{-7,-7},{7,-7},{-7,7},{7,7},{-7,0},{7,0},{0,-7},{0,7}};
        for (int[] c : corners) {
            for (int h = 1; h <= 6; h++) {
                world.setBlock(new BlockPos(cx+c[0], y+h, cz+c[1]), col, 3);
            }
        }
    }

    private static void buildPyramidTop(ServerLevel world, int cx, int cz, int y) {
        BlockState cut = Blocks.CUT_SANDSTONE.defaultBlockState();
        for (int layer = 0; layer < 4; layer++) {
            int r = 5 - layer;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    world.setBlock(new BlockPos(cx+dx, y+1+layer, cz+dz), cut, 3);
                }
            }
        }
    }
}