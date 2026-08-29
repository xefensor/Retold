package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistent generation identity stored with each Aender chunk.
 */
public record AenderChunkRealityData(boolean stale, boolean deferredRelease, long signature) {
    public static final AenderChunkRealityData STALE = new AenderChunkRealityData(true, false, 0L);

    public static final Codec<AenderChunkRealityData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("stale", false).forGetter(AenderChunkRealityData::stale),
                    Codec.BOOL.optionalFieldOf("deferred_release", false)
                            .forGetter(AenderChunkRealityData::deferredRelease),
                    Codec.LONG.optionalFieldOf("signature", 0L).forGetter(AenderChunkRealityData::signature)
            ).apply(instance, AenderChunkRealityData::new));

    public static AenderChunkRealityData current(long signature) {
        return new AenderChunkRealityData(false, false, signature);
    }

    public static AenderChunkRealityData deferredRelease(long signature) {
        return new AenderChunkRealityData(false, true, signature);
    }
}
