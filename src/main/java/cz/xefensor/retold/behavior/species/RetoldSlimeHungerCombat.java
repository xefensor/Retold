package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldSlimeHungerCombat {
    private RetoldSlimeHungerCombat() {
    }

    public static boolean hasHuntDrive(
            PathfinderMob slime,
            long gameTime
    ) {
        if (slime == null || !RetoldMobRules.isSlimeHungry(slime)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                slime,
                gameTime
        );

        return RetoldMobRules.hasProfileHuntDrive(slime, state);
    }

    public static boolean shouldBlockHostility(Mob attacker) {
        if (!(attacker instanceof PathfinderMob slime)
                || !RetoldMobRules.isSlimeHungry(slime)
                || !(slime.level() instanceof ServerLevel level)) {
            return false;
        }

        return !hasHuntDrive(
                slime,
                level.getGameTime()
        );
    }
}
