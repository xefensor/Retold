package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RetoldLearnRecipePayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "learn_recipe_from_villager")
    );

    public static final StreamCodec<ByteBuf, RetoldLearnRecipePayload> STREAM_CODEC =
            StreamCodec.unit(new RetoldLearnRecipePayload());

    @Override
    public Type type() {
        return TYPE;
    }
}