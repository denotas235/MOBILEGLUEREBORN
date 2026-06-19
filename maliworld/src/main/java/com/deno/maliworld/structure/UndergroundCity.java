package com.deno.maliworld.structure;

import com.deno.maliworld.MaliWorldMod;
import com.deno.maliworld.config.MaliWorldConfig;
import com.deno.maliworld.worldgen.noise.SimplexNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

/**
 * Generates massive underground cities in deep cavern chambers.
 * Disabled by default (heavy, set UNDERGROUND_CITIES=true in config).
 * Depth: Y=-40 to Y=-80, size: 200x200 block chamber.
 */
public final class UndergroundCity {

    private static final double CITY_FREQ   = 0.0005;
    private static final double CITY_THRESH = 0.92;
    // Y range
    private static final int CITY_TOP_Y    = -40;
    private static final int CITY_BOTTOM_Y = -80;
    private static final int CITY_SIZE     = 100; // half-width

    private UndergroundCity() {}

    public static void onWorldLoad(ServerLevel world, long seed) {
        if (!MaliWorldConfig.UNDERGROUND_CITIES) return;
        MaliWorldMod.LOGGER.info("[MaliWorld] UndergroundCity ATIVA - modo pesado ligado.");
    }

    public static boolean shouldSpawnCity(int chunkX, int chunkZ) {
        if (!MaliWorldConfig.UNDERGROUND_CITIES) return false;
        double n = SimplexNoise.noise(chunkX * CITY_FREQ * 16, chunkZ * CITY_FREQ * 16);
        return n * 0.5 + 0.5 > CITY_THRESH;
    }

    public static void generateCity(ServerLevel world, BlockPos origin, long seed) {
        if (!MaliWorldConfig.UNDERGROUND_CITIES) return;
        Random rng = new Random(seed ^ origin.asLong());
        BlockPos center = new BlockPos(origin.getX(), CITY_TOP_Y, origin.getZ());

        MaliWorldMod.LOGGER.info("[MaliWorld] Gerando cidade subterranea em {}", center);

        // 1. Carve the giant chamber
        carveChamber(world, center);
        // 2. Place streets
        placeStreets(world, center, rng);
        // 3. Place buildings
        placeBuildings(world, center, rng);
        // 4. Central plaza
        buildCentralPlaza(world, center, rng);
        // 5. Lighting crystals
        placeCrystalLights(world, center, rng);
    }

    private static void carveChamber(ServerLevel world, BlockPos center) {
        int sizeY = CITY_TOP_Y - CITY_BOTTOM_Y;
        for (int x = -CITY_SIZE; x <= CITY_SIZE; x++) {
            for (int z = -CITY_SIZE; z <= CITY_SIZE; z++) {
                for (int y = CITY_BOTTOM_Y; y <= CITY_TOP_Y; y++) {
                    BlockPos pos = new BlockPos(center.getX()+x, y, center.getZ()+z);
                    if (!world.getBlockState(pos).is(net.minecraft.tags.BlockTags.FEATURES_CANNOT_REPLACE)) {
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Stone floor
        for (int x = -CITY_SIZE; x <= CITY_SIZE; x++) {
            for (int z = -CITY_SIZE; z <= CITY_SIZE; z++) {
                world.setBlock(new BlockPos(center.getX()+x, CITY_BOTTOM_Y, center.getZ()+z),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
    }

    private static void placeStreets(ServerLevel world, BlockPos center, Random rng) {
        // Main cross-streets
        for (int i = -CITY_SIZE; i <= CITY_SIZE; i++) {
            // East-West street
            world.setBlock(new BlockPos(center.getX()+i, CITY_BOTTOM_Y+1, center.getZ()),
                Blocks.COBBLESTONE.defaultBlockState(), 3);
            // North-South street
            world.setBlock(new BlockPos(center.getX(), CITY_BOTTOM_Y+1, center.getZ()+i),
                Blocks.COBBLESTONE.defaultBlockState(), 3);
        }
        // Secondary streets every 20 blocks
        for (int street = -80; street <= 80; street += 20) {
            for (int i = -CITY_SIZE; i <= CITY_SIZE; i++) {
                world.setBlock(new BlockPos(center.getX()+i, CITY_BOTTOM_Y+1, center.getZ()+street),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
                world.setBlock(new BlockPos(center.getX()+street, CITY_BOTTOM_Y+1, center.getZ()+i),
                    Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
        }
    }

    private static void placeBuildings(ServerLevel world, BlockPos center, Random rng) {
        // Place buildings in a grid pattern
        for (int bx = -4; bx <= 4; bx++) {
            for (int bz = -4; bz <= 4; bz++) {
                if (bx == 0 && bz == 0) continue; // skip center plaza
                int wx = center.getX() + bx * 20 + rng.nextInt(5) - 2;
                int wz = center.getZ() + bz * 20 + rng.nextInt(5) - 2;
                int h = 4 + rng.nextInt(8);
                int w = 5 + rng.nextInt(5);
                buildUndergroundBuilding(world, new BlockPos(wx, CITY_BOTTOM_Y+1, wz), w, h, rng);
            }
        }
    }

    private static void buildUndergroundBuilding(ServerLevel world, BlockPos base, int w, int h, Random rng) {
        for (int x = 0; x <= w; x++) {
            for (int z = 0; z <= w; z++) {
                for (int y = 0; y <= h; y++) {
                    boolean isShell = x==0||x==w||z==0||z==w||y==h;
                    if (isShell) {
                        world.setBlock(base.offset(x, y, z),
                            rng.nextFloat() < 0.3f ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                                                  : Blocks.STONE_BRICKS.defaultBlockState(), 3);
                    } else {
                        world.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        // Door
        world.setBlock(base.offset(w/2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(base.offset(w/2, 2, 0), Blocks.AIR.defaultBlockState(), 3);
        // Window
        world.setBlock(base.offset(1, 2, 0), Blocks.GLASS_PANE.defaultBlockState(), 3);
        // Lantern inside
        world.setBlock(base.offset(w/2, h-1, w/2), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void buildCentralPlaza(ServerLevel world, BlockPos center, Random rng) {
        int plaza = 15;
        for (int x = -plaza; x <= plaza; x++) {
            for (int z = -plaza; z <= plaza; z++) {
                double dist = Math.sqrt(x*x + z*z);
                if (dist <= plaza) {
                    world.setBlock(new BlockPos(center.getX()+x, CITY_BOTTOM_Y+1, center.getZ()+z),
                        dist < 3 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                                 : Blocks.STONE_BRICKS.defaultBlockState(), 3);
                }
            }
        }
        // Dry fountain at center
        world.setBlock(new BlockPos(center.getX(), CITY_BOTTOM_Y+2, center.getZ()),
            Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), 3);
        world.setBlock(new BlockPos(center.getX(), CITY_BOTTOM_Y+3, center.getZ()),
            Blocks.AMETHYST_BLOCK.defaultBlockState(), 3);
    }

    private static void placeCrystalLights(ServerLevel world, BlockPos center, Random rng) {
        // Amethyst clusters as lights on ceiling and walls
        for (int i = 0; i < 30; i++) {
            int lx = rng.nextInt(CITY_SIZE * 2) - CITY_SIZE;
            int lz = rng.nextInt(CITY_SIZE * 2) - CITY_SIZE;
            world.setBlock(new BlockPos(center.getX()+lx, CITY_TOP_Y-1, center.getZ()+lz),
                Blocks.GLOWSTONE.defaultBlockState(), 3);
        }
    }
}