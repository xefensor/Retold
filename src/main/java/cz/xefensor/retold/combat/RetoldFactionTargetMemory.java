package cz.xefensor.retold.combat;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.territory.RetoldTerritoryTargetBlocker;
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

        if (
                currentTarget == ownership.target
                        && currentTarget != null
                        && RetoldAiTargets.isValidAssignmentTarget(mob, currentTarget)
        ) {
            return;
        }

        RetoldAiTargets.clearPiglinBrainTargetIfPresent(mob, ownership.target);
        TARGET_OWNERS.remove(mob);

        clearIdleIllagerAggression(mob);
    }

    public static void clearTargetReferences(
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

        RetoldAiTargets.clearTargetAndAggression(mob, target, false);
        clearIdleIllagerAggression(mob);
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
        RetoldAiTargets.clearTargetAndAggression(mob, target, true);
    }

    private static void clearIdleIllagerAggression(Mob mob) {
        if (!RetoldFactionMembers.isMemberOf(mob, RetoldFaction.ILLAGERS)) {
            return;
        }

        if (mob.getTarget() != null) {
            return;
        }

        mob.setAggressive(false);
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
