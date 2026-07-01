package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RetoldAenderChunkData(
        int stabilizerCount,
        long appliedTerrainRevision,
        boolean regenerateOnNextLoad,
        long targetTerrainRevision
) {
    public static final RetoldAenderChunkData EMPTY =
            new RetoldAenderChunkData(0, 0L, false, 0L);

    public static final Codec<RetoldAenderChunkData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT
                            .optionalFieldOf("stabilizer_count", 0)
                            .forGetter(RetoldAenderChunkData::stabilizerCount),

                    Codec.LONG
                            .optionalFieldOf("applied_terrain_revision", 0L)
                            .forGetter(RetoldAenderChunkData::appliedTerrainRevision),

                    Codec.BOOL
                            .optionalFieldOf("regenerate_on_next_load", false)
                            .forGetter(RetoldAenderChunkData::regenerateOnNextLoad),

                    Codec.LONG
                            .optionalFieldOf("target_terrain_revision", 0L)
                            .forGetter(RetoldAenderChunkData::targetTerrainRevision)
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
                appliedTerrainRevision,
                false,
                targetTerrainRevision
        );
    }

    public RetoldAenderChunkData withRemovedStabilizer() {
        if (stabilizerCount <= 0) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount - 1,
                appliedTerrainRevision,
                regenerateOnNextLoad,
                targetTerrainRevision
        );
    }

    public RetoldAenderChunkData withTerrainRegenerationScheduled(
            long targetRevision
    ) {
        if (isStabilized()) {
            return this;
        }

        if (targetRevision <= 0L) {
            return this;
        }

        if (appliedTerrainRevision == targetRevision
                && !regenerateOnNextLoad) {
            return this;
        }

        return new RetoldAenderChunkData(
                stabilizerCount,
                appliedTerrainRevision,
                true,
                targetRevision
        );
    }

    public RetoldAenderChunkData withTerrainRegenerationFinished() {
        return new RetoldAenderChunkData(
                stabilizerCount,
                targetTerrainRevision,
                false,
                targetTerrainRevision
        );
    }
}