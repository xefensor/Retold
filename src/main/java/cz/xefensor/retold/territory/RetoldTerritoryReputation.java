package cz.xefensor.retold.territory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RetoldTerritoryReputation {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Map<ReputationKey, ReputationEntry> REPUTATION = new HashMap<>();

    private static long lastDecayAt = -RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS;
    private static long lastSaveAt = -RetoldTerritoryConstants.REPUTATION_SAVE_INTERVAL_TICKS;

    private static boolean dirty;
    private static boolean loaded;
    private static Path loadedSavePath;

    private RetoldTerritoryReputation() {
    }

    public static void loadFromServer(MinecraftServer server) {
        if (server == null) {
            return;
        }

        Path savePath = getSavePath(server);

        if (loaded && savePath.equals(loadedSavePath)) {
            return;
        }

        REPUTATION.clear();
        loaded = true;
        dirty = false;
        loadedSavePath = savePath;

        if (!Files.exists(savePath)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                return;
            }

            JsonObject root = rootElement.getAsJsonObject();
            JsonArray entries = getArray(root, "entries");

            if (entries == null) {
                return;
            }

            for (JsonElement entryElement : entries) {
                if (!entryElement.isJsonObject()) {
                    continue;
                }

                readEntry(entryElement.getAsJsonObject());
            }
        } catch (IOException | RuntimeException exception) {
            System.err.println("[Retold] Failed to load territory reputation: " + exception.getMessage());
        }
    }

    public static void saveIfDirty(MinecraftServer server, long gameTime) {
        if (!dirty) {
            return;
        }

        if (gameTime - lastSaveAt < RetoldTerritoryConstants.REPUTATION_SAVE_INTERVAL_TICKS) {
            return;
        }

        saveToServer(server, gameTime);
    }

    public static void saveToServer(MinecraftServer server, long gameTime) {
        if (server == null) {
            return;
        }

        loadFromServer(server);

        Path savePath = getSavePath(server);

        try {
            Files.createDirectories(savePath.getParent());

            JsonObject root = new JsonObject();
            root.addProperty("version", RetoldTerritoryConstants.REPUTATION_SAVE_VERSION);

            JsonArray entries = new JsonArray();

            for (Map.Entry<ReputationKey, ReputationEntry> mapEntry : REPUTATION.entrySet()) {
                ReputationEntry entry = mapEntry.getValue();

                if (entry.suspicion <= 0) {
                    continue;
                }

                entries.add(writeEntry(mapEntry.getKey(), entry));
            }

            root.add("entries", entries);

            try (BufferedWriter writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            dirty = false;
            loaded = true;
            loadedSavePath = savePath;
            lastSaveAt = gameTime;
        } catch (IOException | RuntimeException exception) {
            System.err.println("[Retold] Failed to save territory reputation: " + exception.getMessage());
        }
    }

    public static void tickDecay(long gameTime) {
        if (
                gameTime - lastDecayAt
                        < RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS
        ) {
            return;
        }

        lastDecayAt = gameTime;

        boolean changed = false;
        Iterator<Map.Entry<ReputationKey, ReputationEntry>> iterator = REPUTATION.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ReputationKey, ReputationEntry> mapEntry = iterator.next();
            ReputationEntry entry = mapEntry.getValue();

            if (entry.suspicion <= 0) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (
                    gameTime - entry.lastSeenAt
                            <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS
            ) {
                continue;
            }

            entry.suspicion -= RetoldTerritoryConstants.REPUTATION_DECAY_AMOUNT;
            entry.lastUpdatedAt = gameTime;
            changed = true;

            if (entry.suspicion <= 0) {
                iterator.remove();
            }
        }

        if (changed) {
            dirty = true;
        }
    }

    public static int getSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        if (territory == null || intruder == null) {
            return 0;
        }

        ReputationEntry entry = REPUTATION.get(key(territory, intruder));

        if (entry == null) {
            return 0;
        }

        return entry.suspicion;
    }

    public static RetoldWarningLevel getWarningLevel(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        return getWarningLevel(getSuspicion(territory, intruder));
    }

    public static boolean shouldAttack(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        return getSuspicion(territory, intruder)
                >= RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD;
    }

    public static void markSeen(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        ReputationEntry entry = getOrCreateEntry(territory, intruder, gameTime);

        if (entry == null) {
            return;
        }

        entry.lastSeenAt = gameTime;

        if (entry.suspicion > 0) {
            dirty = true;
        }
    }

    public static void addTrespassSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        ReputationEntry entry = getOrCreateEntry(territory, intruder, gameTime);

        if (entry == null) {
            return;
        }

        if (
                gameTime - entry.lastTrespassSuspicionAt
                        < RetoldTerritoryConstants.REPUTATION_TRESPASS_SUSPICION_COOLDOWN_TICKS
        ) {
            return;
        }

        entry.lastTrespassSuspicionAt = gameTime;

        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_TRESPASS_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addVisibleWarningSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        addVisibleWarningSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_VISIBLE_WARNING_DEFAULT_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addVisibleWarningSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            int amount,
            long gameTime
    ) {
        ReputationEntry entry = getOrCreateEntry(territory, intruder, gameTime);

        if (entry == null) {
            return;
        }

        if (
                gameTime - entry.lastVisibleWarningSuspicionAt
                        < RetoldTerritoryConstants.REPUTATION_VISIBLE_WARNING_SUSPICION_COOLDOWN_TICKS
        ) {
            return;
        }

        entry.lastVisibleWarningSuspicionAt = gameTime;
        addSuspicion(territory, intruder, amount, gameTime);
    }

    public static void addTooCloseSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        ReputationEntry entry = getOrCreateEntry(territory, intruder, gameTime);

        if (entry == null) {
            return;
        }

        if (
                gameTime - entry.lastTooCloseSuspicionAt
                        < RetoldTerritoryConstants.REPUTATION_TOO_CLOSE_SUSPICION_COOLDOWN_TICKS
        ) {
            return;
        }

        entry.lastTooCloseSuspicionAt = gameTime;

        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_TOO_CLOSE_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addStealingSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_STEALING_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addBlockBreakSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_BLOCK_BREAK_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addAttackSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_ATTACK_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addKillSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        addSuspicion(
                territory,
                intruder,
                RetoldTerritoryConstants.REPUTATION_KILL_SUSPICION_GAIN,
                gameTime
        );
    }

    public static void addSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            int amount,
            long gameTime
    ) {
        if (amount <= 0) {
            return;
        }

        ReputationEntry entry = getOrCreateEntry(territory, intruder, gameTime);

        if (entry == null) {
            return;
        }

        int oldSuspicion = entry.suspicion;

        entry.suspicion = Math.min(
                RetoldTerritoryConstants.REPUTATION_MAX_SUSPICION,
                entry.suspicion + amount
        );

        entry.lastUpdatedAt = gameTime;

        if (entry.suspicion != oldSuspicion) {
            dirty = true;
        }
    }

    public static String getDebugText(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        if (territory == null || intruder == null) {
            return "No territory reputation";
        }

        ReputationEntry entry = REPUTATION.get(key(territory, intruder));

        int suspicion = entry == null ? 0 : entry.suspicion;
        long ticksSinceSeen = entry == null ? -1L : gameTime - entry.lastSeenAt;

        RetoldWarningLevel warningLevel = getWarningLevel(suspicion);

        String decayState;

        if (entry == null || suspicion <= 0) {
            decayState = "empty";
        } else if (
                ticksSinceSeen
                        <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS
        ) {
            decayState = "blocked, seen " + ticksSinceSeen + "t ago";
        } else {
            decayState = "decaying";
        }

        return territory.debugName()
                + " | Suspicion: "
                + suspicion
                + "/"
                + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + " | Level: "
                + warningLevel.name()
                + " | Decay: "
                + decayState;
    }

    public static String getMostSuspiciousDebugText(
            LivingEntity intruder,
            long gameTime
    ) {
        if (intruder == null) {
            return "No reputation";
        }

        ReputationKey bestKey = null;
        ReputationEntry bestEntry = null;

        for (Map.Entry<ReputationKey, ReputationEntry> mapEntry : REPUTATION.entrySet()) {
            ReputationKey key = mapEntry.getKey();
            ReputationEntry entry = mapEntry.getValue();

            if (!key.intruderUuid.equals(intruder.getUUID())) {
                continue;
            }

            if (entry.suspicion <= 0) {
                continue;
            }

            if (bestEntry == null || entry.suspicion > bestEntry.suspicion) {
                bestKey = key;
                bestEntry = entry;
            }
        }

        if (bestKey == null || bestEntry == null) {
            return "No reputation";
        }

        long ticksSinceSeen = gameTime - bestEntry.lastSeenAt;
        RetoldWarningLevel warningLevel = getWarningLevel(bestEntry.suspicion);

        String decayState = ticksSinceSeen
                <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS
                ? "blocked, seen " + ticksSinceSeen + "t ago"
                : "decaying";

        return bestKey.territoryKey
                + " | Suspicion: "
                + bestEntry.suspicion
                + "/"
                + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + " | Level: "
                + warningLevel.name()
                + " | Decay: "
                + decayState;
    }

    private static ReputationEntry getOrCreateEntry(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        if (territory == null || intruder == null) {
            return null;
        }

        ReputationKey key = key(territory, intruder);
        ReputationEntry entry = REPUTATION.computeIfAbsent(
                key,
                ignored -> new ReputationEntry()
        );

        if (entry.firstCreatedAt == 0L) {
            entry.firstCreatedAt = gameTime;
        }

        return entry;
    }

    private static ReputationKey key(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        return new ReputationKey(
                territory.reputationKey(),
                intruder.getUUID()
        );
    }

    private static RetoldWarningLevel getWarningLevel(int suspicion) {
        if (suspicion >= RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD) {
            return RetoldWarningLevel.ATTACK;
        }

        if (suspicion >= RetoldTerritoryConstants.REPUTATION_FINAL_WARNING_THRESHOLD) {
            return RetoldWarningLevel.FINAL_WARNING;
        }

        if (suspicion >= RetoldTerritoryConstants.REPUTATION_WARNING_THRESHOLD) {
            return RetoldWarningLevel.WARNING;
        }

        if (suspicion >= RetoldTerritoryConstants.REPUTATION_NOTICED_THRESHOLD) {
            return RetoldWarningLevel.NOTICED;
        }

        return RetoldWarningLevel.NONE;
    }

    private static Path getSavePath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve(RetoldTerritoryConstants.REPUTATION_SAVE_DIRECTORY)
                .resolve(RetoldTerritoryConstants.REPUTATION_SAVE_FILE_NAME);
    }

    private static void readEntry(JsonObject json) {
        String territoryKey = getString(json, "territory", "");
        String intruderUuidText = getString(json, "intruder", "");

        if (territoryKey.isBlank() || intruderUuidText.isBlank()) {
            return;
        }

        UUID intruderUuid;

        try {
            intruderUuid = UUID.fromString(intruderUuidText);
        } catch (IllegalArgumentException exception) {
            return;
        }

        ReputationEntry entry = new ReputationEntry();
        entry.suspicion = getInt(json, "suspicion", 0);

        if (entry.suspicion <= 0) {
            return;
        }

        entry.firstCreatedAt = getLong(json, "firstCreatedAt", 0L);
        entry.lastUpdatedAt = getLong(json, "lastUpdatedAt", 0L);
        entry.lastSeenAt = getLong(json, "lastSeenAt", 0L);

        entry.lastTrespassSuspicionAt = getLong(
                json,
                "lastTrespassSuspicionAt",
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
        );

        entry.lastVisibleWarningSuspicionAt = getLong(
                json,
                "lastVisibleWarningSuspicionAt",
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
        );

        entry.lastTooCloseSuspicionAt = getLong(
                json,
                "lastTooCloseSuspicionAt",
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME
        );

        REPUTATION.put(
                new ReputationKey(territoryKey, intruderUuid),
                entry
        );
    }

    private static JsonObject writeEntry(
            ReputationKey key,
            ReputationEntry entry
    ) {
        JsonObject json = new JsonObject();

        json.addProperty("territory", key.territoryKey);
        json.addProperty("intruder", key.intruderUuid.toString());
        json.addProperty("suspicion", entry.suspicion);
        json.addProperty("firstCreatedAt", entry.firstCreatedAt);
        json.addProperty("lastUpdatedAt", entry.lastUpdatedAt);
        json.addProperty("lastSeenAt", entry.lastSeenAt);
        json.addProperty("lastTrespassSuspicionAt", entry.lastTrespassSuspicionAt);
        json.addProperty("lastVisibleWarningSuspicionAt", entry.lastVisibleWarningSuspicionAt);
        json.addProperty("lastTooCloseSuspicionAt", entry.lastTooCloseSuspicionAt);

        return json;
    }

    private static JsonArray getArray(JsonObject json, String key) {
        JsonElement element = json.get(key);

        if (element == null || !element.isJsonArray()) {
            return null;
        }

        return element.getAsJsonArray();
    }

    private static String getString(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);

        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);

        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }

        try {
            return element.getAsInt();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static long getLong(JsonObject json, String key, long fallback) {
        JsonElement element = json.get(key);

        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }

        try {
            return element.getAsLong();
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private record ReputationKey(
            String territoryKey,
            UUID intruderUuid
    ) {
    }

    private static final class ReputationEntry {
        private int suspicion;
        private long firstCreatedAt;
        private long lastUpdatedAt;
        private long lastSeenAt;

        private long lastTrespassSuspicionAt =
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;

        private long lastVisibleWarningSuspicionAt =
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;

        private long lastTooCloseSuspicionAt =
                RetoldTerritoryConstants.REPUTATION_INITIAL_COOLDOWN_TIME;
    }
}