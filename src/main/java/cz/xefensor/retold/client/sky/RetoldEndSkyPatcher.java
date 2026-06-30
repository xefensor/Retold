package cz.xefensor.retold.client.sky;

import cz.xefensor.retold.Retold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;

public final class RetoldEndSkyPatcher {
    private static long patchedSeed = Long.MIN_VALUE;
    private static SkyRenderer patchedRenderer = null;
    private static boolean needsPatch = true;
    private static boolean patchingDisabled = false;

    private RetoldEndSkyPatcher() {
    }

    public static void markNeedsPatch() {
        needsPatch = true;
    }

    public static void patchSkyRendererTexture(long seed) {
        if (patchingDisabled) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (minecraft.level.dimension() != Level.END) {
            return;
        }

        AbstractTexture texture = RetoldGeneratedEndSkyTexture.getCurrentTexture();

        if (texture == null) {
            return;
        }

        SkyRenderer skyRenderer = findSkyRenderer(minecraft);

        if (skyRenderer == null) {
            return;
        }

        if (!needsPatch && patchedSeed == seed && patchedRenderer == skyRenderer) {
            return;
        }

        try {
            Field endSkyTextureField = SkyRenderer.class.getDeclaredField("endSkyTexture");
            endSkyTextureField.setAccessible(true);
            endSkyTextureField.set(skyRenderer, texture);

            patchedSeed = seed;
            patchedRenderer = skyRenderer;
            needsPatch = false;

            Retold.LOGGER.debug(
                    "Patched End sky texture with seed {}",
                    seed
            );
        } catch (ReflectiveOperationException exception) {
            patchingDisabled = true;

            Retold.LOGGER.warn(
                    "Could not patch End sky texture. Disabling Retold End sky patcher.",
                    exception
            );
        }
    }

    private static SkyRenderer findSkyRenderer(Minecraft minecraft) {
        if (minecraft.levelRenderer == null) {
            return null;
        }

        for (Field field : minecraft.levelRenderer.getClass().getDeclaredFields()) {
            if (field.getType() != SkyRenderer.class) {
                continue;
            }

            try {
                field.setAccessible(true);

                Object value = field.get(minecraft.levelRenderer);

                if (value instanceof SkyRenderer skyRenderer) {
                    return skyRenderer;
                }
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }

        return null;
    }
}