package cz.xefensor.retold.client.sky;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;

import java.lang.reflect.Field;

public final class RetoldEndSkyPatcher {
    private static long patchedSeed = Long.MIN_VALUE;

    private RetoldEndSkyPatcher() {
    }

    public static void patchSkyRendererTexture(long seed) {
        if (patchedSeed == seed) {
            return;
        }

        AbstractTexture texture = RetoldGeneratedEndSkyTexture.getCurrentTexture();

        if (texture == null) {
            return;
        }

        SkyRenderer skyRenderer = findSkyRenderer();

        if (skyRenderer == null) {
            System.out.println("[Retold Debug] Could not find SkyRenderer instance.");
            return;
        }

        try {
            Field endSkyTextureField = SkyRenderer.class.getDeclaredField("endSkyTexture");
            endSkyTextureField.setAccessible(true);
            endSkyTextureField.set(skyRenderer, texture);

            patchedSeed = seed;

            System.out.println("[Retold Debug] Patched SkyRenderer endSkyTexture with seed: " + seed);
        } catch (ReflectiveOperationException exception) {
            System.out.println("[Retold Debug] Failed to patch SkyRenderer endSkyTexture.");
            exception.printStackTrace();
        }
    }

    private static SkyRenderer findSkyRenderer() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.levelRenderer == null) {
            return null;
        }

        for (Field field : minecraft.levelRenderer.getClass().getDeclaredFields()) {
            if (field.getType() == SkyRenderer.class) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(minecraft.levelRenderer);

                    if (value instanceof SkyRenderer skyRenderer) {
                        return skyRenderer;
                    }
                } catch (IllegalAccessException exception) {
                    exception.printStackTrace();
                }
            }
        }

        return null;
    }
}