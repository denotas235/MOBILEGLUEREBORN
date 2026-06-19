// MobileGlues - GreedyMesher.java
// Greedy Meshing — merges adjacent identical block faces into single quads
// Reduces per-chunk vertex count up to ~16x on uniform surfaces
// SPDX-License-Identifier: LGPL-2.1-only
package com.nexus.optimization;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Pure-Java greedy meshing algorithm.
 * Groups adjacent identical block faces into the largest possible quads,
 * eliminating redundant vertex data for uniform surfaces.
 */
public final class GreedyMesher {

    /** A merged rectangular face ready for vertex emission. */
    public record MergedQuad(
        int x, int y, int z,
        int width, int height,
        Direction face,
        BlockState block
    ) {}

    // ── Compile tracking (used by SectionCompilerMixin) ───────────────────────
    private static final ConcurrentHashMap<Long, Long> sCompileStart = new ConcurrentHashMap<>();
    private static final AtomicLong totalQuads    = new AtomicLong();
    private static final AtomicLong mergedQuads   = new AtomicLong();

    public static void notifyCompileStart(SectionPos pos) {
        long key = SectionPos.asLong(pos.x(), pos.y(), pos.z());
        sCompileStart.put(key, System.nanoTime());
    }

    public static double elapsedMs(SectionPos pos) {
        long key = SectionPos.asLong(pos.x(), pos.y(), pos.z());
        Long t = sCompileStart.get(key);
        return t == null ? -1.0 : (System.nanoTime() - t) / 1_000_000.0;
    }

    public static long getTotalQuads()  { return totalQuads.get(); }
    public static long getMergedQuads() { return mergedQuads.get(); }

    // ── Greedy mesh algorithm ─────────────────────────────────────────────────

    /**
     * Produces merged quads for one face direction within a 16×16×16 section.
     *
     * @param region     chunk data provider
     * @param sectionPos section origin
     * @param face       which face direction to process
     * @return list of merged quads (may be empty if no exposed faces)
     */
    public List<MergedQuad> mesh(
            RenderSectionRegion region,
            SectionPos sectionPos,
            Direction face) {

        List<MergedQuad> result = new ArrayList<>();
        boolean[][] merged = new boolean[16][16];

        int ox = sectionPos.minBlockX();
        int oy = sectionPos.minBlockY();
        int oz = sectionPos.minBlockZ();

        // U = horizontal sweep axis, V = vertical sweep axis, N = layer normal
        int[] u = uAxis(face), v = vAxis(face), n = nAxis(face);

        for (int layer = 0; layer < 16; layer++) {
            clearGrid(merged);

            for (int j = 0; j < 16; j++) {
                for (int i = 0; i < 16; i++) {
                    if (merged[i][j]) continue;

                    int bx = ox + (n[0] != 0 ? layer : (u[0] != 0 ? i : j));
                    int by = oy + (n[1] != 0 ? layer : (u[1] != 0 ? i : j));
                    int bz = oz + (n[2] != 0 ? layer : (u[2] != 0 ? i : j));

                    BlockState bs = getBlock(region, bx, by, bz);
                    if (bs == null || !canMesh(bs)) continue;
                    if (!isFaceExposed(region, bx, by, bz, face)) continue;

                    totalQuads.incrementAndGet();

                    // Expand in U direction
                    int width = 1;
                    while (i + width < 16 && !merged[i + width][j]) {
                        int nx = bx + u[0]*width, ny = by + u[1]*width, nz = bz + u[2]*width;
                        if (!canMerge(bs, getBlock(region, nx, ny, nz))) break;
                        width++;
                    }

                    // Expand in V direction
                    int height = 1;
                    expand:
                    while (j + height < 16) {
                        for (int k = 0; k < width; k++) {
                            if (merged[i+k][j+height]) break expand;
                            int nx = bx + u[0]*k + v[0]*height;
                            int ny = by + u[1]*k + v[1]*height;
                            int nz = bz + u[2]*k + v[2]*height;
                            if (!canMerge(bs, getBlock(region, nx, ny, nz))) break expand;
                        }
                        height++;
                    }

                    // Mark merged cells
                    for (int dv = 0; dv < height; dv++)
                        for (int du = 0; du < width; du++)
                            merged[i+du][j+dv] = true;

                    mergedQuads.incrementAndGet();
                    result.add(new MergedQuad(bx, by, bz, width, height, face, bs));
                }
            }
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private BlockState getBlock(RenderSectionRegion region, int x, int y, int z) {
        try { return region.getBlockState(new BlockPos(x, y, z)); }
        catch (Exception e) { return null; }
    }

    private boolean canMesh(BlockState bs) {
        return bs != null && !bs.isAir() && !bs.hasBlockEntity() && bs.isSolidRender();
    }

    private boolean canMerge(BlockState a, BlockState b) {
        return b != null && canMesh(a) && canMesh(b) && a.getBlock() == b.getBlock();
    }

    private boolean isFaceExposed(RenderSectionRegion region, int x, int y, int z, Direction d) {
        BlockState nb = getBlock(region, x + d.getStepX(), y + d.getStepY(), z + d.getStepZ());
        return nb == null || !nb.isSolidRender();
    }

    private static void clearGrid(boolean[][] g) { for (boolean[] r : g) Arrays.fill(r, false); }

    private static int[] uAxis(Direction d) {
        return switch (d) { case UP, DOWN, NORTH, SOUTH -> new int[]{1,0,0}; default -> new int[]{0,1,0}; };
    }
    private static int[] vAxis(Direction d) {
        return switch (d) { case UP, DOWN -> new int[]{0,0,1}; case NORTH, SOUTH -> new int[]{0,1,0}; default -> new int[]{0,0,1}; };
    }
    private static int[] nAxis(Direction d) {
        return switch (d) { case UP, DOWN -> new int[]{0,1,0}; case NORTH, SOUTH -> new int[]{0,0,1}; default -> new int[]{1,0,0}; };
    }
}
