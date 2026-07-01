package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RetoldAenderChunkData(
        int stabilizerCount,
        int appliedRegionVersion,
        boolean regenerateOnNextLoad,
        long regenerationSalt,
        int regenerationRegionVersion
) {
    public static final RetoldAenderChunkData EMPTY =
            new RetoldAenderChunkData(0, 0, false, 0L, 0);

    public static final Codec<RetoldAenderChunkData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT
                            .optionalFieldOf("stabilizer_count", 0)
                            .forGetter(RetoldAenderChunkData::stabilizerCount),

                    Codec.INT
                            .optionalFieldOf("applied_region_version", 0)
                            .forGetter(RetoldAenderChunkData::appliedRegionVersion),

                    Codec.BOOL
                            .optionalFieldOf("regenerate_on_next_load", false)
                            .forGetter(RetoldAenderChunkData::regenerateOnNextLoad),

                    Codec.LONG
                            .optionalFieldOf("regeneration_salt", 0L)
                            .forGetter(RetoldAenderChunkData::regenerationSalt),

                    Codec.INT
                            .optionalFieldOf("regeneration_region_version", 0)
                            .forGetter(RetoldAenderChunkData::regenerationRegionVersion)
            ).apply(instance, RetoldAenderChunkData::new));

    public RetoldAenderChunkData {
        stabilizerCount = Math.max(0, stabilizerCount);
        appliedRegionVersion = Math.max(0, appliedRegionVersion);
        regenerationRegionVersion = Math.max(0, regenerationRegionVersion);

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
                appliedRegionVersion,
                false,
                regenerationSalt,
                regenerationRegionVersion
        );
    }

    public RetoldAenderChunkData withRemovedStabilizer() {
        if (stabilizerCount <= 0) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount - 1,
                appliedRegionVersion,
                regenerateOnNextLoad,
                regenerationSalt,
                regenerationRegionVersion
        );
    }

    public RetoldAenderChunkData withRegenerationScheduled(
            long salt,
            int targetRegionVersion
    ) {
        if (isStabilized()) {
            return this;
        }

        if (targetRegionVersion <= appliedRegionVersion) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount,
                appliedRegionVersion,
                true,
                salt,
                targetRegionVersion
        );
    }

    public RetoldAenderChunkData withRegenerationFinished() {
        int finishedVersion =
                Math.max(appliedRegionVersion, regenerationRegionVersion);

        return new RetoldAenderChunkData(
                stabilizerCount,
                finishedVersion,
                false,
                regenerationSalt,
                finishedVersion
        );
    }
}