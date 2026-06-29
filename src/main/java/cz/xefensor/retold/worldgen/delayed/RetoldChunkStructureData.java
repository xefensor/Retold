package cz.xefensor.retold.worldgen.delayed;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record RetoldChunkStructureData(
        int playerEditCount,
        Set<String> checkedStructures,
        Set<String> deferredStructures,
        Set<String> mobSuppressedStructures
) {
    public static final int EDITED_THRESHOLD = 16;

    public static final RetoldChunkStructureData EMPTY =
            new RetoldChunkStructureData(0, Set.of(), Set.of(), Set.of());

    public static final Codec<RetoldChunkStructureData> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("player_edit_count", 0)
                            .forGetter(RetoldChunkStructureData::playerEditCount),

                    Codec.STRING.listOf()
                            .optionalFieldOf("checked_structures", List.of())
                            .forGetter(data -> List.copyOf(data.checkedStructures())),

                    Codec.STRING.listOf()
                            .optionalFieldOf("deferred_structures", List.of())
                            .forGetter(data -> List.copyOf(data.deferredStructures())),

                    Codec.STRING.listOf()
                            .optionalFieldOf("mob_suppressed_structures", List.of())
                            .forGetter(data -> List.copyOf(data.mobSuppressedStructures()))
            ).apply(instance, (editCount, checkedList, deferredList, suppressedList) ->
                    new RetoldChunkStructureData(
                            editCount,
                            new HashSet<>(checkedList),
                            new HashSet<>(deferredList),
                            new HashSet<>(suppressedList)
                    )
            ));

    public RetoldChunkStructureData {
        playerEditCount = Math.max(0, Math.min(playerEditCount, EDITED_THRESHOLD));
        checkedStructures = Set.copyOf(checkedStructures);
        deferredStructures = Set.copyOf(deferredStructures);
        mobSuppressedStructures = Set.copyOf(mobSuppressedStructures);
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
                deferredStructures,
                mobSuppressedStructures
        );
    }

    public boolean hasChecked(String structureId) {
        return checkedStructures.contains(structureId);
    }

    public RetoldChunkStructureData withChecked(String structureId) {
        if (checkedStructures.contains(structureId)) {
            return this;
        }

        HashSet<String> copy = new HashSet<>(checkedStructures);
        copy.add(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                copy,
                deferredStructures,
                mobSuppressedStructures
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
                copy,
                mobSuppressedStructures
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
                copy,
                mobSuppressedStructures
        );
    }

    public boolean hasAnyDeferredStructures() {
        return !deferredStructures.isEmpty();
    }

    public boolean hasMobSuppressed(String structureId) {
        return mobSuppressedStructures.contains(structureId);
    }

    public RetoldChunkStructureData withMobSuppressed(String structureId) {
        if (mobSuppressedStructures.contains(structureId)) {
            return this;
        }

        HashSet<String> copy = new HashSet<>(mobSuppressedStructures);
        copy.add(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                checkedStructures,
                deferredStructures,
                copy
        );
    }

    public RetoldChunkStructureData withoutMobSuppressed(String structureId) {
        if (!mobSuppressedStructures.contains(structureId)) {
            return this;
        }

        HashSet<String> copy = new HashSet<>(mobSuppressedStructures);
        copy.remove(structureId);

        return new RetoldChunkStructureData(
                playerEditCount,
                checkedStructures,
                deferredStructures,
                copy
        );
    }
}