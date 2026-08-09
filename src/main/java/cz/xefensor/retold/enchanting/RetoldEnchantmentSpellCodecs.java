package cz.xefensor.retold.enchanting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

final class RetoldEnchantmentSpellCodecs {
    private static final Codec<String> IDENTIFIER_STRING = Identifier.CODEC.xmap(
            Identifier::toString,
            Identifier::parse
    );

    private static final Codec<RetoldEnchantmentWord> WORD = RecordCodecBuilder.create(instance ->
            instance.group(
                    IDENTIFIER_STRING.fieldOf("domain").forGetter(RetoldEnchantmentWord::domain),
                    IDENTIFIER_STRING.fieldOf("effect").forGetter(RetoldEnchantmentWord::effect),
                    IDENTIFIER_STRING.fieldOf("modifier").forGetter(RetoldEnchantmentWord::modifier)
            ).apply(instance, RetoldEnchantmentWord::new)
    );

    static final Codec<RetoldEnchantmentSpellDefinition> DEFINITION =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            IDENTIFIER_STRING.fieldOf("enchantment")
                                    .forGetter(RetoldEnchantmentSpellDefinition::enchantment),
                            WORD.fieldOf("word")
                                    .forGetter(RetoldEnchantmentSpellDefinition::word)
                    ).apply(instance, RetoldEnchantmentSpellDefinition::new)
            );

    private RetoldEnchantmentSpellCodecs() {
    }
}
