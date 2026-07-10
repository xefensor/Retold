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

        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        if (path.equals("fox")) {
            return isDay(level);
        }

        if (path.equals("cat") || path.equals("ocelot")) {
            return isDay(level) || isLateNight(level);
        }

        if (path.equals("wolf")) {
            return isDay(level);
        }

        if (path.equals("dolphin")) {
            return isLateNight(level);
        }

        if (RetoldMobRules.profileType(mob) == RetoldMobProfileType.HUNGRY_GRAZER) {
            return isNight(level);
        }

        if (RetoldMobRules.isEntityPath(mob, "pig")) {
            return isNight(level);
        }

        return false;
    }

    static boolean isActive(
            ServerLevel level,
            PathfinderMob mob
    ) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        if (path.equals("fox")) {
            return isNight(level) || isDawn(level) || isDusk(level);
        }

        if (path.equals("cat") || path.equals("ocelot")) {
            return isDawn(level) || isDusk(level) || isNight(level);
        }

        if (path.equals("wolf")) {
            return isDawn(level) || isDusk(level) || isNight(level);
        }

        if (path.equals("dolphin")) {
            return !isLateNight(level);
        }

        if (path.equals("chicken")) {
            return isDay(level) && !level.isRainingAt(mob.blockPosition());
        }

        if (path.equals("rabbit")) {
            return isDawn(level) || isDay(level) || isDusk(level);
        }

        if (path.equals("pig")) {
            return isDay(level) || isDusk(level);
        }

        if (RetoldMobRules.profileType(mob) == RetoldMobProfileType.HUNGRY_GRAZER) {
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
