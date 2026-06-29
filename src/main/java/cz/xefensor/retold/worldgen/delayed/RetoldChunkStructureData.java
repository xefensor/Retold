package cz.xefensor.retold.worldgen.delayed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record RetoldChunkStructureData(
        int playerEditCount,
        Set<String> checkedStructures
) {
    public static final int EDITED_THRESHOLD = 16;

    public static final RetoldChunkStructureData EMPTY =
            new RetoldChunkStructureData(0, Set.of());

    public static final Codec<RetoldChunkStructureData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("player_edit_count", 0)
                            .forGetter(RetoldChunkStructureData::playerEditCount),

                    Codec.STRING.listOf()
                            .optionalFieldOf("checked_structures", List.of())
                            .forGetter(data -> List.copyOf(data.checkedStructures()))
            ).apply(instance, (editCount, checkedList) ->
                    new RetoldChunkStructureData(editCount, new HashSet<>(checkedList))
            ));

    public RetoldChunkStructureData {
        playerEditCount = Math.max(0, Math.min(playerEditCount, EDITED_THRESHOLD));
        checkedStructures = Set.copyOf(checkedStructures);
    }

    public boolean isEditedByPlayer() {
        return playerEditCount >= EDITED_THRESHOLD;
    }

    public RetoldChunkStructureData withPlayerEdit() {
        if (playerEditCount >= EDITED_THRESHOLD) {
            return this;
        }

        return new RetoldChunkStructureData(
                playerEditCount + 1,
                checkedStructures
        );
    }

    public boolean hasChecked(String structureId) {
        return checkedStructures.contains(structureId);
    }

    public RetoldChunkStructureData withChecked(String structureId) {
        HashSet<String> copy = new HashSet<>(checkedStructures);
        copy.add(structureId);

        return new RetoldChunkStructureData(playerEditCount, copy);
    }
}