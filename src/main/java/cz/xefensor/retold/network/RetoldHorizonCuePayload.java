package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldHorizonCuePayload(
        long phase,
        int durationTicks
) implements CustomPacketPayload {
    public static final Type<RetoldHorizonCuePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "horizon_ambient")
    );

    public static final StreamCodec<ByteBuf, RetoldHorizonCuePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.LONG,
                    RetoldHorizonCuePayload::phase,
                    ByteBufCodecs.VAR_INT,
                    RetoldHorizonCuePayload::durationTicks,
                    RetoldHorizonCuePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
