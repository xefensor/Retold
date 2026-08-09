package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record RetoldEnchantmentKnowledgeSyncPayload(
        List<Identifier> knownEnchantments
) implements CustomPacketPayload {
    private static final int MAX_KNOWN_ENCHANTMENTS = 4_096;

    public static final CustomPacketPayload.Type<RetoldEnchantmentKnowledgeSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "enchantment_knowledge_sync")
            );

    private static final StreamCodec<ByteBuf, List<Identifier>> ENCHANTMENT_LIST_CODEC =
            ByteBufCodecs.collection(
                    ArrayList::new,
                    Identifier.STREAM_CODEC,
                    MAX_KNOWN_ENCHANTMENTS
            );

    public static final StreamCodec<ByteBuf, RetoldEnchantmentKnowledgeSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ENCHANTMENT_LIST_CODEC,
                    RetoldEnchantmentKnowledgeSyncPayload::knownEnchantments,
                    RetoldEnchantmentKnowledgeSyncPayload::new
            );

    public RetoldEnchantmentKnowledgeSyncPayload(Collection<Identifier> knownEnchantments) {
        this(knownEnchantments.stream().sorted().toList());
    }

    public RetoldEnchantmentKnowledgeSyncPayload {
        knownEnchantments = List.copyOf(knownEnchantments);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
