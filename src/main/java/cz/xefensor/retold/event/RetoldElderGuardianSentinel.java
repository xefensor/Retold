package cz.xefensor.retold.event;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RetoldElderGuardianSentinel {
    private static final int SENTINEL_THINK_INTERVAL_TICKS = 20;
    private static final int SENTINEL_ALERT_COOLDOWN_TICKS = 20 * 4;
    private static final int DAMAGE_ALERT_COOLDOWN_TICKS = 20;

    private static final double SENTINEL_SCAN_RADIUS_BLOCKS = 64.0D;
    private static final double SENTINEL_SCAN_RADIUS_SQUARED =
            SENTINEL_SCAN_RADIUS_BLOCKS * SENTINEL_SCAN_RADIUS_BLOCKS;

    private static final Map<UUID, Long> LAST_SENTINEL_ALERT_AT = new HashMap<>();
    private static final Map<UUID, Long> LAST_DAMAGE_ALERT_AT = new HashMap<>();

    private RetoldElderGuardianSentinel() {
    }

    static void onElderGuardianTick(ElderGuardian elderGuardian) {
        if (!(elderGuardian.level() instanceof ServerLevel level)) {
            return;
        }

        if (!elderGuardian.isAlive()) {
            cleanup(elderGuardian);
            return;
        }

        long gameTime = level.getGameTime();

        if ((gameTime + elderGuardian.getId()) % SENTINEL_THINK_INTERVAL_TICKS != 0L) {
            return;
        }

        Player target = currentPlayerTarget(elderGuardian);
        int pressureLevel = 3;

        if (target == null) {
            target = findBestMonumentIntruder(level, elderGuardian);
            pressureLevel = 2;
        }

        if (target == null) {
            return;
        }

        alertFromSentinel(
                level,
                elderGuardian,
                target,
                pressureLevel,
                gameTime,
                SENTINEL_ALERT_COOLDOWN_TICKS,
                LAST_SENTINEL_ALERT_AT
        );
    }

    static void onIncomingDamage(
            LivingIncomingDamageEvent event,
            ElderGuardian elderGuardian
    ) {
        if (!(elderGuardian.level() instanceof ServerLevel level)) {
            return;
        }

        Player attacker = livingPlayer(event.getSource().getEntity());

        if (!isValidThreat(elderGuardian, attacker)) {
            return;
        }

        alertFromSentinel(
                level,
                elderGuardian,
                attacker,
                3,
                level.getGameTime(),
                DAMAGE_ALERT_COOLDOWN_TICKS,
                LAST_DAMAGE_ALERT_AT
        );
    }

    private static void alertFromSentinel(
            ServerLevel level,
            ElderGuardian elderGuardian,
            Player target,
            int pressureLevel,
            long gameTime,
            int cooldownTicks,
            Map<UUID, Long> cooldowns
    ) {
        Long lastAlertAt = cooldowns.get(elderGuardian.getUUID());

        if (lastAlertAt != null && gameTime - lastAlertAt < cooldownTicks) {
            return;
        }

        cooldowns.put(elderGuardian.getUUID(), gameTime);

        if (elderGuardian.hasLineOfSight(target)) {
            if (!RetoldCombatTargets.applyAttackTarget(
                    elderGuardian,
                    target,
                    RetoldTargetSource.FACTION_ASSIST
            )) {
                return;
            }
        }

        RetoldGuardianAlertController.alertNearby(
                level,
                elderGuardian.blockPosition(),
                target,
                pressureLevel
        );
    }

    private static Player currentPlayerTarget(ElderGuardian elderGuardian) {
        LivingEntity target = elderGuardian.getTarget();

        if (!(target instanceof Player player)) {
            return null;
        }

        if (!isValidThreat(elderGuardian, player)) {
            return null;
        }

        return player;
    }

    private static Player findBestMonumentIntruder(
            ServerLevel level,
            ElderGuardian elderGuardian
    ) {
        StructureStart monumentStart = RetoldOceanMonumentSupport.findAt(
                level,
                elderGuardian
        );

        if (monumentStart == null || !monumentStart.isValid()) {
            return null;
        }

        AABB monumentBounds = AABB.of(monumentStart.getBoundingBox()).inflate(3.0D);
        AABB scanBounds = elderGuardian.getBoundingBox().inflate(SENTINEL_SCAN_RADIUS_BLOCKS);
        List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class,
                scanBounds,
                player -> isValidThreat(elderGuardian, player)
                        && monumentBounds.contains(player.position())
        );

        Player bestPlayer = null;
        double bestScore = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            double distanceSquared = elderGuardian.distanceToSqr(player);

            if (distanceSquared > SENTINEL_SCAN_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (elderGuardian.hasLineOfSight(player)) {
                score -= 256.0D;
            }

            if (player.isInWater()) {
                score -= 64.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPlayer = player;
            }
        }

        return bestPlayer;
    }

    private static boolean isValidThreat(
            ElderGuardian elderGuardian,
            Player player
    ) {
        if (elderGuardian == null || player == null) {
            return false;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return false;
        }

        if (player.level() != elderGuardian.level()) {
            return false;
        }

        return elderGuardian.distanceToSqr(player) <= SENTINEL_SCAN_RADIUS_SQUARED;
    }

    private static Player livingPlayer(Object entity) {
        if (entity instanceof Player player) {
            return player;
        }

        return null;
    }

    private static void cleanup(ElderGuardian elderGuardian) {
        LAST_SENTINEL_ALERT_AT.remove(elderGuardian.getUUID());
        LAST_DAMAGE_ALERT_AT.remove(elderGuardian.getUUID());
    }
}
