package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

final class RetoldAnimalDailyRhythm {
    private RetoldAnimalDailyRhythm() {
    }

    static boolean shouldIdleAtHome(
            ServerLevel level,
            PathfinderMob mob
    ) {
        return !isActive(level, mob);
    }

    static boolean shouldRestAtHome(
            ServerLevel level,
            PathfinderMob mob
    ) {
        if (level.isRainingAt(mob.blockPosition())) {
            return true;
        }

        if (RetoldMobRules.isFox(mob)) {
            return isDay(level);
        }

        if (RetoldMobRules.isCatOrOcelot(mob)) {
            return isDay(level) || isLateNight(level);
        }

        if (RetoldMobRules.isWolf(mob)) {
            return isDay(level);
        }

        if (RetoldMobRules.isDolphin(mob)) {
            return isLateNight(level);
        }

        if (RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isHungryGrazer(mob)) {
            return isNight(level);
        }

        if (RetoldMobRules.isPig(mob)) {
            return isNight(level);
        }

        return false;
    }

    static boolean isActive(
            ServerLevel level,
            PathfinderMob mob
    ) {
        if (RetoldMobRules.isFox(mob)) {
            return isNight(level) || isDawn(level) || isDusk(level);
        }

        if (RetoldMobRules.isCatOrOcelot(mob)) {
            return isDawn(level) || isDusk(level) || isNight(level);
        }

        if (RetoldMobRules.isWolf(mob)) {
            return isDawn(level) || isDusk(level) || isNight(level);
        }

        if (RetoldMobRules.isDolphin(mob)) {
            return !isLateNight(level);
        }

        if (RetoldMobRules.isChicken(mob)) {
            return isDay(level) && !level.isRainingAt(mob.blockPosition());
        }

        if (RetoldMobRules.isRabbit(mob)) {
            return isDawn(level) || isDay(level) || isDusk(level);
        }

        if (RetoldMobRules.isPig(mob)) {
            return isDay(level) || isDusk(level);
        }

        if (RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && RetoldMobRules.isHungryGrazer(mob)) {
            return isDay(level) || isDusk(level);
        }

        return isDay(level);
    }

    static boolean isDay(ServerLevel level) {
        long dayTime = dayTime(level);

        return dayTime >= 1000L && dayTime < 12000L;
    }

    static boolean isNight(ServerLevel level) {
        long dayTime = dayTime(level);

        return dayTime >= 13000L || dayTime < 1000L;
    }

    static boolean isDawn(ServerLevel level) {
        long dayTime = dayTime(level);

        return dayTime >= 23000L || dayTime < 1000L;
    }

    static boolean isDusk(ServerLevel level) {
        long dayTime = dayTime(level);

        return dayTime >= 12000L && dayTime < 14000L;
    }

    static boolean isLateNight(ServerLevel level) {
        long dayTime = dayTime(level);

        return dayTime >= 16000L && dayTime < 23000L;
    }

    private static long dayTime(ServerLevel level) {
        return Math.floorMod(
                level.getOverworldClockTime(),
                24000L
        );
    }
}
