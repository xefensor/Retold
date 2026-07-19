package cz.xefensor.retold.territory;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.List;
import java.util.UUID;

final class RetoldTerritoryReputationData extends SavedData {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(UUID.fromString(value));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Invalid territory reputation UUID: " + value);
                }
            },
            UUID::toString
    );

    private static final Codec<RetoldTerritoryReputationStore.SerializedEntry> ENTRY_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("territory").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::territoryKey
                    ),
                    UUID_CODEC.fieldOf("intruder").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::intruderUuid
                    ),
                    Codec.INT.fieldOf("suspicion").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::suspicion
                    ),
                    Codec.LONG.fieldOf("first_created_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::firstCreatedAt
                    ),
                    Codec.LONG.fieldOf("last_updated_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::lastUpdatedAt
                    ),
                    Codec.LONG.fieldOf("last_seen_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::lastSeenAt
                    ),
                    Codec.LONG.fieldOf("last_trespass_suspicion_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::lastTrespassSuspicionAt
                    ),
                    Codec.LONG.fieldOf("last_visible_warning_suspicion_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::lastVisibleWarningSuspicionAt
                    ),
                    Codec.LONG.fieldOf("last_too_close_suspicion_at").forGetter(
                            RetoldTerritoryReputationStore.SerializedEntry::lastTooCloseSuspicionAt
                    )
            ).apply(instance, RetoldTerritoryReputationStore.SerializedEntry::new));

    private static final Codec<RetoldTerritoryReputationStore.SerializedState> RAW_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(
                            RetoldTerritoryReputationStore.SerializedState::version
                    ),
                    Codec.BOOL.optionalFieldOf("legacy_json_migrated", false).forGetter(
                            RetoldTerritoryReputationStore.SerializedState::legacyJsonMigrated
                    ),
                    ENTRY_CODEC.listOf().fieldOf("entries").forGetter(
                            RetoldTerritoryReputationStore.SerializedState::entries
                    )
            ).apply(instance, RetoldTerritoryReputationStore.SerializedState::new));

    private static final Codec<RetoldTerritoryReputationData> CODEC = withErrorLogging(
            RAW_CODEC
                    .flatXmap(
                            RetoldTerritoryReputationData::validateSerializedState,
                            RetoldTerritoryReputationData::validateSerializedState
                    )
                    .xmap(
                            RetoldTerritoryReputationData::new,
                            data -> data.store.serialize()
                    )
    );

    static final SavedDataType<RetoldTerritoryReputationData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "territory_reputation"),
            RetoldTerritoryReputationData::new,
            CODEC
    );

    private final RetoldTerritoryReputationStore store;

    RetoldTerritoryReputationData() {
        store = new RetoldTerritoryReputationStore();
    }

    private RetoldTerritoryReputationData(RetoldTerritoryReputationStore.SerializedState state) {
        store = RetoldTerritoryReputationStore.fromSerializedState(state);
    }

    static RetoldTerritoryReputationData get(ServerLevel level) {
        return get(level.getServer());
    }

    static RetoldTerritoryReputationData get(MinecraftServer server) {
        RetoldTerritoryReputationData data = server.getDataStorage().computeIfAbsent(TYPE);
        RetoldTerritoryReputationMigration.migrateIfNeeded(server, data);
        return data;
    }

    int getSuspicion(String territoryKey, UUID intruderUuid) {
        return store.getSuspicion(territoryKey, intruderUuid);
    }

    void markSeen(String territoryKey, UUID intruderUuid, long gameTime) {
        if (store.markSeen(territoryKey, intruderUuid, gameTime)) {
            setDirty();
        }
    }

    boolean markTrespassSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        boolean changed = store.markTrespassSuspicionCooldown(territoryKey, intruderUuid, gameTime);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    boolean markVisibleWarningSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        boolean changed = store.markVisibleWarningSuspicionCooldown(territoryKey, intruderUuid, gameTime);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    boolean markTooCloseSuspicionCooldown(String territoryKey, UUID intruderUuid, long gameTime) {
        boolean changed = store.markTooCloseSuspicionCooldown(territoryKey, intruderUuid, gameTime);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    void addSuspicion(String territoryKey, UUID intruderUuid, int amount, long gameTime) {
        if (store.addSuspicion(territoryKey, intruderUuid, amount, gameTime)) {
            setDirty();
        }
    }

    void tickDecay(long gameTime) {
        if (store.tickDecay(gameTime)) {
            setDirty();
        }
    }

    boolean shouldRequestSave(long gameTime) {
        return store.shouldRequestSave(gameTime, isDirty());
    }

    RetoldTerritoryReputationStore.MostSuspiciousEntry findMostSuspicious(UUID intruderUuid) {
        return store.findMostSuspicious(intruderUuid);
    }

    RetoldTerritoryReputationStore.SerializedEntry snapshot(String territoryKey, UUID intruderUuid) {
        return store.snapshot(territoryKey, intruderUuid);
    }

    boolean beginLegacyMigrationCheck() {
        return store.beginLegacyMigrationCheck();
    }

    boolean isLegacyMigrationConfirmedOnLoad() {
        return store.isLegacyMigrationConfirmedOnLoad();
    }

    boolean isLegacyJsonMigrated() {
        return store.isLegacyJsonMigrated();
    }

    void importLegacyEntries(List<RetoldTerritoryReputationStore.SerializedEntry> entries) {
        store.importLegacyEntries(entries);
        setDirty();
    }

    private static DataResult<RetoldTerritoryReputationStore.SerializedState> validateSerializedState(
            RetoldTerritoryReputationStore.SerializedState state
    ) {
        try {
            RetoldTerritoryReputationStore.validateSerializedState(state);
            return DataResult.success(state);
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static <T> Codec<T> withErrorLogging(Codec<T> codec) {
        return codec.mapResult(new Codec.ResultFunction<>() {
            @Override
            public <R> DataResult<Pair<T, R>> apply(
                    DynamicOps<R> ops,
                    R input,
                    DataResult<Pair<T, R>> result
            ) {
                result.error().ifPresent(error -> logCodecFailure("load", error.message()));
                return result;
            }

            @Override
            public <R> DataResult<R> coApply(DynamicOps<R> ops, T input, DataResult<R> result) {
                result.error().ifPresent(error -> logCodecFailure("save", error.message()));
                return result;
            }
        });
    }

    private static void logCodecFailure(String action, String message) {
        IllegalArgumentException exception = new IllegalArgumentException(message);
        Retold.LOGGER.error("Failed to {} territory reputation SavedData", action, exception);
    }
}
