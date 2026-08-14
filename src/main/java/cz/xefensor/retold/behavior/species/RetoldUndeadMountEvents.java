package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldUndeadMountEvents {
    private static final Map<AbstractHorse, Long> NEXT_STRIKE_AT = new WeakHashMap<>();

    private static final int TARGET_SCAN_CACHE_TICKS = 8;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final int ATTACK_CONTROL_TICKS = 20 * 4;
    private static final int OWNER_THREAT_MEMORY_TICKS = 20 * 5;
    private static final int STRIKE_COOLDOWN_TICKS = 20;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 24.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;
    private static final double TARGET_KEEP_RADIUS_BLOCKS = 36.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;
    private static final double CLOSE_AWARENESS_RADIUS_SQUARED = 6.0D * 6.0D;
    private static final double ATTACK_SPEED = 1.05D;

    private RetoldUndeadMountEvents() {
    }

    public static void onModifyAttributes(EntityAttributeModificationEvent event) {
        addAttackDamageIfMissing(event, EntityTypes.SKELETON_HORSE);
        addAttackDamageIfMissing(event, EntityTypes.ZOMBIE_HORSE);
        addAttackDamageIfMissing(event, EntityTypes.CAMEL_HUSK);
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !(event.getEntity() instanceof AbstractHorse mount)
                || !RetoldMobRules.isUndeadMount(mount)
                || !(mount.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        if (!isValidThreat(mount, attacker)) {
            return;
        }

        beginCombat(
                mount,
                attacker,
                RetoldTargetSource.RETALIATION,
                level.getGameTime()
        );
    }

    public static void onEntityMount(EntityMountEvent event) {
        if (event.isCanceled()
                || !event.isMounting()
                || !(event.getEntityMounting() instanceof Player player)
                || !(event.getEntityBeingMounted() instanceof AbstractHorse mount)
                || mount.level().isClientSide()
                || !RetoldMobRules.isUndeadMount(mount)
                || hasOwner(mount)
                || !mount.isTamed()
                || mount.isMobControlled()) {
            return;
        }

        mount.setOwner(player);
        mount.setTamed(true);
        mount.setPersistenceRequired();
        stopCombat(mount, mount.getTarget());
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob instanceof AbstractHorse mount)
                || level == null
                || mount.level() != level
                || !RetoldMobRules.isUndeadMount(mount)) {
            return;
        }

        LivingEntity target = mount.getTarget();

        if (RetoldAiControl.isControlledBy(
                mount,
                RetoldAiControlOwner.UNDEAD_MOUNT
        ) && !ownsCombat(mount, target)) {
            clearStaleCombatControl(mount);
        }

        if (ownsCombat(mount, target) && !isValidThreat(mount, target)) {
            stopCombat(mount, target);
            target = null;
        }

        LivingEntity retaliationThreat = recentThreat(
                mount,
                mount.getLastHurtByMob(),
                mount.getLastHurtByMobTimestamp()
        );

        if (retaliationThreat != null && target != retaliationThreat) {
            beginCombat(
                    mount,
                    retaliationThreat,
                    RetoldTargetSource.RETALIATION,
                    gameTime
            );
            return;
        }

        LivingEntity ownerThreat = findOwnerThreat(mount);

        if (ownerThreat != null && target != ownerThreat) {
            beginCombat(
                    mount,
                    ownerThreat,
                    RetoldTargetSource.OWNER_DEFENSE,
                    gameTime
            );
            return;
        }

        target = mount.getTarget();

        if (ownsCombat(mount, target)) {
            continueCombat(level, mount, target, gameTime);
            return;
        }

        if (hasOwner(mount) || mount.isMobControlled()) {
            return;
        }

        if (isValidWildThreat(mount, target, TARGET_KEEP_RADIUS_SQUARED)) {
            beginCombat(
                    mount,
                    target,
                    RetoldTargetSource.FACTION_COMBAT,
                    gameTime
            );
            return;
        }

        if (RetoldAiControl.isControlled(mount)) {
            return;
        }

        LivingEntity newTarget = findWildTarget(level, mount, gameTime);

        if (newTarget != null) {
            beginCombat(
                    mount,
                    newTarget,
                    RetoldTargetSource.FACTION_COMBAT,
                    gameTime
            );
        }
    }

    private static void addAttackDamageIfMissing(
            EntityAttributeModificationEvent event,
            EntityType<? extends LivingEntity> type
    ) {
        if (!event.has(type, Attributes.ATTACK_DAMAGE)) {
            event.add(type, Attributes.ATTACK_DAMAGE);
        }
    }

    private static LivingEntity findOwnerThreat(AbstractHorse mount) {
        LivingEntity owner = mount.getOwner();

        if (!RetoldBehaviorCoordinator.isAliveInSameLevel(mount, owner)) {
            return null;
        }

        LivingEntity attacker = recentThreat(
                owner,
                owner.getLastHurtByMob(),
                owner.getLastHurtByMobTimestamp()
        );
        LivingEntity attacked = recentThreat(
                owner,
                owner.getLastHurtMob(),
                owner.getLastHurtMobTimestamp()
        );
        boolean validAttacker = isValidClaimedThreat(mount, attacker);
        boolean validAttacked = isValidClaimedThreat(mount, attacked);

        if (validAttacker && validAttacked) {
            return owner.getLastHurtByMobTimestamp() >= owner.getLastHurtMobTimestamp()
                    ? attacker
                    : attacked;
        }

        if (validAttacker) {
            return attacker;
        }

        return validAttacked ? attacked : null;
    }

    private static LivingEntity recentThreat(
            LivingEntity subject,
            LivingEntity threat,
            int timestamp
    ) {
        if (subject == null || threat == null) {
            return null;
        }

        int age = subject.tickCount - timestamp;

        if (age < 0 || age > OWNER_THREAT_MEMORY_TICKS) {
            return null;
        }

        return threat;
    }

    private static LivingEntity findWildTarget(
            ServerLevel level,
            AbstractHorse mount,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                mount,
                LivingEntity.class,
                TARGET_SEARCH_RADIUS_BLOCKS,
                gameTime,
                TARGET_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidWildThreat(mount, candidate, TARGET_SEARCH_RADIUS_SQUARED)) {
                continue;
            }

            double distanceSquared = mount.distanceToSqr(candidate);

            if (distanceSquared > CLOSE_AWARENESS_RADIUS_SQUARED
                    && !RetoldAiSightCache.canSee(mount, candidate, gameTime)) {
                continue;
            }

            if (distanceSquared < bestScore) {
                bestScore = distanceSquared;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static boolean beginCombat(
            AbstractHorse mount,
            LivingEntity target,
            RetoldTargetSource source,
            long gameTime
    ) {
        if (!isSupportedSource(source) || !isValidThreat(mount, target)) {
            return false;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                mount,
                RetoldAiControlOwner.UNDEAD_MOUNT,
                RetoldAiPriorities.ATTACK,
                combatReason(source),
                gameTime,
                ATTACK_CONTROL_TICKS
        )) {
            return false;
        }

        return RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                mount,
                target,
                source,
                RetoldAiControlOwner.UNDEAD_MOUNT
        );
    }

    private static void continueCombat(
            ServerLevel level,
            AbstractHorse mount,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidThreat(mount, target)) {
            stopCombat(mount, target);
            return;
        }

        RetoldAiControl.refreshIfOwnedBy(
                mount,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.UNDEAD_MOUNT,
                gameTime,
                ATTACK_CONTROL_TICKS
        );
        mount.getLookControl().setLookAt(target, 30.0F, 30.0F);

        if (!mount.isVehicle()) {
            RetoldBehaviorMovement.throttledMoveTo(
                    mount,
                    target,
                    ATTACK_SPEED,
                    gameTime,
                    PATH_INTERVAL_TICKS,
                    2.0D * 2.0D
            );
        }

        tryStrike(level, mount, target, gameTime);
    }

    private static void tryStrike(
            ServerLevel level,
            AbstractHorse mount,
            LivingEntity target,
            long gameTime
    ) {
        if (!mount.getBoundingBox().inflate(0.75D).intersects(target.getBoundingBox())) {
            return;
        }

        long nextStrikeAt = NEXT_STRIKE_AT.getOrDefault(mount, 0L);

        if (gameTime < nextStrikeAt) {
            return;
        }

        mount.swing(InteractionHand.MAIN_HAND);

        if (mount.doHurtTarget(level, target)) {
            NEXT_STRIKE_AT.put(mount, gameTime + STRIKE_COOLDOWN_TICKS);
        }
    }

    private static boolean ownsCombat(
            AbstractHorse mount,
            LivingEntity target
    ) {
        return target != null
                && RetoldAiControl.isControlledAsBy(
                mount,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.UNDEAD_MOUNT
        )
                && RetoldFactionTargetMemory.isOwnedByAny(
                mount,
                target,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldTargetSource.OWNER_DEFENSE,
                RetoldTargetSource.RETALIATION
        );
    }

    private static boolean isValidThreat(
            AbstractHorse mount,
            LivingEntity target
    ) {
        if (hasOwner(mount)) {
            return isValidClaimedThreat(mount, target);
        }

        return isValidWildThreat(mount, target, TARGET_KEEP_RADIUS_SQUARED);
    }

    private static boolean isValidWildThreat(
            AbstractHorse mount,
            LivingEntity target,
            double maxDistanceSquared
    ) {
        return RetoldBehaviorCombat.isValidEnemyTarget(
                mount,
                target,
                maxDistanceSquared,
                false
        );
    }

    private static boolean isValidClaimedThreat(
            AbstractHorse mount,
            LivingEntity threat
    ) {
        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(mount, threat)
                || threat == mount
                || mount.distanceToSqr(threat) > TARGET_KEEP_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity owner = mount.getOwner();

        if (threat == owner
                || owner != null && owner.isAlliedTo(threat)
                || sharesOwner(mount, threat)) {
            return false;
        }

        if (owner instanceof Player ownerPlayer
                && threat instanceof Player targetPlayer
                && !ownerPlayer.canHarmPlayer(targetPlayer)) {
            return false;
        }

        return true;
    }

    private static boolean sharesOwner(
            AbstractHorse mount,
            LivingEntity other
    ) {
        if (!(other instanceof OwnableEntity ownable)) {
            return false;
        }

        return mount.getOwnerReference() != null
                && mount.getOwnerReference().equals(ownable.getOwnerReference());
    }

    private static boolean hasOwner(AbstractHorse mount) {
        return mount.getOwnerReference() != null;
    }

    private static boolean isSupportedSource(RetoldTargetSource source) {
        return source == RetoldTargetSource.FACTION_COMBAT
                || source == RetoldTargetSource.OWNER_DEFENSE
                || source == RetoldTargetSource.RETALIATION;
    }

    private static String combatReason(RetoldTargetSource source) {
        return switch (source) {
            case OWNER_DEFENSE -> "undead_mount_owner_defense";
            case RETALIATION -> "undead_mount_retaliation";
            default -> "undead_mount_hostility";
        };
    }

    private static void stopCombat(
            AbstractHorse mount,
            LivingEntity target
    ) {
        if (target != null && (ownsCombat(mount, target)
                || RetoldAiControl.isControlledBy(
                mount,
                RetoldAiControlOwner.UNDEAD_MOUNT
        ))) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    mount,
                    target,
                    true
            );
        } else if (target == null) {
            RetoldFactionTargetMemory.cleanupTargetState(mount);
        }

        RetoldAiControl.clearIfOwnedBy(
                mount,
                RetoldAiControlOwner.UNDEAD_MOUNT
        );
        NEXT_STRIKE_AT.remove(mount);
    }

    private static void clearStaleCombatControl(AbstractHorse mount) {
        RetoldFactionTargetMemory.cleanupTargetState(mount);
        RetoldAiControl.clearIfOwnedBy(
                mount,
                RetoldAiControlOwner.UNDEAD_MOUNT
        );
        NEXT_STRIKE_AT.remove(mount);
    }
}
