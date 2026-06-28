package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldRequestTeachingPreviewPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "request_teaching_preview")
    );

    public static final StreamCodec<ByteBuf, RetoldRequestTeachingPreviewPayload> STREAM_CODEC =
            StreamCodec.unit(new RetoldRequestTeachingPreviewPayload());

    @Override
    public Type type() {
        return TYPE;
    }
}