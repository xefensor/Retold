package cz.xefensor.retold.combat;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.territory.RetoldTerritoryTargetBlocker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

public final class RetoldFactionTargetGuards {
    private static final ThreadLocal<Boolean> IGNORE_TARGET_GUARD =
            ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<Boolean> IGNORE_AGGRESSIVE_GUARD =
            ThreadLocal.withInitial(() -> false);

    private RetoldFactionTargetGuards() {
    }

    public static boolean shouldBlockTarget(Mob mob, LivingEntity target) {
        if (mob == null || target == null) {
            return false;
        }

        if (IGNORE_TARGET_GUARD.get()) {
            return false;
        }

        if (!(mob instanceof PathfinderMob pathfinderMob)) {
            return false;
        }

        if (target == mob.getLastHurtByMob()) {
            return false;
        }

        return RetoldTerritoryTargetBlocker.shouldBlockTargetDuringWarning(
                pathfinderMob,
                target
        );
    }

    public static void setTargetIgnoringGuard(Mob mob, LivingEntity target) {
        if (mob == null) {
            return;
        }

        boolean previous = IGNORE_TARGET_GUARD.get();
        IGNORE_TARGET_GUARD.set(true);

        try {
            mob.setTarget(target);
        } finally {
            IGNORE_TARGET_GUARD.set(previous);
        }
    }

    public static void setTargetIgnoringWarning(Mob mob, LivingEntity target) {
        setTargetIgnoringGuard(mob, target);
    }

    public static void setAggressiveIgnoringGuard(Mob mob, boolean aggressive) {
        if (mob == null) {
            return;
        }

        boolean previous = IGNORE_AGGRESSIVE_GUARD.get();
        IGNORE_AGGRESSIVE_GUARD.set(true);

        try {
            mob.setAggressive(aggressive);
        } finally {
            IGNORE_AGGRESSIVE_GUARD.set(previous);
        }
    }

    public static void setAggressiveIgnoringWarning(Mob mob, boolean aggressive) {
        setAggressiveIgnoringGuard(mob, aggressive);
    }

    public static boolean shouldBlockAggressiveState(Mob mob, boolean aggressive) {
        if (mob == null) {
            return false;
        }

        if (!aggressive) {
            return false;
        }

        if (IGNORE_AGGRESSIVE_GUARD.get()) {
            return false;
        }

        if (!(mob instanceof AbstractPiglin piglin)) {
            return false;
        }

        if (RetoldFactionMembers.getFaction(piglin) != RetoldFaction.NETHER_REMNANTS) {
            return false;
        }

        LivingEntity mobTarget = piglin.getTarget();

        if (isValidTargetForMob(piglin, mobTarget)) {
            return false;
        }

        LivingEntity brainAttackTarget = piglin.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElse(null);

        return !isValidTargetForMob(piglin, brainAttackTarget);
    }

    private static boolean isValidTargetForMob(Mob mob, LivingEntity target) {
        return target != null
                && target.isAlive()
                && target.level() == mob.level();
    }
}