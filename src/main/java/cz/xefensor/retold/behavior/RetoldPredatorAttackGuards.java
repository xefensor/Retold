package cz.xefensor.retold.behavior;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldPredatorAttackGuards {
    private static final ThreadLocal<Boolean> ALLOW_RETOLED_PREDATOR_HURT =
            ThreadLocal.withInitial(() -> false);

    private RetoldPredatorAttackGuards() {
    }

    public static boolean shouldBlockVanillaPredatorHurt(
            Mob attacker,
            LivingEntity target
    ) {
        if (attacker == null || target == null) {
            return false;
        }

        if (ALLOW_RETOLED_PREDATOR_HURT.get()) {
            return false;
        }

        if (!(attacker instanceof PathfinderMob predator)) {
            return false;
        }

        if (!RetoldMobRules.isManagedPredator(predator)) {
            return false;
        }

        /*
         * Only block the vanilla food-prey attack layer.
         * This does NOT block things like wolves fighting skeletons,
         * because skeletons are enemies, not wolf food.
         */
        return RetoldMobRules.canHuntPrey(
                predator,
                target,
                predator.level().getGameTime()
        );
    }

    public static boolean doRetoldPredatorHurt(
            PathfinderMob predator,
            LivingEntity target
    ) {
        ALLOW_RETOLED_PREDATOR_HURT.set(true);

        try {
            return predator.doHurtTarget(
                    target.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
                            ? serverLevel
                            : null,
                    target
            );
        } finally {
            ALLOW_RETOLED_PREDATOR_HURT.set(false);
        }
    }
}