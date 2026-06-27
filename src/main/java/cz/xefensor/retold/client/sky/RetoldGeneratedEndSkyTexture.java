package cz.xefensor.retold.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import cz.xefensor.retold.Retold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

public final class RetoldGeneratedEndSkyTexture {
    private static final int SIZE = 256;

    private static DynamicTexture currentTexture;

    private RetoldGeneratedEndSkyTexture() {
    }

    public static Identifier generateAndRegister(long seed) {
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        Random random = new Random(seed);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);

                image.setPixel(x, y, rgba(r, g, b, 255));
            }
        }

        currentTexture = new DynamicTexture(
                () -> "Retold generated End sky",
                image
        );

        currentTexture.upload();

        Identifier id = Identifier.fromNamespaceAndPath(
                Retold.MODID,
                "generated/end_sky"
        );

        Minecraft.getInstance().getTextureManager().register(id, currentTexture);

        return id;
    }

    public static DynamicTexture getCurrentTexture() {
        return currentTexture;
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24)
                | (b << 16)
                | (g << 8)
                | r;
    }
}