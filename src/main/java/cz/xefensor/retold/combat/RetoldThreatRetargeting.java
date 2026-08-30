package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Lets ordinary combat AI reconsider a retained target when another attacker proves materially
 * more dangerous. Damage events supply the evidence, so this owner adds no scans or tick work.
 */
public final class RetoldThreatRetargeting {
    private static final int MAX_TRACKED_ATTACKERS = 8;
    private static final int MAX_HITS_PER_ATTACKER = 16;

    private static final Map<Mob, ThreatState> THREAT_STATES =
            new WeakHashMap<>();

    private RetoldThreatRetargeting() {
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || event.getHealthDamage() <= 0.0F
                || !canUseThreatRetargeting(mob)) {
            return;
        }

        Entity causingEntity = event.getSource().getEntity();

        if (!(causingEntity instanceof LivingEntity attacker)
                || attacker == mob
                || !isValidRetaliationTarget(mob, attacker)) {
            return;
        }

        long gameTime = level.getGameTime();
        ThreatState state = THREAT_STATES.computeIfAbsent(
                mob,
                ignored -> new ThreatState()
        );
        state.record(attacker, event.getHealthDamage(), gameTime);

        LivingEntity currentTarget = mob.getTarget();

        if (currentTarget == null) {
            return;
        }

        if (currentTarget == attacker) {
            if (RetoldFactionTargetMemory.isOwnedByAny(
                    mob,
                    attacker,
                    RetoldTargetSource.THREAT_RESPONSE
            )) {
                RetoldCombatTargets.applyAttackTarget(
                        mob,
                        attacker,
                        RetoldTargetSource.RETALIATION
                );
            }

            return;
        }

        RetoldAiControlMode controlMode = RetoldAiControl.getMode(mob);

        if (controlMode == RetoldAiControlMode.FLEE
                || controlMode == RetoldAiControlMode.SHELTER) {
            return;
        }

        RetoldTargetSource currentSource = RetoldFactionTargetMemory.getSource(
                mob,
                currentTarget
        );

        if (RetoldThreatRetargetPolicy.preservesExistingDuty(currentSource)) {
            return;
        }

        long ticksSinceLastSwitch = state.ticksSinceLastSwitch(gameTime);
        double currentScore = state.score(currentTarget, gameTime);
        double challengerScore = state.score(attacker, gameTime);

        if (!RetoldThreatRetargetPolicy.shouldSwitch(
                currentScore,
                challengerScore,
                ticksSinceLastSwitch
        )) {
            return;
        }

