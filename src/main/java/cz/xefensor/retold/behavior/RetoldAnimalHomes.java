package cz.xefensor.retold.behavior;

import cz.xefensor.retold.worldgen.delayed.RetoldAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAnimalHomes {
    private static final Map<PathfinderMob, RetoldAnimalHomeMemory> HOMES = new WeakHashMap<>();

    private RetoldAnimalHomes() {
    }

    public static RetoldAnimalHomeMemory get(PathfinderMob mob) {
        if (mob == null) {
            return null;
        }

        RetoldAnimalHomeMemory memory = HOMES.get(mob);

        if (memory != null) {
            return memory;
        }

        return loadPersistedHome(mob);
    }

    public static boolean isValidFor(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        if (level == null || mob == null || home == null) {
            return false;
        }

        if (home.type() == RetoldAnimalHomeType.NONE) {
            return false;
        }

        if (!home.dimension().equals(level.dimension())) {
            return false;
        }

        return home.type() == homeTypeFor(mob);
    }

    public static void markUsed(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldAnimalHomeMemory home = get(mob);

        if (home != null) {
            home.markUsed(gameTime);
            persist(mob, home);
        }
    }

    public static RetoldAnimalHomeMemory getOrCreatePackHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            BlockPos fallbackPos,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = firstValidHome(level, leader, members, gameTime);

        if (existing != null) {
            assignPackHome(leader, members, existing, gameTime);
            return existing;
        }

        RetoldAnimalHomeType type = homeTypeFor(leader);

        if (type == RetoldAnimalHomeType.NONE) {
            return null;
        }

        RetoldAnimalHomeMemory created = new RetoldAnimalHomeMemory(
                type,
                level.dimension(),
                fallbackPos,
                gameTime
        );
        assignPackHome(leader, members, created, gameTime);
        return created;
    }

    public static void remove(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        HOMES.remove(mob);
        mob.removeData(RetoldAttachments.ANIMAL_HOME_DATA.get());
    }

    private static RetoldAnimalHomeMemory firstValidHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            long gameTime
    ) {
        RetoldAnimalHomeMemory leaderHome = validHome(level, leader, gameTime);

        if (leaderHome != null) {
            return leaderHome;
        }

        for (PathfinderMob member : members) {
            RetoldAnimalHomeMemory memberHome = validHome(level, member, gameTime);

            if (memberHome != null) {
                return memberHome;
            }
        }

        return null;
    }

    private static RetoldAnimalHomeMemory validHome(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldAnimalHomeMemory home = get(mob);

        if (home == null || home.type() == RetoldAnimalHomeType.NONE) {
            return null;
        }

        if (!isValidFor(level, mob, home)) {
            return null;
        }

        home.markUsed(gameTime);
        return home;
    }

    private static void assignPackHome(
            PathfinderMob leader,
            List<PathfinderMob> members,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        remember(leader, home, gameTime);

        for (PathfinderMob member : members) {
            remember(member, home, gameTime);
        }
    }

    private static void remember(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        if (mob == null || home == null) {
            return;
        }

        home.markUsed(gameTime);
        HOMES.put(mob, home);
        persist(mob, home);
    }

    private static RetoldAnimalHomeMemory loadPersistedHome(PathfinderMob mob) {
        RetoldAnimalHomeData data = mob.getExistingDataOrNull(
                RetoldAttachments.ANIMAL_HOME_DATA.get()
        );

        if (data == null) {
            return null;
        }

        RetoldAnimalHomeMemory memory = data.toMemory();

        if (memory == null) {
            return null;
        }

        HOMES.put(mob, memory);
        return memory;
    }

    private static void persist(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        if (mob == null || home == null) {
            return;
        }

        mob.setData(
                RetoldAttachments.ANIMAL_HOME_DATA.get(),
                RetoldAnimalHomeData.fromMemory(home)
        );
    }

    private static RetoldAnimalHomeType homeTypeFor(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(mob.getType());

        if (path.equals("wolf")) {
            return RetoldAnimalHomeType.WOLF_DEN;
        }

        if (path.equals("dolphin")) {
            return RetoldAnimalHomeType.DOLPHIN_POD_RANGE;
        }

        if (RetoldMobProfiles.isType(path, RetoldMobProfileType.HUNGRY_GRAZER)) {
            return RetoldAnimalHomeType.HERD_RANGE;
        }

        if (path.equals("pig")) {
            return RetoldAnimalHomeType.FORAGING_RANGE;
        }

        if (path.equals("chicken")) {
            return RetoldAnimalHomeType.ROOST;
        }

        if (path.equals("rabbit")) {
            return RetoldAnimalHomeType.WARREN;
        }

        return RetoldAnimalHomeType.NONE;
    }
}
