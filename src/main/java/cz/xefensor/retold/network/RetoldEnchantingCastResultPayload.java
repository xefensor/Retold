package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Reports only whether a server-authoritative cast succeeded, without a failure reason. */
public record RetoldEnchantingCastResultPayload(
        int containerId,
        boolean success
) implements CustomPacketPayload {
    public static final Type<RetoldEnchantingCastResultPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "enchanting_cast_result")
    );

    public static final StreamCodec<ByteBuf, RetoldEnchantingCastResultPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RetoldEnchantingCastResultPayload::containerId,
                    ByteBufCodecs.BOOL,
                    RetoldEnchantingCastResultPayload::success,
                    RetoldEnchantingCastResultPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
