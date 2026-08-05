package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.species.RetoldSlimeStarvationBehavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

public final class RetoldStarvationBehavior {
    public static final int CRITICAL_HUNGER = 100;
    public static final float DAMAGE_PER_HUNGER_INTERVAL = 1.0F;

    private RetoldStarvationBehavior() {
    }

    /**
     * Applies one species-metabolism starvation pulse and reports whether the
     * original mob can continue its current tick. Cube Mobs retain their
     * established split-or-die response instead of also taking generic damage.
     */
    public static boolean applyCriticalHunger(
            ServerLevel level,
            Mob mob,
            RetoldMobState state,
            long gameTime
    ) {
        if (level == null
                || mob == null
                || state == null
                || state.hunger() < CRITICAL_HUNGER) {
            return true;
        }

        if (mob instanceof AbstractCubeMob cubeMob
                && mob instanceof PathfinderMob pathfinderMob
                && RetoldMobRules.isSlimeHungry(cubeMob)) {
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
                DAMAGE_PER_HUNGER_INTERVAL
        );
        return mob.isAlive() && !mob.isRemoved();
    }
}
