package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldEndSkySeedSyncPayload(long seed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RetoldEndSkySeedSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "end_sky_seed_sync")
            );

    public static final StreamCodec<ByteBuf, RetoldEndSkySeedSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    RetoldEndSkySeedSyncPayload::seed,
                    RetoldEndSkySeedSyncPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}