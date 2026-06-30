package cz.xefensor.retold.client.sky;

import net.minecraft.resources.Identifier;

public final class RetoldClientEndSky {
    private static long seed = 0L;
    private static Identifier texture = null;

    private RetoldClientEndSky() {
    }

    public static long getSeed() {
        return seed;
    }

    public static void setSeed(long newSeed) {
        if (seed == newSeed && texture != null) {
            return;
        }

        seed = newSeed;
        texture = RetoldGeneratedEndSkyTexture.generateAndRegister(newSeed);
        RetoldEndSkyPatcher.markNeedsPatch();
    }

    public static Identifier getTexture() {
        if (texture == null) {
            texture = RetoldGeneratedEndSkyTexture.generateAndRegister(seed);
            RetoldEndSkyPatcher.markNeedsPatch();
        }

        return texture;
    }
}