package com.deno.maliworld.worldgen.noise;

/**
 * Pure-Java 2D/3D Simplex Noise (no external deps).
 * Based on Stefan Gustavson's implementation.
 * Returns values in [-1, 1].
 */
public final class SimplexNoise {

    private static final int[] P = {
        151,160,137,91,90,15,131,13,201,95,96,53,194,233,7,225,
        140,36,103,30,69,142,8,99,37,240,21,10,23,190,6,148,247,
        120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,
        33,88,237,149,56,87,174,20,125,136,171,168,68,175,74,165,71,
        134,139,48,27,166,77,146,158,231,83,111,229,122,60,211,133,230,
        220,105,92,41,55,46,245,40,244,102,143,54,65,25,63,161,1,216,
        80,73,209,76,132,187,208,89,18,169,200,196,135,130,116,188,159,
        86,164,100,109,198,173,186,3,64,52,217,226,250,124,123,5,202,
        38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,
        189,28,42,223,183,170,213,119,248,152,2,44,154,163,70,221,153,
        101,155,167,43,172,9,129,22,39,253,19,98,108,110,79,113,224,
        232,178,185,112,104,218,246,97,228,251,34,242,193,238,210,144,
        12,191,179,162,241,81,51,145,235,249,14,239,107,49,192,214,31,
        181,199,106,157,184,84,204,176,115,121,50,45,127,4,150,254,138,
        236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,
        156,180
    };

    private static final int[] PERM     = new int[512];
    private static final int[] PERM12   = new int[512];
    private static final int[][] GRAD3 = {
        {1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},
        {1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
        {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}
    };
    private static final int[] SIMPLEX = {
        0,1,2,3,0,1,3,2,0,0,0,0,0,2,3,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2,3,0,
        0,2,1,3,0,0,0,0,0,3,1,2,0,3,2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,3,2,0,
        0,0,0,0,0,0,0,0,1,2,0,3,0,0,0,0,1,3,0,2,0,0,0,0,0,0,0,0,0,0,0,0,
        2,3,0,1,0,0,0,0,2,0,3,1,0,0,0,0,3,1,0,2,3,2,0,1,0,0,0,0,0,0,0,0,
        2,0,1,3,0,0,0,0,0,0,0,0,0,0,0,0,3,0,1,2,3,0,2,1,0,0,0,0,3,1,2,0,
        2,1,0,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        2,1,3,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        3,2,1,0,2,3,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,3,2,0,1,3,0,1,2
    };

    static {
        for (int i = 0; i < 256; i++) {
            PERM[i] = PERM[i+256] = P[i];
            PERM12[i] = PERM12[i+256] = P[i] % 12;
        }
    }

    private SimplexNoise() {}

    private static double dot(int[] g, double x, double y) {
        return g[0]*x + g[1]*y;
    }
    private static double dot(int[] g, double x, double y, double z) {
        return g[0]*x + g[1]*y + g[2]*z;
    }

    /** 2D Simplex Noise. Returns [-1, 1]. */
    public static double noise(double xin, double yin) {
        final double F2 = 0.5*(Math.sqrt(3.0)-1.0);
        final double G2 = (3.0-Math.sqrt(3.0))/6.0;
        double n0, n1, n2;
        double s = (xin+yin)*F2;
        int i = fastFloor(xin+s), j = fastFloor(yin+s);
        double t = (i+j)*G2;
        double X0=i-t, Y0=j-t;
        double x0=xin-X0, y0=yin-Y0;
        int i1, j1;
        if (x0>y0){i1=1;j1=0;} else {i1=0;j1=1;}
        double x1=x0-i1+G2, y1=y0-j1+G2;
        double x2=x0-1.0+2.0*G2, y2=y0-1.0+2.0*G2;
        int ii=i&255, jj=j&255;
        int gi0=PERM12[ii+PERM[jj]];
        int gi1=PERM12[ii+i1+PERM[jj+j1]];
        int gi2=PERM12[ii+1+PERM[jj+1]];
        double t0=0.5-x0*x0-y0*y0;
        if(t0<0)n0=0; else{t0*=t0;n0=t0*t0*dot(GRAD3[gi0],x0,y0);}
        double t1=0.5-x1*x1-y1*y1;
        if(t1<0)n1=0; else{t1*=t1;n1=t1*t1*dot(GRAD3[gi1],x1,y1);}
        double t2=0.5-x2*x2-y2*y2;
        if(t2<0)n2=0; else{t2*=t2;n2=t2*t2*dot(GRAD3[gi2],x2,y2);}
        return 70.0*(n0+n1+n2);
    }

    /** 3D Simplex Noise. Returns [-1, 1]. */
    public static double noise(double xin, double yin, double zin) {
        final double F3=1.0/3.0, G3=1.0/6.0;
        double s=(xin+yin+zin)*F3;
        int i=fastFloor(xin+s),j=fastFloor(yin+s),k=fastFloor(zin+s);
        double t=(i+j+k)*G3;
        double x0=xin-(i-t),y0=yin-(j-t),z0=zin-(k-t);
        int i1,j1,k1,i2,j2,k2;
        if(x0>=y0){if(y0>=z0){i1=1;j1=0;k1=0;i2=1;j2=1;k2=0;}
        else if(x0>=z0){i1=1;j1=0;k1=0;i2=1;j2=0;k2=1;}
        else{i1=0;j1=0;k1=1;i2=1;j2=0;k2=1;}}
        else{if(y0<z0){i1=0;j1=0;k1=1;i2=0;j2=1;k2=1;}
        else if(x0<z0){i1=0;j1=1;k1=0;i2=0;j2=1;k2=1;}
        else{i1=0;j1=1;k1=0;i2=1;j2=1;k2=0;}}
        double x1=x0-i1+G3,y1=y0-j1+G3,z1=z0-k1+G3;
        double x2=x0-i2+2.0*G3,y2=y0-j2+2.0*G3,z2=z0-k2+2.0*G3;
        double x3=x0-1.0+3.0*G3,y3=y0-1.0+3.0*G3,z3=z0-1.0+3.0*G3;
        int ii=i&255,jj=j&255,kk=k&255;
        int gi0=PERM12[ii+PERM[jj+PERM[kk]]];
        int gi1=PERM12[ii+i1+PERM[jj+j1+PERM[kk+k1]]];
        int gi2=PERM12[ii+i2+PERM[jj+j2+PERM[kk+k2]]];
        int gi3=PERM12[ii+1+PERM[jj+1+PERM[kk+1]]];
        double n0,n1,n2,n3;
        double t0=0.6-x0*x0-y0*y0-z0*z0;
        if(t0<0)n0=0; else{t0*=t0;n0=t0*t0*dot(GRAD3[gi0],x0,y0,z0);}
        double t1=0.6-x1*x1-y1*y1-z1*z1;
        if(t1<0)n1=0; else{t1*=t1;n1=t1*t1*dot(GRAD3[gi1],x1,y1,z1);}
        double t2=0.6-x2*x2-y2*y2-z2*z2;
        if(t2<0)n2=0; else{t2*=t2;n2=t2*t2*dot(GRAD3[gi2],x2,y2,z2);}
        double t3=0.6-x3*x3-y3*y3-z3*z3;
        if(t3<0)n3=0; else{t3*=t3;n3=t3*t3*dot(GRAD3[gi3],x3,y3,z3);}
        return 32.0*(n0+n1+n2+n3);
    }

    private static int fastFloor(double x){return x>0?(int)x:(int)x-1;}
}