package cz.xefensor.retold.territory;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class RetoldTerritoryReputation {
    private static final Map<ReputationKey, ReputationEntry> REPUTATION = new HashMap<>();

    private static long lastDecayAt = -RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS;

    private RetoldTerritoryReputation() {
    }

    public static void tickDecay(long gameTime) {
        if (
                gameTime - lastDecayAt
                        < RetoldTerritoryConstants.REPUTATION_DECAY_INTERVAL_TICKS
        ) {
            return;
        }

        lastDecayAt = gameTime;

        Iterator<Map.Entry<ReputationKey, ReputationEntry>> iterator = REPUTATION.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ReputationKey, ReputationEntry> mapEntry = iterator.next();
            ReputationEntry entry = mapEntry.getValue();

            if (entry.suspicion <= 0) {
                iterator.remove();
                continue;
            }

            if (
                    gameTime - entry.lastSeenAt
                            <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS
            ) {
                continue;
            }

            entry.suspicion -= RetoldTerritoryConstants.REPUTATION_DECAY_AMOUNT;

            if (entry.suspicion <= 0) {
                iterator.remove();
            }
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

        entry.suspicion = Math.min(
                RetoldTerritoryConstants.REPUTATION_MAX_SUSPICION,
                entry.suspicion + amount
        );

        entry.lastUpdatedAt = gameTime;
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