package cz.xefensor.retold.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Creates the Aender portal sprite from the installed Nether portal texture at
 * resource-load time. Retold therefore preserves the vanilla animation without
 * distributing a copied or recolored Minecraft texture.
 */
public record AenderPortalSpriteSource(Identifier source, Identifier target) implements SpriteSource {
    public static final MapCodec<AenderPortalSpriteSource> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("source").forGetter(AenderPortalSpriteSource::source),
                    Identifier.CODEC.fieldOf("target").forGetter(AenderPortalSpriteSource::target)
            ).apply(instance, AenderPortalSpriteSource::new)
    );

    @Override
    public void run(ResourceManager resourceManager, Output output) {
        Identifier sourceFile = TEXTURE_ID_CONVERTER.idToFile(source);
        Optional<Resource> resource = resourceManager.getResource(sourceFile);

        if (resource.isEmpty()) {
            Retold.LOGGER.warn("Could not create Aender portal sprite because {} is missing", sourceFile);
            return;
        }

        output.add(target, loader -> loadRecoloredSprite(resource.get()));
    }

    private @Nullable SpriteContents loadRecoloredSprite(Resource resource) {
        Optional<AnimationMetadataSection> animation;
        Optional<TextureMetadataSection> texture;

        try {
            ResourceMetadata metadata = resource.metadata();
            animation = metadata.getSection(AnimationMetadataSection.TYPE);
            texture = metadata.getSection(TextureMetadataSection.TYPE);
        } catch (Exception exception) {
            Retold.LOGGER.error("Could not read Aender portal source metadata", exception);
            return null;
        }

        NativeImage sourceImage;

        try (InputStream input = resource.open()) {
            sourceImage = NativeImage.read(input);
        } catch (IOException exception) {
            Retold.LOGGER.error("Could not read Aender portal source texture", exception);
            return null;
        }

        NativeImage recolored = sourceImage.mappedCopy(AenderPortalSpriteSource::toAenderGreen);
        sourceImage.close();

        FrameSize frameSize = animation
                .map(section -> section.calculateFrameSize(recolored.getWidth(), recolored.getHeight()))
                .orElseGet(() -> new FrameSize(recolored.getWidth(), recolored.getHeight()));

        if (!Mth.isMultipleOf(recolored.getWidth(), frameSize.width())
                || !Mth.isMultipleOf(recolored.getHeight(), frameSize.height())) {
            Retold.LOGGER.error(
                    "Aender portal source size {},{} is not a multiple of frame size {},{}",
                    recolored.getWidth(),
                    recolored.getHeight(),
                    frameSize.width(),
                    frameSize.height()
            );
            recolored.close();
            return null;
        }

        return new SpriteContents(
                target,
                frameSize,
                recolored,
                animation,
                List.of(),
                texture
        );
    }

    private static int toAenderGreen(int pixel) {
        int intensity = Math.max(ARGB.red(pixel), Math.max(ARGB.green(pixel), ARGB.blue(pixel)));
        int red = intensity * 140 / 255;
        int green = Math.min(255, intensity * 325 / 255);
        int blue = intensity * 180 / 255;
        return ARGB.color(ARGB.alpha(pixel), red, green, blue);
    }

    @Override
    public MapCodec<AenderPortalSpriteSource> codec() {
        return MAP_CODEC;
    }
}
