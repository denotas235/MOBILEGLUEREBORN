package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Generates partially destroyed ruins scattered across the world.
 * Types: abandoned houses, fallen towers, ancient foundations, stone altars.
 */
public final class NaturalRuins {

    private static final double RUIN_FREQ    = 0.0015;
    private static final double RUIN_THRESH  = 0.82;

    private NaturalRuins() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] NaturalRuins pronto para seed={}", seed);
    }

    /** True if a ruin should spawn near this chunk (called per chunk) */
    public static boolean shouldSpawnRuin(int chunkX, int chunkZ) {
        double n = SimplexNoise.noise(chunkX * RUIN_FREQ * 16, chunkZ * RUIN_FREQ * 16);
        return n * 0.5 + 0.5 > RUIN_THRESH;
    }

    /**
     * Generate a small ruin at position.
     * Type is chosen by noise-derived value.
     */
    public static void generateRuin(ServerLevel world, BlockPos origin, long seed) {
        Random rng = new Random(seed ^ origin.asLong());
        int type = rng.nextInt(4);
        int surfY = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin).getY();
        BlockPos base = new BlockPos(origin.getX(), surfY, origin.getZ());

        switch (type) {
            case 0 -> generateAbandonedHouse(world, base, rng);
            case 1 -> generateFallenTower(world, base, rng);
            case 2 -> generateAncientFoundation(world, base, rng);
            default -> generateStoneAltar(world, base, rng);
        }
    }

    private static void generateAbandonedHouse(ServerLevel world, BlockPos base, Random rng) {
        int w = 5 + rng.nextInt(3), l = 5 + rng.nextInt(3), h = 3 + rng.nextInt(2);
        BlockState wall = rng.nextBoolean() ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                            : Blocks.COBBLESTONE.defaultBlockState();
        // Build walls with random holes (ruined)
        for (int x = 0; x <= w; x++) {
            for (int z = 0; z <= l; z++) {
                boolean isWall = x == 0 || x == w || z == 0 || z == l;
                if (!isWall) continue;
                for (int y = 0; y < h; y++) {
                    if (rng.nextFloat() < 0.7f) { // 30% blocks missing = ruined look
                        world.setBlock(base.offset(x, y, z), wall, 3);
                    }
                }
            }
        }
        // Add moss carpet inside
        for (int x = 1; x < w; x++) {
            for (int z = 1; z < l; z++) {
                if (rng.nextFloat() < 0.3f) {
                    world.setBlock(base.offset(x, 0, z), Blocks.MOSS_CARPET.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void generateFallenTower(ServerLevel world, BlockPos base, Random rng) {
        int radius = 2, height = 4 + rng.nextInt(4);
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        // Build partial tower (only to partial height, missing chunks)
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double dist = Math.sqrt(x*x + z*z);
                    boolean isRing = dist >= radius - 0.5 && dist <= radius + 0.5;
                    if (!isRing) continue;
                    float destroyChance = (float)y / height;
                    if (rng.nextFloat() > destroyChance * 0.8f) {
                        BlockState blk = rng.nextFloat() < 0.4f ? mossy : stone;
                        world.setBlock(base.offset(x, y, z), blk, 3);
                    }
                }
            }
        }
    }

    private static void generateAncientFoundation(ServerLevel world, BlockPos base, Random rng) {
        int w = 8 + rng.nextInt(6), l = 8 + rng.nextInt(6);
        for (int x = 0; x <= w; x++) {
            for (int z = 0; z <= l; z++) {
                boolean edge = x == 0 || x == w || z == 0 || z == l;
                BlockState blk = edge ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                     : Blocks.COARSE_DIRT.defaultBlockState();
                if (rng.nextFloat() > 0.15f) {
                    world.setBlock(base.offset(x, 0, z), blk, 3);
                }
            }
        }
    }

    private static void generateStoneAltar(ServerLevel world, BlockPos base, Random rng) {
        // 3x3 platform, 2 steps high
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                world.setBlock(base.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    world.setBlock(base.offset(x, 1, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }
        // Centre top: carved stone or crafting table
        world.setBlock(base.offset(0, 2, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        // Pillars at corners
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                for (int y = 0; y < 3; y++) {
                    if (rng.nextFloat() > 0.3f) {
                        world.setBlock(base.offset(dx, y, dz),
                            Blocks.STONE_BRICK_SLAB.defaultBlockState(), 3);
                    }
                }
            }
        }
    }
}