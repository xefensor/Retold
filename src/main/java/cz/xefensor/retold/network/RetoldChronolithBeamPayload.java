package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RetoldChronolithBeamPayload(
        boolean active,
        UUID playerId,
        BlockPos pos
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RetoldChronolithBeamPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "chronolith_beam")
            );

    public static final StreamCodec<ByteBuf, RetoldChronolithBeamPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL,
                    RetoldChronolithBeamPayload::active,
                    UUIDUtil.STREAM_CODEC,
                    RetoldChronolithBeamPayload::playerId,
                    BlockPos.STREAM_CODEC,
                    RetoldChronolithBeamPayload::pos,
                    RetoldChronolithBeamPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
