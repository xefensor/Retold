package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.enchanting.RetoldEnchantmentWord;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** A client request to cast one semantic word through the active enchanting menu. */
public record RetoldEnchantingCastPayload(
        int containerId,
        RetoldEnchantmentWord word,
        int level
) implements CustomPacketPayload {
    public static final Type<RetoldEnchantingCastPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "enchanting_cast")
    );

    private static final StreamCodec<ByteBuf, String> IDENTIFIER_STRING_CODEC =
            Identifier.STREAM_CODEC.map(Identifier::toString, Identifier::parse);

    private static final StreamCodec<ByteBuf, RetoldEnchantmentWord> WORD_CODEC =
            StreamCodec.composite(
                    IDENTIFIER_STRING_CODEC,
                    RetoldEnchantmentWord::domain,
                    IDENTIFIER_STRING_CODEC,
                    RetoldEnchantmentWord::effect,
                    IDENTIFIER_STRING_CODEC,
                    RetoldEnchantmentWord::modifier,
                    RetoldEnchantmentWord::new
            );

    public static final StreamCodec<ByteBuf, RetoldEnchantingCastPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    RetoldEnchantingCastPayload::containerId,
                    WORD_CODEC,
                    RetoldEnchantingCastPayload::word,
                    ByteBufCodecs.VAR_INT,
                    RetoldEnchantingCastPayload::level,
                    RetoldEnchantingCastPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