        if (RetoldCombatTargets.applyAttackTarget(
                mob,
                attacker,
                RetoldTargetSource.RETALIATION
        )) {
            state.markSwitched(gameTime);
        }
    }

    private static boolean canUseThreatRetargeting(Mob mob) {
        RetoldMobProfileType type = RetoldMobProfiles.get(mob).type();

        return switch (type) {
            case PACK_PREDATOR,
                 SOLO_OPPORTUNIST,
                 AQUATIC_PREDATOR,
                 HUNGRY_SWARM_PREDATOR,
                 HIVE_COLONY,
                 NETHER_HUNGRY,
                 UNDEAD_HUNGRY,
                 UNDEAD_TOLERANT,
                 PHANTOM_STALKER,
                 GHAST_ARTILLERY,
                 ZOGLIN_RAMPAGER,
                 SLIME_HUNGRY,
                 SMALL_ARTHROPOD_SWARM,
                 PROTECTIVE_NEUTRAL,
                 AMPHIBIAN_FORAGER,
                 AQUATIC_HELPER_PREDATOR,
                 UNDEAD_MOUNT,
                 AQUATIC_TERRITORY_GUARD,
                 TERRITORY_GUARD,
                 COMMANDER_SUPPORT,
                 ILLAGER_RAIDER,
                 BAT_COLONY -> true;
            default -> false;
        };
    }

    private static boolean isValidRetaliationTarget(
            Mob mob,
            LivingEntity attacker
    ) {
        if (!RetoldAiTargets.isValidAssignmentTarget(mob, attacker)) {
            return false;
        }

        if (!mob.canAttack(attacker)
                && !RetoldMobTargetPolicy.shouldAllowFactionGhastTarget(mob, attacker)) {
            return false;
        }

        return !RetoldMobTargetPolicy.shouldBlockDeliberateHostility(
                mob,
                attacker,
                RetoldTargetSource.RETALIATION
        );
    }

    private static final class ThreatState {
        private final Map<LivingEntity, DamageHistory> histories =
                new IdentityHashMap<>();
        private long lastSwitchTick = Long.MIN_VALUE;

        private void record(
                LivingEntity attacker,
                float damage,
                long gameTime
        ) {
            prune(gameTime);

            DamageHistory history = histories.get(attacker);

            if (history == null) {
                if (histories.size() >= MAX_TRACKED_ATTACKERS) {
                    histories.entrySet().stream()
                            .min(Comparator.comparingLong(entry -> entry.getValue().lastHitTick()))
                            .map(Map.Entry::getKey)
                            .ifPresent(histories::remove);
                }

                history = new DamageHistory();
                histories.put(attacker, history);
            }

            history.record(damage, gameTime);
        }

        private double score(LivingEntity attacker, long gameTime) {
            DamageHistory history = histories.get(attacker);

            if (history == null) {
                return 0.0D;
            }

            history.prune(gameTime);

            if (history.isEmpty()) {
                histories.remove(attacker);
                return 0.0D;
            }

            return RetoldThreatRetargetPolicy.score(
                    history.totalDamage(),
                    history.largestHit()
            );
        }

        private long ticksSinceLastSwitch(long gameTime) {
            if (lastSwitchTick == Long.MIN_VALUE) {
                return RetoldThreatRetargetPolicy.RETARGET_LOCK_TICKS;
            }

            return gameTime - lastSwitchTick;
        }

        private void markSwitched(long gameTime) {
            lastSwitchTick = gameTime;
        }

        private void prune(long gameTime) {
            Iterator<Map.Entry<LivingEntity, DamageHistory>> iterator =
                    histories.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<LivingEntity, DamageHistory> entry = iterator.next();
                LivingEntity attacker = entry.getKey();
                DamageHistory history = entry.getValue();

                history.prune(gameTime);

                if (!attacker.isAlive()
                        || attacker.isRemoved()
                        || history.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private static final class DamageHistory {
        private final ArrayDeque<DamageHit> hits = new ArrayDeque<>();
        private float totalDamage;

        private void record(float damage, long gameTime) {
            while (hits.size() >= MAX_HITS_PER_ATTACKER) {
                totalDamage -= hits.removeFirst().damage();
            }

            hits.addLast(new DamageHit(gameTime, damage));
            totalDamage += damage;
        }

        private void prune(long gameTime) {
            long oldestAllowedTick = gameTime
                    - RetoldThreatRetargetPolicy.RECENT_DAMAGE_WINDOW_TICKS;

            while (!hits.isEmpty() && hits.getFirst().gameTime() < oldestAllowedTick) {
                totalDamage -= hits.removeFirst().damage();
            }

            if (hits.isEmpty()) {
                totalDamage = 0.0F;
            }
        }

        private boolean isEmpty() {
            return hits.isEmpty();
        }

        private float totalDamage() {
            return Math.max(0.0F, totalDamage);
        }

        private float largestHit() {
            float largestHit = 0.0F;

            for (DamageHit hit : hits) {
                largestHit = Math.max(largestHit, hit.damage());
            }

            return largestHit;
        }

        private long lastHitTick() {
            return hits.isEmpty() ? Long.MIN_VALUE : hits.getLast().gameTime();
        }
    }

    private record DamageHit(long gameTime, float damage) {
    }
}
