package cz.xefensor.retold.client.sky;

import com.mojang.blaze3d.platform.NativeImage;
import cz.xefensor.retold.Retold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Random;

public final class RetoldGeneratedEndSkyTexture {
    private static final int SIZE = 256;

    private RetoldGeneratedEndSkyTexture() {
    }

    public static Identifier generateAndRegister(long seed) {
        NativeImage image = new NativeImage(SIZE, SIZE, false);
        Random random = new Random(seed);

        int baseR = 8 + random.nextInt(12);
        int baseG = 4 + random.nextInt(8);
        int baseB = 20 + random.nextInt(25);

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int noise = randomNoise(seed, x, y);

                int r = clamp(baseR + noise / 7);
                int g = clamp(baseG + noise / 10);
                int b = clamp(baseB + noise / 4);

                image.setPixel(x, y, rgba(r, g, b, 255));
            }
        }

        addBabelLines(image, seed);
        addVoidStars(image, seed);

        DynamicTexture texture = new DynamicTexture(
                () -> "Retold generated End sky",
                image
        );

        texture.upload();

        Identifier id = Identifier.fromNamespaceAndPath(
                Retold.MODID,
                "generated/end_sky"
        );

        Minecraft.getInstance().getTextureManager().register(id, texture);

        return id;
    }

    private static void addBabelLines(NativeImage image, long seed) {
        Random random = new Random(seed ^ 0xBABEL);

        for (int i = 0; i < 90; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);
            int height = 8 + random.nextInt(70);
            int width = 1 + random.nextInt(3);

            int r = 40 + random.nextInt(50);
            int g = 25 + random.nextInt(40);
            int b = 80 + random.nextInt(80);

            for (int yy = y; yy < Math.min(SIZE, y + height); yy++) {
                for (int xx = x; xx < Math.min(SIZE, x + width); xx++) {
                    image.setPixel(xx, yy, rgba(r, g, b, 190));
                }
            }
        }
    }

    private static void addVoidStars(NativeImage image, long seed) {
        Random random = new Random(seed ^ 0x51A2F00DL);

        for (int i = 0; i < 350; i++) {
            int x = random.nextInt(SIZE);
            int y = random.nextInt(SIZE);

            int brightness = 120 + random.nextInt(100);

            image.setPixel(
                    x,
                    y,
                    rgba(brightness, brightness, 255, 255)
            );
        }
    }

    private static int randomNoise(long seed, int x, int y) {
        long value = seed;
        value ^= x * 341873128712L;
        value ^= y * 132897987541L;
        value = (value ^ (value >>> 13)) * 1274126177L;
        value ^= value >>> 16;

        return (int) (value & 31);
    }

    private static int rgba(int r, int g, int b, int a) {
        return (a << 24)
                | (b << 16)
                | (g << 8)
                | r;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}