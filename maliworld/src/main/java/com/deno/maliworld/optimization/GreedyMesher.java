package com.deno.maliworld.optimization;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Greedy Meshing: combina faces adjacentes identicas num unico quad.
 * Reduz o numero de vertices por chunk em superficies uniformes.
 */
public final class GreedyMesher {

    public record MergedQuad(int x, int y, int z, int width, int height, Direction face, BlockState block) {}

    private static final ConcurrentHashMap<Long, Long> sCompileStart = new ConcurrentHashMap<>();
    private static final AtomicLong totalQuads  = new AtomicLong();
    private static final AtomicLong mergedQuads = new AtomicLong();

    private GreedyMesher() {}

    public static void notifyCompileStart(SectionPos pos) {
        sCompileStart.put(SectionPos.asLong(pos.x(), pos.y(), pos.z()), System.nanoTime());
    }

    public static double elapsedMs(SectionPos pos) {
        Long t = sCompileStart.remove(SectionPos.asLong(pos.x(), pos.y(), pos.z()));
        return t == null ? -1.0 : (System.nanoTime() - t) / 1_000_000.0;
    }

    public static long getTotalQuads()  { return totalQuads.get(); }
    public static long getMergedQuads() { return mergedQuads.get(); }

    public List<MergedQuad> mesh(RenderSectionRegion region, SectionPos sectionPos, Direction face) {
        List<MergedQuad> result = new ArrayList<>();
        boolean[][] merged = new boolean[16][16];
        int ox = sectionPos.minBlockX(), oy = sectionPos.minBlockY(), oz = sectionPos.minBlockZ();
        int[] u = uAxis(face), v = vAxis(face), n = nAxis(face);

        for (int layer = 0; layer < 16; layer++) {
            clearGrid(merged);
            for (int j = 0; j < 16; j++) {
                for (int i = 0; i < 16; i++) {
                    if (merged[i][j]) continue;
                    int bx = ox + (n[0]!=0?layer:(u[0]!=0?i:j));
                    int by = oy + (n[1]!=0?layer:(u[1]!=0?i:j));
                    int bz = oz + (n[2]!=0?layer:(u[2]!=0?i:j));
                    BlockState bs = getBlock(region, bx, by, bz);
                    if (!canMesh(bs) || !isFaceExposed(region, bx, by, bz, face)) continue;
                    totalQuads.incrementAndGet();
                    int width = 1;
                    while (i+width<16 && !merged[i+width][j] && canMerge(bs, getBlock(region,bx+u[0]*width,by+u[1]*width,bz+u[2]*width))) width++;
                    int height = 1;
                    outer: while (j+height<16) {
                        for (int k=0;k<width;k++) { if(merged[i+k][j+height]||!canMerge(bs,getBlock(region,bx+u[0]*k+v[0]*height,by+u[1]*k+v[1]*height,bz+u[2]*k+v[2]*height))) break outer; }
                        height++;
                    }
                    for (int dv=0;dv<height;dv++) for (int du=0;du<width;du++) merged[i+du][j+dv]=true;
                    mergedQuads.incrementAndGet();
                    result.add(new MergedQuad(bx,by,bz,width,height,face,bs));
                }
            }
        }
        return result;
    }

    private BlockState getBlock(RenderSectionRegion r, int x, int y, int z) {
        try { return r.getBlockState(new BlockPos(x,y,z)); } catch (Exception e) { return null; }
    }
    private boolean canMesh(BlockState bs) { return bs!=null && !bs.isAir() && !bs.hasBlockEntity() && bs.canOcclude(); }
    private boolean canMerge(BlockState a, BlockState b) { return b!=null&&canMesh(a)&&canMesh(b)&&a.getBlock()==b.getBlock(); }
    private boolean isFaceExposed(RenderSectionRegion r, int x, int y, int z, Direction d) {
        BlockState nb = getBlock(r,x+d.getStepX(),y+d.getStepY(),z+d.getStepZ()); return nb==null||!nb.canOcclude();
    }
    private static void clearGrid(boolean[][] g) { for (boolean[] row:g) Arrays.fill(row,false); }
    private static int[] uAxis(Direction d) { return switch(d){case UP,DOWN,NORTH,SOUTH->new int[]{1,0,0};default->new int[]{0,1,0};}; }
    private static int[] vAxis(Direction d) { return switch(d){case UP,DOWN->new int[]{0,0,1};case NORTH,SOUTH->new int[]{0,1,0};default->new int[]{0,0,1};}; }
    private static int[] nAxis(Direction d) { return switch(d){case UP,DOWN->new int[]{0,1,0};case NORTH,SOUTH->new int[]{0,0,1};default->new int[]{1,0,0};}; }
}