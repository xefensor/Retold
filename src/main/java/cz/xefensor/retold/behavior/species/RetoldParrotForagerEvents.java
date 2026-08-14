package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Parrot-specific behavior that does not belong in the shared food pipeline.
 *
 * <p>Seed and crop meals continue through the ordinary bounded forage owner so
 * they inherit hunger persistence, flight navigation, griefing policy, and
 * priority checks. This adapter only supplies the owner's readable danger
 * warning while leaving vanilla taming, following, shoulder riding, and
 * movement goals intact.</p>
 */
public final class RetoldParrotForagerEvents {
    private static final Map<Parrot, OwnerWarning> OWNER_WARNINGS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Long> SHOULDER_WARNING_PULSES = new WeakHashMap<>();

    private static final int THREAT_SCAN_CACHE_TICKS = 10;
    private static final int RECENT_ATTACK_TICKS = 20 * 5;
    private static final int WARNING_MEMORY_TICKS = 20 * 3;
    private static final int WARNING_PULSE_TICKS = 20;

    private static final double THREAT_SCAN_RADIUS = 18.0D;
    private static final double THREAT_SCAN_RADIUS_SQUARED =
            THREAT_SCAN_RADIUS * THREAT_SCAN_RADIUS;
    private static final double HEARING_RADIUS_SQUARED = 6.0D * 6.0D;

    private RetoldParrotForagerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (Math.floorMod(gameTime + player.getId(), 10L) != 0L) {
            return;
        }

