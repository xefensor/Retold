package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.species.RetoldSlimeStarvationBehavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

/** Applies bounded starvation consequences after an unloaded hunger timeline resolves. */
public final class RetoldUnloadedStarvation {
    private static final float MINIMUM_PROTECTED_HEALTH = 1.0F;

    private RetoldUnloadedStarvation() {
    }

    /**
     * Applies the accumulated critical pulses in one bounded transaction and
     * reports whether the original mob can continue catch-up processing.
     */
    public static boolean apply(
            ServerLevel level,
            Mob mob,
            RetoldMobState state,
            long gameTime,
            int criticalPulses
    ) {
        if (level == null
                || mob == null
                || state == null
                || criticalPulses <= 0
                || !mob.isAlive()
                || mob.isRemoved()) {
            return mob != null && mob.isAlive() && !mob.isRemoved();
        }

        if (isProtectedFromOfflineDeath(mob)) {
            mob.setHealth(Math.max(
                    MINIMUM_PROTECTED_HEALTH,
                    mob.getHealth() - criticalPulses
            ));
            return true;
        }

        if (mob instanceof AbstractCubeMob
                && mob instanceof PathfinderMob pathfinderMob) {
            RetoldSlimeStarvationBehavior.tryApplyCriticalHunger(
                    level,
                    pathfinderMob,
                    state,
                    gameTime
            );
            return mob.isAlive() && !mob.isRemoved();
        }

        mob.hurtServer(
                level,
                level.damageSources().starve(),
                criticalPulses
        );
        return mob.isAlive() && !mob.isRemoved();
    }

    private static boolean isProtectedFromOfflineDeath(Mob mob) {
        return mob.hasCustomName()
                || mob instanceof TamableAnimal tamable && tamable.isTame()
                || mob instanceof AbstractHorse horse && horse.isTamed();
    }
}
