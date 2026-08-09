package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldTeachingPreviewPayload(
        boolean active,
        Component buttonLabel,
        Component status,
        Component cost,
        Component tooltip,
        Feedback feedback
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "teaching_preview")
    );

    public static final StreamCodec<ByteBuf, RetoldTeachingPreviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    RetoldTeachingPreviewPayload::active,
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                    RetoldTeachingPreviewPayload::buttonLabel,
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                    RetoldTeachingPreviewPayload::status,
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                    RetoldTeachingPreviewPayload::cost,
                    ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC,
                    RetoldTeachingPreviewPayload::tooltip,
                    Feedback.STREAM_CODEC,
                    RetoldTeachingPreviewPayload::feedback,
                    RetoldTeachingPreviewPayload::new
            );

    @Override
    public Type type() {
        return TYPE;
    }

    public enum Feedback {
        NONE,
        SUCCESS,
        REJECTED;

        private static final StreamCodec<ByteBuf, Feedback> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(
                        id -> values()[id],
                        Feedback::ordinal
                );
    }
}
