package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.species.RetoldUndeadStagePressure;
import cz.xefensor.retold.worldgen.fire.Wildfire;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Central perceptual distance boundary for new and retained combat targets.
 *
 * <p>Species owners may use smaller scan or release radii. This policy is the final shared ceiling
 * that also covers vanilla goals, Brain memories, faction scans, and relayed targets.</p>
 */
public final class RetoldTargetRangePolicy {
    private static final int RECENT_DIRECT_ATTACK_TICKS = 20 * 5;

    private static final double MIN_UNKNOWN_ACQUISITION_RANGE_BLOCKS = 12.0D;
    private static final double MAX_UNKNOWN_ACQUISITION_RANGE_BLOCKS = 48.0D;

    private RetoldTargetRangePolicy() {
    }

    public static boolean shouldBlockTargetAssignment(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (!RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            return false;
        }

        boolean retained = mob.getTarget() == target
                || RetoldAiTargets.getBrainAttackTargetSafely(mob) == target;
        double allowedRange = retained
                ? retentionRange(mob, target)
                : acquisitionRange(mob, target);

        if (!retained && isUrgentAcquisition(mob, target, source)) {
            allowedRange = retentionRange(mob, target);
        } else if (!retained && source == RetoldTargetSource.THREAT_RESPONSE) {
            allowedRange = Math.max(allowedRange, 24.0D);
        }

        return mob.distanceToSqr(target) > allowedRange * allowedRange;
    }

    public static boolean isWithinAcquisitionRange(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        return mob != null
                && target != null
                && !shouldBlockTargetAssignment(mob, target, source);
    }

    public static boolean isWithinRetentionRange(
            Mob mob,
            LivingEntity target
    ) {
        if (!RetoldAiTargets.isAliveInSameLevel(mob, target)) {
            return false;
        }

        double range = retentionRange(mob, target);
        return mob.distanceToSqr(target) <= range * range;
    }

    public static double maximumAcquisitionRange(Mob mob) {
        if (mob == null) {
            return 0.0D;
        }

        if (mob instanceof Wildfire
                || RetoldMobRules.isGhastArtillery(mob)) {
            return 64.0D;
        }

        return acquisitionRange(mob, null);
    }

    public static boolean releaseTargetIfBeyondRetention(Mob mob) {
        if (mob == null) {
            return false;
        }

        LivingEntity mobTarget = mob.getTarget();
        LivingEntity brainTarget = RetoldAiTargets.getBrainAttackTargetSafely(mob);
        boolean released = releaseIfBeyondRetention(mob, mobTarget);

        if (brainTarget != mobTarget) {
            released |= releaseIfBeyondRetention(mob, brainTarget);
        }

        return released;
    }

    private static boolean releaseIfBeyondRetention(
            Mob mob,
            LivingEntity target
    ) {
        if (target == null || isWithinRetentionRange(mob, target)) {
            return false;
        }

        RetoldFactionTargetMemory.clearTargetOwnership(mob, target);
        RetoldAiTargets.clearAllMatchingTargetReferences(
                mob,
                target,
                true
        );
        return true;
    }

    private static boolean isUrgentAcquisition(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (source == RetoldTargetSource.RETALIATION
                || source == RetoldTargetSource.OWNER_DEFENSE
                || source == RetoldTargetSource.TERRITORY_ATTACK) {
            return true;
        }

        LivingEntity recentAttacker = mob.getLastHurtByMob();
        int attackAge = mob.tickCount - mob.getLastHurtByMobTimestamp();

        return recentAttacker == target
                && attackAge >= 0
                && attackAge <= RECENT_DIRECT_ATTACK_TICKS;
    }

    private static double acquisitionRange(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null) {
            return 0.0D;
        }

        if (mob instanceof Wildfire) {
            return target != null && target.getType() == EntityTypes.GHAST
                    ? 64.0D
                    : 40.0D;
        }

        RetoldMobProfileType type = RetoldMobRules.profileType(mob);

