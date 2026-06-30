package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RetoldAenderChunkData(
        int stabilizerCount
) {
    public static final RetoldAenderChunkData EMPTY =
            new RetoldAenderChunkData(0);

    public static final Codec<RetoldAenderChunkData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT
                            .optionalFieldOf("stabilizer_count", 0)
                            .forGetter(RetoldAenderChunkData::stabilizerCount)
            ).apply(instance, RetoldAenderChunkData::new));

    public RetoldAenderChunkData {
        stabilizerCount = Math.max(0, stabilizerCount);
    }

    public boolean isStabilized() {
        return stabilizerCount > 0;
    }

    public RetoldAenderChunkData withAddedStabilizer() {
        return new RetoldAenderChunkData(stabilizerCount + 1);
    }

    public RetoldAenderChunkData withRemovedStabilizer() {
        if (stabilizerCount <= 0) {
            return this;
        }

        return new RetoldAenderChunkData(stabilizerCount - 1);
    }
}