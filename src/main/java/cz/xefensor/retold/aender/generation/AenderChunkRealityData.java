package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistent generation identity stored with each Aender chunk.
 */
public record AenderChunkRealityData(boolean stale, long signature) {
    public static final AenderChunkRealityData STALE = new AenderChunkRealityData(true, 0L);

    public static final Codec<AenderChunkRealityData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("stale", false).forGetter(AenderChunkRealityData::stale),
                    Codec.LONG.optionalFieldOf("signature", 0L).forGetter(AenderChunkRealityData::signature)
            ).apply(instance, AenderChunkRealityData::new));

    public static AenderChunkRealityData current(long signature) {
        return new AenderChunkRealityData(false, signature);
    }
}
