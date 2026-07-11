package cz.xefensor.retold.combat;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.territory.RetoldTerritoryTargetBlocker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFactionTargetMemory {
    private static final Map<Mob, TargetOwnership> TARGET_OWNERS =
            new WeakHashMap<>();

    private RetoldFactionTargetMemory() {
    }

    public static boolean trySetTarget(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (mob == null || target == null || source == null) {
            return false;
        }

        if (!RetoldAiTargets.isValidAssignmentTarget(mob, target)) {
            return false;
        }

        if (shouldBlockDuringTerritoryWarning(mob, target, source)) {
            clearBlockedWarningTarget(mob, target);
            return false;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(mob, target);

        if (mob.getTarget() != target) {
            return false;
        }

        TARGET_OWNERS.put(mob, new TargetOwnership(target, source));
        RetoldAiTargets.setPiglinBrainTargetIfNeeded(mob, target);

        return true;
    }

    public static void clearTargetIfOwnedBy(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (mob == null || target == null || source == null) {
            return;
        }

        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership == null) {
            return;
        }

        if (ownership.target != target) {
            return;
        }

        if (ownership.source != source) {
            return;
        }

        clearOwnedTarget(mob, target);
    }

    public static void clearTargetIfOwnedByAny(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource... sources
    ) {
        if (mob == null || target == null || sources == null || sources.length == 0) {
            return;
        }

        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership == null) {
            return;
        }

        if (ownership.target != target) {
            return;
        }

        if (!Arrays.asList(sources).contains(ownership.source)) {
            return;
        }

        clearOwnedTarget(mob, target);
    }

    public static boolean isOwnedByAny(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource... sources
    ) {
        if (mob == null || target == null || sources == null || sources.length == 0) {
            return false;
        }

        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership == null) {
            return false;
        }

        if (ownership.target != target) {
            return false;
        }

        return Arrays.asList(sources).contains(ownership.source);
    }

    public static String debugOwnershipText(Mob mob) {
        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership == null) {
            return "none";
        }

        LivingEntity currentTarget = mob == null ? null : mob.getTarget();
        LivingEntity brainTarget = mob == null ? null : RetoldAiTargets.getBrainAttackTargetSafely(mob);
        String targetState = targetStateText(ownership, currentTarget, brainTarget);

        return ownership.source + ":" + targetState + ":" + entityText(ownership.target);
    }

    public static void cleanupTargetState(Mob mob) {
        if (mob == null) {
            return;
        }

        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership == null) {
            clearIdleIllagerAggression(mob);
            return;
        }

        LivingEntity currentTarget = mob.getTarget();
        LivingEntity brainTarget = RetoldAiTargets.getBrainAttackTargetSafely(mob);

        if (
                isCurrentOwnedTarget(mob, ownership, currentTarget)
                        || isCurrentOwnedTarget(mob, ownership, brainTarget)
        ) {
            return;
        }

        RetoldAiTargets.clearPiglinBrainTargetIfPresent(mob, ownership.target);
        TARGET_OWNERS.remove(mob);

        clearIdleIllagerAggression(mob);
    }

    public static void clearTargetOwnership(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null || target == null) {
            return;
        }

        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership != null && ownership.target == target) {
            TARGET_OWNERS.remove(mob);
        }
    }

    private static void clearOwnedTarget(
            Mob mob,
            LivingEntity target
    ) {
        TargetOwnership ownership = TARGET_OWNERS.get(mob);

        if (ownership != null && ownership.target == target) {
            TARGET_OWNERS.remove(mob);
        }

        if (mob.getTarget() == target) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(mob, null);
        }

        RetoldAiTargets.clearPiglinBrainTargetIfPresent(mob, target);
        clearIdleIllagerAggression(mob);
    }

    private static boolean isCurrentOwnedTarget(
            Mob mob,
            TargetOwnership ownership,
            LivingEntity target
    ) {
        return target == ownership.target
                && target != null
                && RetoldAiTargets.isValidAssignmentTarget(mob, target);
    }

    private static String targetStateText(
            TargetOwnership ownership,
            LivingEntity currentTarget,
            LivingEntity brainTarget
    ) {
        if (currentTarget == ownership.target && brainTarget == ownership.target) {
            return "current+brain";
        }

        if (currentTarget == ownership.target) {
            return "current";
        }

        if (brainTarget == ownership.target) {
            return "brain";
        }

        return "stale";
    }

    private static boolean shouldBlockDuringTerritoryWarning(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource source
    ) {
        if (source == RetoldTargetSource.TERRITORY_ATTACK || source == RetoldTargetSource.RETALIATION) {
            return false;
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return false;
        }

        return RetoldTerritoryTargetBlocker.shouldBlockTargetDuringWarning(
                pathfinderMob,
                target
        );
    }

    private static void clearBlockedWarningTarget(
            Mob mob,
            LivingEntity target
    ) {
        RetoldCombatTargets.clearTargetReferencesAndAggression(mob, target, true);
    }

    private static void clearIdleIllagerAggression(Mob mob) {
        if (!RetoldFactionMembers.isIllager(mob)) {
            return;
        }

        if (mob.getTarget() != null) {
            return;
        }

        RetoldAiTargets.setAggression(mob, false);
    }

    private static String entityText(LivingEntity entity) {
        if (entity == null) {
            return "none";
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getPath() + "#" + entity.getId();
    }

    private static final class TargetOwnership {
        private final LivingEntity target;
        private final RetoldTargetSource source;

        private TargetOwnership(
                LivingEntity target,
                RetoldTargetSource source
        ) {
            this.target = target;
            this.source = source;
        }
    }
}
