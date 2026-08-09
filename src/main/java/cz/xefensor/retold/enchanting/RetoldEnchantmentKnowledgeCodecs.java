package cz.xefensor.retold.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

final class RetoldEnchantmentKnowledgeCodecs {
    private static final Codec<String> IDENTIFIER_STRING = Identifier.CODEC.xmap(
            Identifier::toString,
            Identifier::parse
    );

    private static final Codec<RetoldEnchantmentKnowledgeStore.PlayerKnowledge> PLAYER =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            UUIDUtil.STRING_CODEC.fieldOf("player")
                                    .forGetter(RetoldEnchantmentKnowledgeStore.PlayerKnowledge::playerId),
                            IDENTIFIER_STRING.listOf().fieldOf("enchantments")
                                    .forGetter(RetoldEnchantmentKnowledgeStore.PlayerKnowledge::enchantments)
                    ).apply(instance, RetoldEnchantmentKnowledgeStore.PlayerKnowledge::new)
            );

    static final Codec<RetoldEnchantmentKnowledgeStore.SerializedState> STATE =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.INT.fieldOf("version")
                                    .forGetter(RetoldEnchantmentKnowledgeStore.SerializedState::version),
                            PLAYER.listOf().fieldOf("players")
                                    .forGetter(RetoldEnchantmentKnowledgeStore.SerializedState::players)
                    ).apply(instance, RetoldEnchantmentKnowledgeStore.SerializedState::new)
            );

    private RetoldEnchantmentKnowledgeCodecs() {
    }
}
