package cz.xefensor.retold.territory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public final class RetoldTerritoryReputation {
    private RetoldTerritoryReputation() {
    }

    public static int getSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        RetoldTerritoryReputationData data = getData(intruder);

        if (territory == null || intruder == null || data == null) {
            return 0;
        }

        return data.getSuspicion(territory.reputationKey(), intruder.getUUID());
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
        RetoldTerritoryReputationData data = getData(territory, intruder);

        if (data != null) {
            data.markSeen(territory.reputationKey(), intruder.getUUID(), gameTime);
        }
    }

    public static void addTrespassSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        RetoldTerritoryReputationData data = getData(territory, intruder);

        if (data == null || !data.markTrespassSuspicionCooldown(
                territory.reputationKey(),
                intruder.getUUID(),
                gameTime
        )) {
            return;
        }

        data.addSuspicion(
                territory.reputationKey(),
                intruder.getUUID(),
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
        RetoldTerritoryReputationData data = getData(territory, intruder);

        if (data == null || !data.markVisibleWarningSuspicionCooldown(
                territory.reputationKey(),
                intruder.getUUID(),
                gameTime
        )) {
            return;
        }

        data.addSuspicion(territory.reputationKey(), intruder.getUUID(), amount, gameTime);
    }

    public static void addTooCloseSuspicion(
            RetoldTerritoryContext territory,
            LivingEntity intruder,
            long gameTime
    ) {
        RetoldTerritoryReputationData data = getData(territory, intruder);

        if (data == null || !data.markTooCloseSuspicionCooldown(
                territory.reputationKey(),
                intruder.getUUID(),
                gameTime
        )) {
            return;
        }

        data.addSuspicion(
                territory.reputationKey(),
                intruder.getUUID(),
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

        RetoldTerritoryReputationData data = getData(territory, intruder);

        if (data != null) {
            data.addSuspicion(territory.reputationKey(), intruder.getUUID(), amount, gameTime);
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

        int suspicion = getSuspicion(territory, intruder);
        RetoldTerritoryReputationData data = getData(intruder);
        RetoldTerritoryReputationStore.SerializedEntry entry = data == null
                ? null
                : data.snapshot(territory.reputationKey(), intruder.getUUID());
        long ticksSinceSeen = entry == null ? -1L : gameTime - entry.lastSeenAt();

        RetoldWarningLevel warningLevel = getWarningLevel(suspicion);
        String decayState;

        if (entry == null || suspicion <= 0) {
            decayState = "empty";
        } else if (ticksSinceSeen <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS) {
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
        RetoldTerritoryReputationData data = getData(intruder);

        if (intruder == null || data == null) {
            return "No reputation";
        }

        RetoldTerritoryReputationStore.MostSuspiciousEntry entry = data.findMostSuspicious(intruder.getUUID());

        if (entry == null) {
            return "No reputation";
        }

        long ticksSinceSeen = gameTime - entry.lastSeenAt();
        RetoldWarningLevel warningLevel = getWarningLevel(entry.suspicion());
        String decayState = ticksSinceSeen <= RetoldTerritoryConstants.REPUTATION_SEEN_DECAY_BLOCK_TICKS
                ? "blocked, seen " + ticksSinceSeen + "t ago"
                : "decaying";

        return entry.territoryKey()
                + " | Suspicion: "
                + entry.suspicion()
                + "/"
                + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + " | Level: "
                + warningLevel.name()
                + " | Decay: "
                + decayState;
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

    private static RetoldTerritoryReputationData getData(
            RetoldTerritoryContext territory,
            LivingEntity intruder
    ) {
        if (territory == null) {
            return null;
        }

        return getData(intruder);
    }

    private static RetoldTerritoryReputationData getData(LivingEntity intruder) {
        if (intruder == null || !(intruder.level() instanceof ServerLevel level)) {
            return null;
        }

        return RetoldTerritoryReputationData.get(level);
    }
}
