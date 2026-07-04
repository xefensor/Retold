package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

public final class RetoldFactionTargetGuards {
    private static final ThreadLocal<TargetBypass> CURRENT_BYPASS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_CHECKING_TARGET = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> IS_BYPASSING_AGGRESSIVE = ThreadLocal.withInitial(() -> false);

    private RetoldFactionTargetGuards() {
    }

    public static void setTargetIgnoringWarning(Mob mob, LivingEntity target) {
        if (target == null) {
            mob.setTarget(null);
            return;
        }

        TargetBypass previousBypass = CURRENT_BYPASS.get();

        CURRENT_BYPASS.set(new TargetBypass(mob, target));

        try {
            mob.setTarget(target);
        } finally {
            if (previousBypass == null) {
                CURRENT_BYPASS.remove();
            } else {
                CURRENT_BYPASS.set(previousBypass);
            }
        }
    }

    public static void setAggressiveIgnoringGuard(Mob mob, boolean aggressive) {
        boolean previous = IS_BYPASSING_AGGRESSIVE.get();

        IS_BYPASSING_AGGRESSIVE.set(true);

        try {
            mob.setAggressive(aggressive);
        } finally {
            IS_BYPASSING_AGGRESSIVE.set(previous);
        }
    }

    public static boolean isBypassingAggressiveGuard() {
        return IS_BYPASSING_AGGRESSIVE.get();
    }

    public static boolean shouldBlockTarget(Mob mob, LivingEntity target) {
        if (target == null) {
            return false;
        }

        TargetBypass bypass = CURRENT_BYPASS.get();

        if (bypass != null && bypass.mob == mob && bypass.target == target) {
            return false;
        }

        if (IS_CHECKING_TARGET.get()) {
            return false;
        }

        if (!(mob instanceof PathfinderMob)) {
            return false;
        }

        if (mob.getLastHurtByMob() == target) {
            return false;
        }

        PathfinderMob pathfinderMob = (PathfinderMob) mob;

        IS_CHECKING_TARGET.set(true);

        try {
            return RetoldFactionTerritoryEvents.shouldBlockTargetDuringWarning(pathfinderMob, target);
        } finally {
            IS_CHECKING_TARGET.set(false);
        }
    }

    public static boolean shouldBlockAggressiveState(Mob mob, boolean aggressive) {
        if (!aggressive) {
            return false;
        }

        if (isBypassingAggressiveGuard()) {
            return false;
        }

        if (!(mob instanceof AbstractPiglin)) {
            return false;
        }

        if (RetoldFactionMembers.getFaction(mob) != RetoldFaction.NETHER_REMNANTS) {
            return false;
        }

        LivingEntity target = mob.getTarget();

        if (isValidCombatTarget(mob, target)) {
            return false;
        }

        AbstractPiglin piglin = (AbstractPiglin) mob;

        LivingEntity brainTarget = piglin.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElse(null);

        return !isValidCombatTarget(mob, brainTarget);
    }

    private static boolean isValidCombatTarget(Mob mob, LivingEntity target) {
        return target != null
                && target.isAlive()
                && mob.level() == target.level();
    }

    private static final class TargetBypass {
        private final Mob mob;
        private final LivingEntity target;

        private TargetBypass(Mob mob, LivingEntity target) {
            this.mob = mob;
            this.target = target;
        }
    }
}