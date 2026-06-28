package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldTeachingPreviewPayload(
        boolean active,
        String label,
        String tooltip
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "teaching_preview")
    );

    public static final StreamCodec<ByteBuf, RetoldTeachingPreviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    RetoldTeachingPreviewPayload::active,
                    ByteBufCodecs.STRING_UTF8,
                    RetoldTeachingPreviewPayload::label,
                    ByteBufCodecs.STRING_UTF8,
                    RetoldTeachingPreviewPayload::tooltip,
                    RetoldTeachingPreviewPayload::new
            );

    @Override
    public Type type() {
        return TYPE;
    }
}