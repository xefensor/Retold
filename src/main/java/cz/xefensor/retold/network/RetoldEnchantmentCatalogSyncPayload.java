package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.enchanting.RetoldEnchantmentSpellDefinition;
import cz.xefensor.retold.enchanting.RetoldEnchantmentWord;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record RetoldEnchantmentCatalogSyncPayload(
        List<RetoldEnchantmentSpellDefinition> definitions
) implements CustomPacketPayload {
    private static final int MAX_SPELL_DEFINITIONS = 4_096;

    public static final CustomPacketPayload.Type<RetoldEnchantmentCatalogSyncPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "enchantment_catalog_sync")
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

    private static final StreamCodec<ByteBuf, RetoldEnchantmentSpellDefinition> DEFINITION_CODEC =
            StreamCodec.composite(
                    IDENTIFIER_STRING_CODEC,
                    RetoldEnchantmentSpellDefinition::enchantment,
                    WORD_CODEC,
                    RetoldEnchantmentSpellDefinition::word,
                    RetoldEnchantmentSpellDefinition::new
            );

    private static final StreamCodec<ByteBuf, List<RetoldEnchantmentSpellDefinition>>
            DEFINITION_LIST_CODEC = ByteBufCodecs.collection(
                    ArrayList::new,
                    DEFINITION_CODEC,
                    MAX_SPELL_DEFINITIONS
            );

    public static final StreamCodec<ByteBuf, RetoldEnchantmentCatalogSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    DEFINITION_LIST_CODEC,
                    RetoldEnchantmentCatalogSyncPayload::definitions,
                    RetoldEnchantmentCatalogSyncPayload::new
            );

    public RetoldEnchantmentCatalogSyncPayload(
            Collection<RetoldEnchantmentSpellDefinition> definitions
    ) {
        this(definitions.stream()
                .sorted(Comparator.comparing(RetoldEnchantmentSpellDefinition::enchantment))
                .toList());
    }

    public RetoldEnchantmentCatalogSyncPayload {
        definitions = definitions.stream()
                .sorted(Comparator.comparing(RetoldEnchantmentSpellDefinition::enchantment))
                .toList();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
