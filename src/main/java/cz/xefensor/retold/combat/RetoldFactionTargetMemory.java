package cz.xefensor.retold.combat;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

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

        if (!target.isAlive()) {
            return false;
        }

        if (target.level() != mob.level()) {
            return false;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(mob, target);
        TARGET_OWNERS.put(mob, new TargetOwnership(target, source));

        return mob.getTarget() == target;
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
                        && currentTarget.isAlive()
                        && currentTarget.level() == mob.level()
        ) {
            return;
        }

        clearPiglinBrainTargetIfOwned(mob, ownership.target);
        TARGET_OWNERS.remove(mob);

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

        clearPiglinBrainTargetIfOwned(mob, target);
        clearIdleIllagerAggression(mob);
    }

    private static void clearPiglinBrainTargetIfOwned(
            Mob mob,
            LivingEntity target
    ) {
        if (!(mob instanceof AbstractPiglin piglin)) {
            return;
        }

        LivingEntity brainTarget = piglin.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElse(null);

        if (brainTarget != target) {
            return;
        }

        piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);

        if (target != null) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    private static void clearIdleIllagerAggression(Mob mob) {
        if (RetoldFactionMembers.getFaction(mob) != RetoldFaction.ILLAGERS) {
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