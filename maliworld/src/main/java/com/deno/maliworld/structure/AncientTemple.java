package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * Generates ancient temples by biome type.
 * Jungle: sandstone pyramid with traps
 * Desert: semi-buried sandstone temple
 * Ocean: prismarine submerged temple
 * Forest: moss-covered stone temple
 */
public final class AncientTemple {

    private static final double TEMPLE_FREQ   = 0.0010;
    private static final double TEMPLE_THRESH = 0.87;

    private AncientTemple() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        MaliWorldMod.LOGGER.debug("[MaliWorld] AncientTemple pronto para seed={}", seed);
    }

    public static boolean shouldSpawnTemple(int chunkX, int chunkZ) {
        double n = SimplexNoise.noise(chunkX * TEMPLE_FREQ * 16, chunkZ * TEMPLE_FREQ * 16);
        return n * 0.5 + 0.5 > TEMPLE_THRESH;
    }

    public static void generateTemple(ServerLevel world, BlockPos origin, long seed, float temp, float humidity) {
        Random rng = new Random(seed ^ origin.asLong());
        int surfY = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, origin).getY();
        BlockPos base = new BlockPos(origin.getX(), surfY, origin.getZ());

        if (surfY <= 62) {
            // Ocean: prismarine submerged temple
            generateOceanTemple(world, base, rng);
        } else if (temp >= 2.0f && humidity > 0.5f) {
            // Jungle: pyramid with traps
            generateJunglePyramid(world, base, rng);
        } else if (temp >= 1.5f && humidity < 0.3f) {
            // Desert: sandstone temple
            generateDesertTemple(world, base, rng);
        } else {
            // Forest: mossy temple
            generateForestTemple(world, base, rng);
        }
    }

    private static void generateJunglePyramid(ServerLevel world, BlockPos base, Random rng) {
        int size = 12;
        for (int layer = 0; layer <= size; layer++) {
            int r = size - layer;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    boolean edge = Math.abs(x) == r || Math.abs(z) == r;
                    if (edge || layer == 0) {
                        world.setBlock(base.offset(x, layer, z),
                            layer % 3 == 0 ? Blocks.CHISELED_SANDSTONE.defaultBlockState()
                                          : Blocks.SANDSTONE.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Entrance
        world.setBlock(base.offset(0, 1, size), Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(base.offset(0, 2, size), Blocks.AIR.defaultBlockState(), 3);
        // Inner chamber with chest
        world.setBlock(base.offset(0, 2, 0), Blocks.CHEST.defaultBlockState(), 3);
        world.setBlock(base.offset(1, 1, 0), Blocks.TRIPWIRE_HOOK.defaultBlockState(), 3);
    }

    private static void generateDesertTemple(ServerLevel world, BlockPos base, Random rng) {
        // Semi-buried structure: half below ground
        int half = rng.nextInt(3) + 1;
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                for (int y = -half; y <= 6; y++) {
                    boolean isShell = x == -6 || x == 6 || z == -6 || z == 6 || y == -half || y == 6;
                    if (isShell) {
                        world.setBlock(base.offset(x, y, z),
                            Math.abs(x+y+z) % 4 == 0 ? Blocks.CHISELED_SANDSTONE.defaultBlockState()
                                                     : Blocks.SANDSTONE.defaultBlockState(), 3);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        world.setBlock(base.offset(0, 1, 0), Blocks.CHEST.defaultBlockState(), 3);
    }

    private static void generateOceanTemple(ServerLevel world, BlockPos base, Random rng) {
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -3; y <= 5; y++) {
                    boolean isShell = x == -5 || x == 5 || z == -5 || z == 5 || y == -3 || y == 5;
                    if (isShell) {
                        world.setBlock(base.offset(x, y, z),
                            y % 2 == 0 ? Blocks.PRISMARINE.defaultBlockState()
                                      : Blocks.PRISMARINE_BRICKS.defaultBlockState(), 3);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.WATER.defaultBlockState(), 3);
                    }
                }
            }
        }
        world.setBlock(base.offset(0, 0, 0), Blocks.CHEST.defaultBlockState(), 3);
    }

    private static void generateForestTemple(ServerLevel world, BlockPos base, Random rng) {
        int size = 8, height = 5;
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                for (int y = 0; y <= height; y++) {
                    boolean isShell = x == -size || x == size || z == -size || z == size || y == height;
                    if (isShell) {
                        boolean mossy = rng.nextFloat() < 0.4f;
                        world.setBlock(base.offset(x, y, z),
                            mossy ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                                 : Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Altar inside
        world.setBlock(base.offset(0, 1, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        world.setBlock(base.offset(0, 2, 0), Blocks.CHEST.defaultBlockState(), 3);
    }
}