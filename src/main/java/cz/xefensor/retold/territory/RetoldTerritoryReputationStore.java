package cz.xefensor.retold.territory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class RetoldTerritoryReputationStore {
    private final Map<ReputationKey, ReputationEntry> reputation = new HashMap<>();

    private long lastDecayAt = -RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS;
    private long lastSaveRequestedAt = -RetoldTerritoryConstants.REPUTATION_SAVE_INTERVAL_TICKS;

    private boolean legacyJsonMigrated;
    private boolean legacyMigrationConfirmedOnLoad;
    private boolean legacyMigrationChecked;

    RetoldTerritoryReputationStore() {
    }

    private RetoldTerritoryReputationStore(SerializedState state) {
        validateSerializedState(state);

        for (SerializedEntry entry : state.entries()) {
            reputation.put(
                    new ReputationKey(entry.territoryKey(), entry.intruderUuid()),
                    ReputationEntry.from(entry)
            );
        }

        legacyJsonMigrated = state.legacyJsonMigrated();
        legacyMigrationConfirmedOnLoad = legacyJsonMigrated;
    }

    static RetoldTerritoryReputationStore fromSerializedState(SerializedState state) {
        return new RetoldTerritoryReputationStore(state);
    }

    int getSuspicion(String territoryKey, UUID intruderUuid) {
        ReputationEntry entry = reputation.get(new ReputationKey(territoryKey, intruderUuid));
        return entry == null ? 0 : entry.suspicion;
    }

    boolean markSeen(String territoryKey, UUID intruderUuid, long gameTime) {
        ReputationEntry entry = getOrCreateEntry(territoryKey, intruderUuid, gameTime);
        entry.lastSeenAt = gameTime;
        return entry.suspicion > 0;
    }

    boolean markTrespassSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        ReputationEntry entry = getOrCreateEntry(territoryKey, intruderUuid, gameTime);

        if (gameTime - entry.lastTrespassSuspicionAt
                < RetoldTerritoryConstants.REPUTATION_TRESPASS_SUSPICION_COOLDOWN_TICKS) {
            return false;
        }

        entry.lastTrespassSuspicionAt = gameTime;
        return true;
    }

    boolean markVisibleWarningSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        ReputationEntry entry = getOrCreateEntry(territoryKey, intruderUuid, gameTime);

        if (gameTime - entry.lastVisibleWarningSuspicionAt
                < RetoldTerritoryConstants.REPUTATION_VISIBLE_WARNING_SUSPICION_COOLDOWN_TICKS) {
            return false;
        }

        entry.lastVisibleWarningSuspicionAt = gameTime;
        return true;
    }

    boolean markTooCloseSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        ReputationEntry entry = getOrCreateEntry(territoryKey, intruderUuid, gameTime);

        if (gameTime - entry.lastTooCloseSuspicionAt
                < RetoldTerritoryConstants.REPUTATION_TOO_CLOSE_SUSPICION_COOLDOWN_TICKS) {
            return false;
        }

        entry.lastTooCloseSuspicionAt = gameTime;
        return true;
    }

    boolean addSuspicion(String territoryKey, UUID intruderUuid, int amount, long gameTime) {
        if (amount <= 0) {
            return false;
        }

        ReputationEntry entry = getOrCreateEntry(territoryKey, intruderUuid, gameTime);
        int oldSuspicion = entry.suspicion;
        long oldLastUpdatedAt = entry.lastUpdatedAt;

        entry.suspicion = Math.min(
                RetoldTerritoryConstants.REPUTATION_MAX_SUSPICION,
                entry.suspicion + amount
        );
        entry.lastUpdatedAt = gameTime;

        return entry.suspicion != oldSuspicion || entry.lastUpdatedAt != oldLastUpdatedAt;
    }

    boolean tickDecay(long gameTime) {
        if (gameTime - lastDecayAt < RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS) {
            return false;
        }

        lastDecayAt = gameTime;

        boolean changed = false;
        Iterator<Map.Entry<ReputationKey, ReputationEntry>> iterator = reputation.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ReputationKey, ReputationEntry> mapEntry = iterator.next();
            ReputationEntry entry = mapEntry.getValue();

            if (entry.suspicion <= 0) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (gameTime - entry.lastSeenAt
                    <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS) {
                continue;
            }

            entry.suspicion -= RetoldTerritoryConstants.REPUTATION_DECAY_AMOUNT;
            entry.lastUpdatedAt = gameTime;
            changed = true;

            if (entry.suspicion <= 0) {
                iterator.remove();
            }
        }

        return changed;
    }

    boolean shouldRequestSave(long gameTime, boolean dirty) {
        if (!dirty) {
            return false;
        }

        if (gameTime - lastSaveRequestedAt < RetoldTerritoryConstants.REPUTATION_SAVE_INTERVAL_TICKS) {
            return false;
        }

        lastSaveRequestedAt = gameTime;
        return true;
    }

    MostSuspiciousEntry findMostSuspicious(UUID intruderUuid) {
        ReputationKey bestKey = null;
        ReputationEntry bestEntry = null;

        for (Map.Entry<ReputationKey, ReputationEntry> mapEntry : reputation.entrySet()) {
            ReputationKey key = mapEntry.getKey();
            ReputationEntry entry = mapEntry.getValue();

            if (!key.intruderUuid.equals(intruderUuid) || entry.suspicion <= 0) {
                continue;
            }

            if (bestEntry == null || entry.suspicion > bestEntry.suspicion) {
                bestKey = key;
                bestEntry = entry;
            }
        }

        if (bestKey == null || bestEntry == null) {
            return null;
        }

        return new MostSuspiciousEntry(
                bestKey.territoryKey,
                bestEntry.suspicion,
                bestEntry.lastSeenAt
        );
    }

    SerializedEntry snapshot(String territoryKey, UUID intruderUuid) {
        ReputationEntry entry = reputation.get(new ReputationKey(territoryKey, intruderUuid));
        return entry == null ? null : entry.serialize(territoryKey, intruderUuid);
    }

    boolean beginLegacyMigrationCheck() {
        if (legacyMigrationChecked) {
            return false;
        }

        legacyMigrationChecked = true;
        return true;
    }

    boolean isLegacyMigrationConfirmedOnLoad() {
        return legacyMigrationConfirmedOnLoad;
    }

    boolean isLegacyJsonMigrated() {
        return legacyJsonMigrated;
    }

    void importLegacyEntries(List<SerializedEntry> entries) {
        if (!reputation.isEmpty()) {
            throw new IllegalStateException("Cannot migrate legacy territory reputation into non-empty SavedData");
        }

        Map<ReputationKey, ReputationEntry> imported = new HashMap<>();

        for (SerializedEntry entry : entries) {
            validateEntry(entry);
            ReputationKey key = new ReputationKey(entry.territoryKey(), entry.intruderUuid());

            if (imported.put(key, ReputationEntry.from(entry)) != null) {
                throw new IllegalArgumentException("Duplicate legacy territory reputation entry for " + key);
            }
        }

        reputation.putAll(imported);
        legacyJsonMigrated = true;
    }

    SerializedState serialize() {
        List<SerializedEntry> entries = new ArrayList<>();

        for (Map.Entry<ReputationKey, ReputationEntry> mapEntry : reputation.entrySet()) {
            ReputationEntry entry = mapEntry.getValue();

            if (entry.suspicion <= 0) {
                continue;
            }

            entries.add(entry.serialize(mapEntry.getKey().territoryKey, mapEntry.getKey().intruderUuid));
        }

        entries.sort(Comparator
                .comparing(SerializedEntry::territoryKey)
                .thenComparing(entry -> entry.intruderUuid().toString()));

        return new SerializedState(
                RetoldTerritoryConstants.REPUTATION_SAVE_VERSION,
                legacyJsonMigrated,
                List.copyOf(entries)
        );
    }

    static void validateSerializedState(SerializedState state) {
        if (state.version() != RetoldTerritoryConstants.REPUTATION_SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported territory reputation SavedData version " + state.version()
            );
        }

        if (state.entries() == null) {
            throw new IllegalArgumentException("Territory reputation SavedData has no entries list");
        }

        Set<ReputationKey> keys = new HashSet<>();

        for (SerializedEntry entry : state.entries()) {
            validateEntry(entry);
            ReputationKey key = new ReputationKey(entry.territoryKey(), entry.intruderUuid());

            if (!keys.add(key)) {
                throw new IllegalArgumentException("Duplicate territory reputation entry for " + key);
            }
        }
    }

    private static void validateEntry(SerializedEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Territory reputation SavedData contains a null entry");
        }

        if (entry.territoryKey() == null || entry.territoryKey().isBlank()) {
            throw new IllegalArgumentException("Territory reputation entry has no territory key");
        }

        if (entry.intruderUuid() == null) {
            throw new IllegalArgumentException("Territory reputation entry has no intruder UUID");
        }

        if (entry.suspicion() <= 0) {
            throw new IllegalArgumentException("Territory reputation entry has non-positive suspicion");
        }
    }

    private ReputationEntry getOrCreateEntry(String territoryKey, UUID intruderUuid, long gameTime) {
        ReputationEntry entry = reputation.computeIfAbsent(
                new ReputationKey(territoryKey, intruderUuid),
                ignored -> new ReputationEntry()
        );

        if (entry.firstCreatedAt == 0L) {
            entry.firstCreatedAt = gameTime;
        }

        return entry;
    }

    record SerializedEntry(
            String territoryKey,
            UUID intruderUuid,
            int suspicion,
            long firstCreatedAt,
            long lastUpdatedAt,
            long lastSeenAt,
            long lastTrespassSuspicionAt,
            long lastVisibleWarningSuspicionAt,
            long lastTooCloseSuspicionAt
    ) {
    }

    record SerializedState(int version, boolean legacyJsonMigrated, List<SerializedEntry> entries) {
    }

    record MostSuspiciousEntry(String territoryKey, int suspicion, long lastSeenAt) {
    }

    private record ReputationKey(String territoryKey, UUID intruderUuid) {
    }

    private static final class ReputationEntry {
        private int suspicion;
        private long firstCreatedAt;
        private long lastUpdatedAt;
        private long lastSeenAt;

        private long lastTrespassSuspicionAt = RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;
        private long lastVisibleWarningSuspicionAt = RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;
        private long lastTooCloseSuspicionAt = RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;

        private static ReputationEntry from(SerializedEntry serialized) {
            ReputationEntry entry = new ReputationEntry();
            entry.suspicion = serialized.suspicion();
            entry.firstCreatedAt = serialized.firstCreatedAt();
            entry.lastUpdatedAt = serialized.lastUpdatedAt();
            entry.lastSeenAt = serialized.lastSeenAt();
            entry.lastTrespassSuspicionAt = serialized.lastTrespassSuspicionAt();
            entry.lastVisibleWarningSuspicionAt = serialized.lastVisibleWarningSuspicionAt();
            entry.lastTooCloseSuspicionAt = serialized.lastTooCloseSuspicionAt();
            return entry;
        }

        private SerializedEntry serialize(String territoryKey, UUID intruderUuid) {
            return new SerializedEntry(
                    territoryKey,
                    intruderUuid,
                    suspicion,
                    firstCreatedAt,
                    lastUpdatedAt,
                    lastSeenAt,
                    lastTrespassSuspicionAt,
                    lastVisibleWarningSuspicionAt,
                    lastTooCloseSuspicionAt
            );
        }
    }
}
