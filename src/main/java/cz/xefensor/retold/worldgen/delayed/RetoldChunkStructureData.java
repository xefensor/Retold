package cz.xefensor.retold.worldgen.delayed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record RetoldChunkStructureData(
        int playerEditCount,
        Set<String> checkedStructures,
        Set<String> deferredStructures
) {
    public static final int EDITED_THRESHOLD = 16;

    public static final RetoldChunkStructureData EMPTY =
            new RetoldChunkStructureData(0, Set.of(), Set.of());

    public static final Codec<RetoldChunkStructureData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("player_edit_count", 0)
                            .forGetter(RetoldChunkStructureData::playerEditCount),

                    Codec.STRING.listOf()
                            .optionalFieldOf("checked_structures", List.of())
                            .forGetter(data -> List.copyOf(data.checkedStructures())),

                    Codec.STRING.listOf()
                            .optionalFieldOf("deferred_structures", List.of())
                            .forGetter(data -> List.copyOf(data.deferredStructures()))
            ).apply(instance, (editCount, checkedList, deferredList) ->
                    new RetoldChunkStructureData(
                            editCount,
                            new HashSet<>(checkedList),
                            new HashSet<>(deferredList)
                    )
            ));

    public RetoldChunkStructureData {
        playerEditCount = Math.max(0, Math.min(playerEditCount, EDITED_THRESHOLD));
        checkedStructures = Set.copyOf(checkedStructures);
        deferredStructures = Set.copyOf(deferredStructures);
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
                checkedStructures,
                deferredStructures
        );
    }

    public boolean hasChecked(String structureId) {
        return checkedStructures.contains(structureId);
    }

    public RetoldChunkStructureData withChecked(String structureId) {
        HashSet<String> copy = new HashSet<>(checkedStructures);
        copy.add(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                copy,
                deferredStructures
        );
    }

    public boolean hasDeferred(String structureId) {
        return deferredStructures.contains(structureId);
    }

    public RetoldChunkStructureData withDeferred(String structureId) {
        if (deferredStructures.contains(structureId)) {
            return this;
        }

        HashSet<String> copy = new HashSet<>(deferredStructures);
        copy.add(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                checkedStructures,
                copy
        );
    }

    public RetoldChunkStructureData withoutDeferred(String structureId) {
        if (!deferredStructures.contains(structureId)) {
            return this;
        }

        HashSet<String> copy = new HashSet<>(deferredStructures);
        copy.remove(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                checkedStructures,
                copy
        );
    }

    public boolean hasAnyDeferredStructures() {
        return !deferredStructures.isEmpty();
    }
}