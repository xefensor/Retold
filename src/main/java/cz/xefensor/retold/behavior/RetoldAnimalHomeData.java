package cz.xefensor.retold.behavior;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record RetoldAnimalHomeData(
        boolean hasHome,
        RetoldAnimalHomeType type,
        Identifier dimension,
        BlockPos pos,
        long createdAt,
        long lastUsedAt
) {
    public static final RetoldAnimalHomeData EMPTY =
            new RetoldAnimalHomeData(
                    false,
                    RetoldAnimalHomeType.NONE,
                    Level.OVERWORLD.identifier(),
                    BlockPos.ZERO,
                    0L,
                    0L
            );

    public static final Codec<RetoldAnimalHomeData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("has_home", false)
                            .forGetter(RetoldAnimalHomeData::hasHome),

                    RetoldAnimalHomeType.CODEC.optionalFieldOf("type", RetoldAnimalHomeType.NONE)
                            .forGetter(RetoldAnimalHomeData::type),

                    Identifier.CODEC.optionalFieldOf("dimension", Level.OVERWORLD.identifier())
                            .forGetter(RetoldAnimalHomeData::dimension),

                    BlockPos.CODEC.optionalFieldOf("pos", BlockPos.ZERO)
                            .forGetter(RetoldAnimalHomeData::pos),

                    Codec.LONG.optionalFieldOf("created_at", 0L)
                            .forGetter(RetoldAnimalHomeData::createdAt),

                    Codec.LONG.optionalFieldOf("last_used_at", 0L)
                            .forGetter(RetoldAnimalHomeData::lastUsedAt)
            ).apply(instance, RetoldAnimalHomeData::new));

    public RetoldAnimalHomeData {
        type = type == null ? RetoldAnimalHomeType.NONE : type;
        dimension = dimension == null ? Level.OVERWORLD.identifier() : dimension;
        pos = pos == null ? BlockPos.ZERO : pos.immutable();

        if (type == RetoldAnimalHomeType.NONE) {
            hasHome = false;
        }

        createdAt = Math.max(0L, createdAt);
        lastUsedAt = Math.max(createdAt, lastUsedAt);
    }

    public boolean shouldSerialize() {
        return hasHome && type != RetoldAnimalHomeType.NONE;
    }

    public RetoldAnimalHomeMemory toMemory() {
        if (!shouldSerialize()) {
            return null;
        }

        return new RetoldAnimalHomeMemory(
                type,
                ResourceKey.create(
                        Registries.DIMENSION,
                        dimension
                ),
                pos,
                createdAt,
                lastUsedAt
        );
    }

    public static RetoldAnimalHomeData fromMemory(RetoldAnimalHomeMemory memory) {
        if (memory == null || memory.type() == RetoldAnimalHomeType.NONE) {
            return EMPTY;
        }

        return new RetoldAnimalHomeData(
                true,
                memory.type(),
                memory.dimension().identifier(),
                memory.pos(),
                memory.createdAt(),
                memory.lastUsedAt()
        );
    }
}
