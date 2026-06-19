package com.deno.maliworld.worldgen.surface;

/** Determina qual bloco fica na superfície baseado em contexto. */
public final class SurfaceDecorator {

    private SurfaceDecorator() {}

    public enum SurfaceType {
        GRASS, DIRT, STONE, SAND, GRAVEL, SNOW, ICE, PODZOL, CLAY
    }

    public static SurfaceType getSurface(float temperature, float humidity, float slope, int altitude) {
        if (altitude > 160) return temperature < 0 ? SurfaceType.SNOW : SurfaceType.STONE;
        if (slope > 0.7f)   return SurfaceType.STONE;
        if (temperature < -0.3f && humidity < 0.0f) return SurfaceType.SNOW;
        if (temperature > 0.5f && humidity < -0.3f) return SurfaceType.SAND;
        if (humidity > 0.4f) return SurfaceType.PODZOL;
        return SurfaceType.GRASS;
    }
}
