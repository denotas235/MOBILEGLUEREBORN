package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Generates stone fortresses on mountain peaks (Y >= 140).
 * One fortress per mountain chain (spaced by noise regions).
 * Features: towers, walls, inner hall, dungeon, panoramic view.
 */
public final class MountainFortress {

    private static final double FORTRESS_FREQ   = 0.0008;
    private static final double FORTRESS_THRESH = 0.88;
    private static final int    MIN_FORTRESS_Y  = 140;

    private MountainFortress() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] MountainFortress pronto para seed={}", seed);
    }

    public static boolean shouldSpawnFortress(int chunkX, int chunkZ) {
        double n = SimplexNoise.noise(chunkX * FORTRESS_FREQ * 16, chunkZ * FORTRESS_FREQ * 16);
        return n * 0.5 + 0.5 > FORTRESS_THRESH;
    }

    public static void generateFortress(ServerLevel world, BlockPos origin, long seed) {
        int surfY = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin).getY();
        if (surfY < MIN_FORTRESS_Y) return; // only on high mountains

        Random rng = new Random(seed ^ origin.asLong());
        BlockPos base = new BlockPos(origin.getX(), surfY, origin.getZ());

        buildWalls(world, base, rng);
        buildTowers(world, base, rng);
        buildInnerHall(world, base, rng);
        buildDungeon(world, base);
    }

    private static void buildWalls(ServerLevel world, BlockPos base, Random rng) {
        int wallSize = 16, wallH = 6;
        BlockState wall = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState battlement = Blocks.STONE_BRICK_SLAB.defaultBlockState();

        for (int i = -wallSize; i <= wallSize; i++) {
            for (int side = 0; side < 4; side++) {
                int x = side < 2 ? i : (side == 2 ? -wallSize : wallSize);
                int z = side >= 2 ? i : (side == 0 ? -wallSize : wallSize);
                for (int y = 0; y < wallH; y++) {
                    world.setBlock(base.offset(x, y, z), wall, 3);
                }
                // Battlements on top (alternating)
                if (Math.abs(i) % 2 == 0) {
                    world.setBlock(base.offset(x, wallH, z), battlement, 3);
                }
            }
        }
    }

    private static void buildTowers(ServerLevel world, BlockPos base, Random rng) {
        int[] corners = {-16, 16};
        for (int cx : corners) {
            for (int cz : corners) {
                int towerH = 10 + rng.nextInt(4);
                for (int y = 0; y < towerH; y++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            double dist = Math.sqrt(dx*dx + dz*dz);
                            if (dist <= 2.5) {
                                world.setBlock(base.offset(cx+dx, y, cz+dz),
                                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
                            }
                        }
                    }
                }
                // Torch at top
                world.setBlock(base.offset(cx, towerH, cz),
                    Blocks.TORCH.defaultBlockState(), 3);
            }
        }
    }

    private static void buildInnerHall(ServerLevel world, BlockPos base, Random rng) {
        int hw = 8, hh = 5;
        // Floor
        for (int x = -hw; x <= hw; x++) {
            for (int z = -hw; z <= hw; z++) {
                world.setBlock(base.offset(x, 0, z), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
        // Walls and roof
        for (int x = -hw; x <= hw; x++) {
            for (int z = -hw; z <= hw; z++) {
                boolean isWall = x == -hw || x == hw || z == -hw || z == hw;
                for (int y = 1; y <= hh; y++) {
                    if (isWall || y == hh) {
                        world.setBlock(base.offset(x, y, z),
                            Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Braziers
        for (int[] corner : new int[][]{{-5,5},{5,5},{-5,-5},{5,-5}}) {
            world.setBlock(base.offset(corner[0], 1, corner[1]),
                Blocks.CAMPFIRE.defaultBlockState(), 3);
        }
    }

    private static void buildDungeon(ServerLevel world, BlockPos base) {
        int depth = 8;
        for (int y = -1; y >= -depth; y--) {
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    boolean isWall = x == -4 || x == 4 || z == -4 || z == 4;
                    if (isWall || y == -depth) {
                        world.setBlock(base.offset(x, y, z),
                            Blocks.COBBLESTONE.defaultBlockState(), 3);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Chest with loot
        world.setBlock(base.offset(2, -depth+1, 2), Blocks.CHEST.defaultBlockState(), 3);
        // Torches
        world.setBlock(base.offset(0, -2, 0), Blocks.LANTERN.defaultBlockState(), 3);
    }
}