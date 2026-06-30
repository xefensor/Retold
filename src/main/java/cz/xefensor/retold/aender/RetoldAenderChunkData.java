package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RetoldAenderChunkData(
        int stabilizerCount,
        boolean regenerateOnNextLoad,
        long regenerationSalt
) {
    public static final RetoldAenderChunkData EMPTY =
            new RetoldAenderChunkData(0, false, 0L);

    public static final Codec<RetoldAenderChunkData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT
                            .optionalFieldOf("stabilizer_count", 0)
                            .forGetter(RetoldAenderChunkData::stabilizerCount),

                    Codec.BOOL
                            .optionalFieldOf("regenerate_on_next_load", false)
                            .forGetter(RetoldAenderChunkData::regenerateOnNextLoad),

                    Codec.LONG
                            .optionalFieldOf("regeneration_salt", 0L)
                            .forGetter(RetoldAenderChunkData::regenerationSalt)
            ).apply(instance, RetoldAenderChunkData::new));

    public RetoldAenderChunkData {
        stabilizerCount = Math.max(0, stabilizerCount);

        if (stabilizerCount > 0) {
            regenerateOnNextLoad = false;
        }
    }

    public boolean isStabilized() {
        return stabilizerCount > 0;
    }

    public boolean shouldRegenerateOnNextLoad() {
        return regenerateOnNextLoad && !isStabilized();
    }

    public RetoldAenderChunkData withAddedStabilizer() {
        return new RetoldAenderChunkData(
                stabilizerCount + 1,
                false,
                regenerationSalt
        );
    }

    public RetoldAenderChunkData withRemovedStabilizer() {
        if (stabilizerCount <= 0) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount - 1,
                regenerateOnNextLoad,
                regenerationSalt
        );
    }

    public RetoldAenderChunkData withRegenerationScheduled(long salt) {
        if (isStabilized()) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount,
                true,
                salt
        );
    }

    public RetoldAenderChunkData withRegenerationFinished() {
        return new RetoldAenderChunkData(
                stabilizerCount,
                false,
                regenerationSalt
        );
    }
}