        return switch (type) {
            case HUNGRY_GRAZER,
                 SMALL_FORAGER,
                 AQUATIC_SCHOOL,
                 LOOSE_AQUATIC_GROUP,
                 PANDA_BAMBOO,
                 SNIFFER_FORAGER,
                 ARMADILLO_DEFENSIVE,
                 TURTLE_BEACH,
                 PARROT_FORAGER,
                 VILLAGER_COMMUNAL -> 16.0D;
            case PACK_PREDATOR,
                 SOLO_OPPORTUNIST,
                 AQUATIC_PREDATOR,
                 HUNGRY_SWARM_PREDATOR,
                 HIVE_COLONY,
                 SLIME_HUNGRY,
                 SMALL_ARTHROPOD_SWARM,
                 PROTECTIVE_NEUTRAL,
                 BAT_COLONY -> 18.0D;
            case AMPHIBIAN_FORAGER -> 12.0D;
            case AQUATIC_HELPER_PREDATOR -> 14.0D;
            case NETHER_HUNGRY,
                 UNDEAD_MOUNT,
                 AQUATIC_TERRITORY_GUARD,
                 TERRITORY_GUARD,
                 COMMANDER_SUPPORT,
                 ILLAGER_RAIDER -> 24.0D;
            case UNDEAD_HUNGRY -> zombieAcquisitionRange(mob);
            case UNDEAD_TOLERANT -> skeletonAcquisitionRange(mob);
            case PHANTOM_STALKER -> 42.0D;
            case GHAST_ARTILLERY -> 64.0D;
            case ZOGLIN_RAMPAGER -> 24.0D;
            case SPECIAL_VANILLA -> specialVanillaAcquisitionRange(mob);
            case APEX_OR_BOSS -> apexAcquisitionRange(mob);
            case NONE -> unknownAcquisitionRange(mob);
        };
    }

    private static double retentionRange(
            Mob mob,
            LivingEntity target
    ) {
        if (mob instanceof Wildfire) {
            return target != null && target.getType() == EntityTypes.GHAST
                    ? 64.0D
                    : 48.0D;
        }

        RetoldMobProfileType type = RetoldMobRules.profileType(mob);

        return switch (type) {
            case PHANTOM_STALKER -> 56.0D;
            case GHAST_ARTILLERY -> 80.0D;
            case ZOGLIN_RAMPAGER -> 42.0D;
            case UNDEAD_MOUNT -> 36.0D;
            case PROTECTIVE_NEUTRAL -> 34.0D;
            case HIVE_COLONY -> 36.0D;
            case BAT_COLONY -> 32.0D;
            case UNDEAD_HUNGRY -> zombieAcquisitionRange(mob) + 12.0D;
            case UNDEAD_TOLERANT -> skeletonAcquisitionRange(mob) + 12.0D;
            case SPECIAL_VANILLA -> specialVanillaRetentionRange(mob);
            case APEX_OR_BOSS -> apexRetentionRange(mob);
            case NONE -> Math.min(
                    unknownAcquisitionRange(mob) + 12.0D,
                    64.0D
            );
            default -> acquisitionRange(mob, target) + 12.0D;
        };
    }

    private static double zombieAcquisitionRange(Mob mob) {
        if (mob.level() instanceof ServerLevel level) {
            return RetoldUndeadStagePressure.zombieNoticeRadius(level);
        }

        return 12.0D;
    }

    private static double skeletonAcquisitionRange(Mob mob) {
        if (mob.level() instanceof ServerLevel level) {
            return RetoldUndeadStagePressure.skeletonNoticeRadius(level);
        }

        return 14.0D;
    }

    private static double specialVanillaAcquisitionRange(Mob mob) {
        if (mob.getType() == EntityTypes.CREEPER) {
            return 16.0D;
        }

        if (mob.getType() == EntityTypes.ENDERMAN) {
            return 32.0D;
        }

        return 24.0D;
    }

    private static double specialVanillaRetentionRange(Mob mob) {
        if (mob.getType() == EntityTypes.CREEPER) {
            return 28.0D;
        }

        if (mob.getType() == EntityTypes.ENDERMAN) {
            return 48.0D;
        }

        return 36.0D;
    }

    private static double apexAcquisitionRange(Mob mob) {
        if (mob.getType() == EntityTypes.ENDER_DRAGON) {
            return 96.0D;
        }

        if (mob.getType() == EntityTypes.WITHER) {
            return 40.0D;
        }

        return 32.0D;
    }

    private static double apexRetentionRange(Mob mob) {
        if (mob.getType() == EntityTypes.ENDER_DRAGON) {
            return 128.0D;
        }

        if (mob.getType() == EntityTypes.WITHER) {
            return 64.0D;
        }

        return 48.0D;
    }

    private static double unknownAcquisitionRange(Mob mob) {
        double followRange = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return Math.max(
                MIN_UNKNOWN_ACQUISITION_RANGE_BLOCKS,
                Math.min(followRange, MAX_UNKNOWN_ACQUISITION_RANGE_BLOCKS)
        );
    }
}
