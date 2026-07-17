package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;

final class RetoldPackAnimals {
    private RetoldPackAnimals() {
    }

    static boolean isValidPackAnimal(PathfinderMob mob) {
        if (!RetoldBehaviorCoordinator.isUsableMob(mob)) {
            return false;
        }

        if (!isPackHunter(mob)) {
            return false;
        }

        return !(mob instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame());
    }

    static boolean isPackHunter(PathfinderMob mob) {
        return RetoldMobRules.isPackSocialHunter(mob);
    }
}
