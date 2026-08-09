package cz.xefensor.retold.enchanting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class RetoldEnchantmentKnowledgeStore {
    static final int SAVE_VERSION = 1;

    private final Map<UUID, Set<String>> knownEnchantmentsByPlayer = new HashMap<>();

    RetoldEnchantmentKnowledgeStore() {
    }

    private RetoldEnchantmentKnowledgeStore(SerializedState state) {
        validateSerializedState(state);

        for (PlayerKnowledge playerKnowledge : state.players()) {
            knownEnchantmentsByPlayer.put(
                    playerKnowledge.playerId(),
                    new HashSet<>(playerKnowledge.enchantments())
            );
        }
    }

    static RetoldEnchantmentKnowledgeStore fromSerializedState(SerializedState state) {
        return new RetoldEnchantmentKnowledgeStore(state);
    }

    boolean hasKnown(UUID playerId, String enchantment) {
        Set<String> knownEnchantments = knownEnchantmentsByPlayer.get(playerId);
        return knownEnchantments != null && knownEnchantments.contains(enchantment);
    }

    boolean markKnown(UUID playerId, String enchantment) {
        return markKnown(playerId, Set.of(enchantment));
    }

    boolean markKnown(UUID playerId, Collection<String> enchantments) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(enchantments, "enchantments");

        for (String enchantment : enchantments) {
            Objects.requireNonNull(enchantment, "enchantment");
            if (enchantment.isBlank()) {
                throw new IllegalArgumentException("Known enchantment id must not be blank");
            }
        }

        if (enchantments.isEmpty()) {
            return false;
        }

        return knownEnchantmentsByPlayer
                .computeIfAbsent(playerId, ignored -> new HashSet<>())
                .addAll(enchantments);
    }

    Set<String> knownEnchantments(UUID playerId) {
        Set<String> knownEnchantments = knownEnchantmentsByPlayer.get(playerId);
        return knownEnchantments == null ? Set.of() : Set.copyOf(knownEnchantments);
    }

    SerializedState serialize() {
        List<PlayerKnowledge> players = new ArrayList<>();

        for (Map.Entry<UUID, Set<String>> entry : knownEnchantmentsByPlayer.entrySet()) {
            List<String> enchantments = entry.getValue().stream()
                    .sorted()
                    .toList();
            players.add(new PlayerKnowledge(entry.getKey(), enchantments));
        }

        players.sort(Comparator.comparing(player -> player.playerId().toString()));
        return new SerializedState(SAVE_VERSION, List.copyOf(players));
    }

    static void validateSerializedState(SerializedState state) {
        if (state.version() != SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported enchantment knowledge SavedData version " + state.version()
            );
        }

        Set<UUID> playerIds = new HashSet<>();

        for (PlayerKnowledge player : state.players()) {
            if (!playerIds.add(player.playerId())) {
                throw new IllegalArgumentException(
                        "Duplicate enchantment knowledge entry for player " + player.playerId()
                );
            }

            if (new HashSet<>(player.enchantments()).size() != player.enchantments().size()) {
                throw new IllegalArgumentException(
                        "Duplicate known enchantment for player " + player.playerId()
                );
            }

            if (player.enchantments().stream().anyMatch(String::isBlank)) {
                throw new IllegalArgumentException(
                        "Blank known enchantment id for player " + player.playerId()
                );
            }
        }
    }

    record PlayerKnowledge(UUID playerId, List<String> enchantments) {
        PlayerKnowledge {
            enchantments = List.copyOf(enchantments);
        }
    }

    record SerializedState(int version, List<PlayerKnowledge> players) {
        SerializedState {
            players = List.copyOf(players);
        }
    }
}
