package cz.xefensor.retold.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import cz.xefensor.retold.Retold;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public final class RetoldGeneratedHorizonTexture {
    private static final int TEXTURE_SIZE = 64;
    private static final int LEGACY_TEXTURE_HEIGHT = 32;
    private static final int COLOR_CHANNELS = 3;

    /*
     * Developer-supplied legacy-layout RGB samples. The data is decoded directly into a
     * NativeImage and expanded for the modern player model; no packaged image resource exists.
     */
    private static final String SOURCE =
            "H4sIAAAAAAACA9WW2U9TQRTG+8CDUlqBaCSUfa1lKQiJoQTZN0M0QjBKDRCqZREkadAHiQGVyJagD6ZJScXEBNKwPrg9GHz0if8Jv7mnHMa7YCXQtDdfJnNnvrn3N+eembmHh/qXI9MCFaRd4AqUnWx+1u7U1WGMXcTszL4EMT8E1LGGirc91VRCMctP8EXpiVp+k3ThNgb5KfhyCtFtvPAzMOIPcSLFEb8K3oi/214Um/xEfi0jCRXsPDK/SrHJT+Qs+iKxtn/abYkQ8IptZgBfz08uyxFy5lhKssQSsGeYsYRhwC34qRc2mDEE7fQEo/MiavxQaZYVVOCsKkhBSUK9yZFa77iCXsyFumDDLY1ift3z4rz5BUaaCKNCbrXbEG2qW74vju+v+H77Z/eXffsrU9vP+6oLU9EFA2wlymTFFNLMeIjReRGN+KdbyjKtIrw2kUWoOPNSNl96v85NoNyZnQj6Hv5c9AVHuyB0wQAbzKhgIIajbnReRCd/lBRKyr96cf6+K+hpddkvA54M36aHAU91TAddMMAGM4bwcKPzIkr86RYlDVLmuiv9A40LfTVBT9uX6aEfr320Q/5aGP881j7bU40uGGCDGUMo+Lx+tedFFPIfb0caDDU7P47dWfW00C/Z+/5m/0BDY27SUlcd1Jpnfdd/81VPDbpggA1mDBH5oySS0Xlx3vz0D0nYweEOxHbpQe1Uh3OytXSi2QH+psxEwENvemtf3K5CFwyfRjphXn10S5TKWJq1fF7QTKN5FtzrPGDZpcvIbzo40B5kaDQNDurK8DmhkBgFhULI1buVhVRGgV/7L3EafoJX9LiuvL+mxNtyIy75QyFggx/CROIxfzh5UEbCXGH3QUW5bpRtrnUWN8rtrspF08xMWG63KRAwra1hU8LWihLCrRB7INwCj7S3J0R1rYe65HbyKzqBH5wR8kPisSMjQqgQiTKFMDy9jj0QY7NoFBtg3tkJ22ju3B4xv5bTkF9+L/PgvfgWjMoeSAXPQ1Rx2NpSz/0s+MuLn6r53e4Ez6h4PvKHn0/8fIsuYmAP9YKTKzInf7gzjT/gqQtpzzJ5vYKKBBjGpgq1sIH4ZewjPw5K0nxvLX6rWLj9MNBEXXJ7hPwEyfHX4WcwhIj5Nzb+IqTIk4cntb0d7lXSTMXvn3QT5/JgC1pOzR9R/sv8HHxMQcXP+c/YmMLRlBlS5kdJLcwvWpSuk/fP/+CX81Pm5w0QdXn/URk0/HKcZX5VXp2CX7ddf30peAnrm+G6wRqUxZBaft11YcRfr1xG/NT7RLrCbHS+UDAptru7xxFWfSNeJlwJhRhSlefMz/lD5cn8Wk6jds4rlPJ8w6fzEb98JoYXPqVWIED7g5afUFXf5fz4Vd/reAqBgOG/RyDAt3KcjxfvpFu1Lv7J/wfsm8d/ABgAAA==";

    private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(
            Retold.MODID,
            "dynamic/horizon_detail"
    );

    private static DynamicTexture texture;

    private RetoldGeneratedHorizonTexture() {
    }

    public static Identifier get() {
        if (texture == null) {
            NativeImage image = generate();
            texture = new DynamicTexture(() -> "Retold generated horizon detail", image);
            Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, texture);
        }

        return TEXTURE_ID;
    }

    private static NativeImage generate() {
        byte[] pixels = decodeSource();
        int expectedLength = TEXTURE_SIZE * LEGACY_TEXTURE_HEIGHT * COLOR_CHANNELS;

        if (pixels.length != expectedLength) {
            throw new IllegalStateException(
                    "Invalid generated horizon texture data length: " + pixels.length
            );
        }

        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        int sourceIndex = 0;

        for (int y = 0; y < LEGACY_TEXTURE_HEIGHT; y++) {
            for (int x = 0; x < TEXTURE_SIZE; x++) {
                int red = Byte.toUnsignedInt(pixels[sourceIndex++]);
                int green = Byte.toUnsignedInt(pixels[sourceIndex++]);
                int blue = Byte.toUnsignedInt(pixels[sourceIndex++]);
                image.setPixel(x, y, argb(red, green, blue));
            }
        }

        expandLegacyLimbs(image);
        return image;
    }

    private static byte[] decodeSource() {
        byte[] compressed = Base64.getDecoder().decode(SOURCE);

        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to decode generated horizon texture", exception);
        }
    }

    private static void expandLegacyLimbs(NativeImage image) {
        image.fillRect(0, LEGACY_TEXTURE_HEIGHT, TEXTURE_SIZE, LEGACY_TEXTURE_HEIGHT, 0);

        // Mirror the legacy right leg into the modern left-leg slots.
        image.copyRect(4, 16, 16, 32, 4, 4, true, false);
        image.copyRect(8, 16, 16, 32, 4, 4, true, false);
        image.copyRect(0, 20, 24, 32, 4, 12, true, false);
        image.copyRect(4, 20, 16, 32, 4, 12, true, false);
        image.copyRect(8, 20, 8, 32, 4, 12, true, false);
        image.copyRect(12, 20, 16, 32, 4, 12, true, false);

        // Mirror the legacy right arm into the modern left-arm slots.
        image.copyRect(44, 16, -8, 32, 4, 4, true, false);
        image.copyRect(48, 16, -8, 32, 4, 4, true, false);
        image.copyRect(40, 20, 0, 32, 4, 12, true, false);
        image.copyRect(44, 20, -8, 32, 4, 12, true, false);
        image.copyRect(48, 20, -16, 32, 4, 12, true, false);
        image.copyRect(52, 20, -8, 32, 4, 12, true, false);
    }

    private static int argb(int red, int green, int blue) {
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
