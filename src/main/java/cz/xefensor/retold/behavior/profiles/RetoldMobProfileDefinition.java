package cz.xefensor.retold.behavior.profiles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

public record RetoldMobProfileDefinition(
        Identifier entity,
        RetoldMobProfile profile
) {
    public static final Codec<RetoldMobProfileDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("entity").forGetter(RetoldMobProfileDefinition::entity),
                    RetoldMobProfile.CODEC.fieldOf("profile").forGetter(RetoldMobProfileDefinition::profile)
            ).apply(instance, RetoldMobProfileDefinition::new)
    );
}
