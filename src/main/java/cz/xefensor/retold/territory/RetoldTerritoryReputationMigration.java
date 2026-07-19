package cz.xefensor.retold.territory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

final class RetoldTerritoryReputationMigration {
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

    private static final Codec<LegacyEntry> LEGACY_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("territory").forGetter(LegacyEntry::territoryKey),
            UUID_CODEC.fieldOf("intruder").forGetter(LegacyEntry::intruderUuid),
            Codec.INT.fieldOf("suspicion").forGetter(LegacyEntry::suspicion),
            Codec.LONG.fieldOf("firstCreatedAt").forGetter(LegacyEntry::firstCreatedAt),
            Codec.LONG.fieldOf("lastUpdatedAt").forGetter(LegacyEntry::lastUpdatedAt),
            Codec.LONG.fieldOf("lastSeenAt").forGetter(LegacyEntry::lastSeenAt),
            Codec.LONG.optionalFieldOf(
                    "lastTrespassSuspicionAt",
                    RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
            ).forGetter(LegacyEntry::lastTrespassSuspicionAt),
            Codec.LONG.optionalFieldOf(
                    "lastVisibleWarningSuspicionAt",
                    RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
            ).forGetter(LegacyEntry::lastVisibleWarningSuspicionAt),
            Codec.LONG.optionalFieldOf(
                    "lastTooCloseSuspicionAt",
                    RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
            ).forGetter(LegacyEntry::lastTooCloseSuspicionAt)
    ).apply(instance, LegacyEntry::new));

    private static final Codec<LegacyFile> LEGACY_FILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("version").forGetter(LegacyFile::version),
            LEGACY_ENTRY_CODEC.listOf().fieldOf("entries").forGetter(LegacyFile::entries)
    ).apply(instance, LegacyFile::new));

    private RetoldTerritoryReputationMigration() {
    }

    static void migrateIfNeeded(MinecraftServer server, RetoldTerritoryReputationData data) {
        Path legacyPath = server.getWorldPath(LevelResource.ROOT)
                .resolve(RetoldTerritoryConstants.REPUTATION_SAVE_DIRECTORY)
                .resolve(RetoldTerritoryConstants.REPUTATION_SAVE_FILE_NAME);

        migrateIfNeeded(legacyPath, data);
    }

    static void migrateIfNeeded(Path legacyPath, RetoldTerritoryReputationData data) {
        if (!data.beginLegacyMigrationCheck()) {
            return;
        }

        if (data.isLegacyMigrationConfirmedOnLoad()) {
            archiveLegacyFile(legacyPath);
            return;
        }

        if (data.isLegacyJsonMigrated() || !Files.exists(legacyPath)) {
            return;
        }

        try {
            LegacyFile legacyFile = readLegacyFile(legacyPath);
            List<RetoldTerritoryReputationStore.SerializedEntry> entries = legacyFile.entries()
                    .stream()
                    .map(LegacyEntry::toSerializedEntry)
                    .toList();

            data.importLegacyEntries(entries);
            Retold.LOGGER.info(
                    "Migrated {} territory reputation entries from {} into SavedData; retaining the JSON until the migration is confirmed on a later load",
                    entries.size(),
                    legacyPath
            );
        } catch (IOException | RuntimeException exception) {
            Retold.LOGGER.error(
                    "Failed to migrate legacy territory reputation from {}; leaving the source file untouched",
                    legacyPath,
                    exception
            );
        }
    }

    private static LegacyFile readLegacyFile(Path legacyPath) throws IOException {
        JsonElement root;

        try (BufferedReader reader = Files.newBufferedReader(legacyPath, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader);
        }

        LegacyFile legacyFile = LEGACY_FILE_CODEC.parse(JsonOps.INSTANCE, root)
                .getOrThrow(IllegalArgumentException::new);

        if (legacyFile.version() != RetoldTerritoryConstants.REPUTATION_SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported legacy territory reputation version " + legacyFile.version()
            );
        }

        for (LegacyEntry entry : legacyFile.entries()) {
            if (entry.territoryKey().isBlank()) {
                throw new IllegalArgumentException("Legacy territory reputation entry has no territory key");
            }

            if (entry.suspicion() <= 0) {
                throw new IllegalArgumentException("Legacy territory reputation entry has non-positive suspicion");
            }
        }

        return legacyFile;
    }

    private static void archiveLegacyFile(Path legacyPath) {
        if (!Files.exists(legacyPath)) {
            return;
        }

        Path archivePath = RetoldTerritoryReputationLegacyFiles.archivePath(legacyPath);

        try {
            RetoldTerritoryReputationLegacyFiles.archive(legacyPath);
            Retold.LOGGER.info(
                    "Archived migrated territory reputation JSON from {} to {}",
                    legacyPath,
                    archivePath
            );
        } catch (IOException | RuntimeException exception) {
            Retold.LOGGER.warn(
                    "Could not archive migrated territory reputation JSON {}; leaving it untouched",
                    legacyPath,
                    exception
            );
        }
    }

    private record LegacyFile(int version, List<LegacyEntry> entries) {
    }

    private record LegacyEntry(
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
        private RetoldTerritoryReputationStore.SerializedEntry toSerializedEntry() {
            return new RetoldTerritoryReputationStore.SerializedEntry(
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