        tickShoulderParrotOwner(level, player, gameTime);
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob instanceof Parrot parrot)
                || parrot.level() != level
                || !RetoldMobRules.isParrotForager(parrot)
                || !parrot.isTame()) {
            clearWarning(mob);
            return;
        }

        LivingEntity owner = parrot.getOwner();

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(parrot, owner)
                || RetoldBehaviorCoordinator.isInvalidPlayerTarget(owner)) {
            OWNER_WARNINGS.remove(parrot);
            return;
        }

        LivingEntity threat = recentOwnerAttacker(parrot, owner);

        if (threat == null) {
            threat = findActiveOwnerThreat(level, parrot, owner, gameTime);
        }

        updateWarning(parrot, threat, gameTime);
    }

    public static void tickShoulderParrotOwner(
            ServerLevel level,
            ServerPlayer owner,
            long gameTime
    ) {
        if (owner.level() != level
                || RetoldBehaviorCoordinator.isInvalidPlayerTarget(owner)
                || (owner.getShoulderParrotLeft().isEmpty()
                && owner.getShoulderParrotRight().isEmpty())) {
            SHOULDER_WARNING_PULSES.remove(owner);
            return;
        }

        LivingEntity threat = recentOwnerAttacker(owner, owner);

        if (threat == null) {
            threat = findActiveOwnerThreat(level, owner, owner, gameTime);
        }

        if (threat == null
                || gameTime < SHOULDER_WARNING_PULSES.getOrDefault(owner, 0L)) {
            return;
        }

        SHOULDER_WARNING_PULSES.put(owner, gameTime + WARNING_PULSE_TICKS);
        pulseShoulderWarning(level, owner);
    }

    public static boolean hasRecentShoulderWarning(
            ServerPlayer owner,
            long gameTime
    ) {
        return owner != null
                && gameTime < SHOULDER_WARNING_PULSES.getOrDefault(owner, 0L);
    }

    public static boolean isWarningOwner(
            Parrot parrot,
            long gameTime
    ) {
        OwnerWarning warning = OWNER_WARNINGS.get(parrot);

        return warning != null
                && gameTime < warning.expiresAt()
                && isValidThreat(parrot, parrot.getOwner(), warning.threat());
    }

    public static LivingEntity warningThreat(Parrot parrot) {
        OwnerWarning warning = OWNER_WARNINGS.get(parrot);
        return warning == null ? null : warning.threat();
    }

    private static LivingEntity recentOwnerAttacker(
            Entity observer,
            LivingEntity owner
    ) {
        LivingEntity attacker = owner.getLastHurtByMob();
        int elapsedTicks = owner.tickCount - owner.getLastHurtByMobTimestamp();

        if (elapsedTicks < 0 || elapsedTicks > RECENT_ATTACK_TICKS) {
            return null;
        }

        return isValidThreat(observer, owner, attacker) ? attacker : null;
    }

    private static LivingEntity findActiveOwnerThreat(
            ServerLevel level,
            Entity observer,
            LivingEntity owner,
            long gameTime
    ) {
        List<Mob> candidates = RetoldAiScanCache.nearby(
                level,
                observer,
                Mob.class,
                THREAT_SCAN_RADIUS,
                gameTime,
                THREAT_SCAN_CACHE_TICKS
        );

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (Mob candidate : candidates) {
            if (candidate.getTarget() != owner
                    || !isValidThreat(observer, owner, candidate)) {
                continue;
            }

            double distanceSquared = observer.distanceToSqr(candidate);

            if (distanceSquared > THREAT_SCAN_RADIUS_SQUARED) {
                continue;
            }

            boolean visible = observer instanceof Mob observerMob
                    ? RetoldAiSightCache.canSee(observerMob, candidate, gameTime)
                    : RetoldAiSightCache.canSee(candidate, owner, gameTime);

            if (!visible && distanceSquared > HEARING_RADIUS_SQUARED) {
                continue;
            }

            double score = visible ? distanceSquared - 12.0D : distanceSquared;

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isValidThreat(
            Entity observer,
            LivingEntity owner,
            LivingEntity threat
    ) {
        if (observer == null || owner == null || threat == null
                || threat == observer || threat == owner) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(observer, threat)
                || RetoldBehaviorCoordinator.isInvalidPlayerTarget(threat)) {
            return false;
        }

        return !(threat instanceof TamableAnimal tamableThreat)
                || !tamableThreat.isTame()
                || tamableThreat.getOwner() != owner;
    }

    private static void updateWarning(
            Parrot parrot,
            LivingEntity threat,
            long gameTime
    ) {
        OwnerWarning current = OWNER_WARNINGS.get(parrot);

        if (threat == null) {
            if (current == null
                    || gameTime >= current.expiresAt()
                    || !isValidThreat(parrot, parrot.getOwner(), current.threat())) {
                OWNER_WARNINGS.remove(parrot);
            }
            return;
        }

        boolean newThreat = current == null || current.threat() != threat;
        long nextPulseAt = newThreat ? gameTime : current.nextPulseAt();
        OwnerWarning updated = new OwnerWarning(
                threat,
                gameTime + WARNING_MEMORY_TICKS,
                nextPulseAt
        );

        if (gameTime >= nextPulseAt) {
            pulseWarning(parrot);
            updated = new OwnerWarning(
                    threat,
                    updated.expiresAt(),
                    gameTime + WARNING_PULSE_TICKS
            );
        }

        OWNER_WARNINGS.put(parrot, updated);

        if (newThreat) {
            RetoldMobState state = RetoldMobStates.getOrCreate(parrot, gameTime);
            state.markDanger(gameTime);
            state.addStress(2);
        }
    }

    private static void pulseWarning(Parrot parrot) {
        if (!(parrot.level() instanceof ServerLevel level)) {
            return;
        }

        if (!parrot.isSilent()) {
            parrot.playSound(
                    SoundEvents.PARROT_HURT,
                    1.0F,
                    0.65F + parrot.getRandom().nextFloat() * 0.15F
            );
        }

        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                parrot.getX(),
                parrot.getEyeY() + 0.15D,
                parrot.getZ(),
                2,
                0.15D,
                0.12D,
                0.15D,
                0.0D
        );
    }

    private static void pulseShoulderWarning(
            ServerLevel level,
            ServerPlayer owner
    ) {
        level.playSound(
                null,
                owner,
                SoundEvents.PARROT_HURT,
                SoundSource.PLAYERS,
                1.0F,
                0.72F + owner.getRandom().nextFloat() * 0.12F
        );
        level.sendParticles(
                ParticleTypes.ANGRY_VILLAGER,
                owner.getX(),
                owner.getEyeY() + 0.2D,
                owner.getZ(),
                2,
                0.3D,
                0.12D,
                0.3D,
                0.0D
        );
    }

    private static void clearWarning(PathfinderMob mob) {
        if (mob instanceof Parrot parrot) {
            OWNER_WARNINGS.remove(parrot);
        }
    }

    private record OwnerWarning(
            LivingEntity threat,
            long expiresAt,
            long nextPulseAt
    ) {
    }
}
