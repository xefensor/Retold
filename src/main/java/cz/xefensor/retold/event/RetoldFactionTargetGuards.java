package cz.xefensor.retold.event;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldFactionTargetGuards {
    private static final ThreadLocal<TargetBypass> CURRENT_BYPASS = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_CHECKING_TARGET = ThreadLocal.withInitial(() -> false);

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
            if (mob.getTarget() != target) {
                mob.setTarget(target);
            }
        } finally {
            if (previousBypass == null) {
                CURRENT_BYPASS.remove();
            } else {
                CURRENT_BYPASS.set(previousBypass);
            }
        }
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

        LivingEntity lastHurtByMob = mob.getLastHurtByMob();

        if (lastHurtByMob == target) {
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

    private static final class TargetBypass {
        private final Mob mob;
        private final LivingEntity target;

        private TargetBypass(Mob mob, LivingEntity target) {
            this.mob = mob;
            this.target = target;
        }
    }
}