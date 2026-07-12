package cz.xefensor.retold.behavior;

import cz.xefensor.retold.worldgen.delayed.RetoldAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldAnimalHomes {
    public static final String VALID = "valid";
    public static final String MISSING = "missing";
    public static final String NONE = "none";
    public static final String WRONG_DIMENSION = "wrong_dimension";
    public static final String OUTSIDE_BUILD_HEIGHT = "outside_build_height";
    public static final String STALE_UNUSED_FAR = "stale_unused_far";
    public static final String WRONG_TYPE_PREFIX = "wrong_type_expected_";

    private static final Map<PathfinderMob, RetoldAnimalHomeMemory> HOMES = new WeakHashMap<>();
    private static final int HOME_OWNER_SCAN_CACHE_TICKS = 15;
    private static final int HOME_MEMBER_POSITION_SCAN_CACHE_TICKS = 15;
    private static final long STALE_HOME_UNUSED_TICKS = 24000L * 7L;
    private static final double STALE_HOME_FORGET_DISTANCE_BLOCKS = 96.0D;
    private static final double STALE_HOME_FORGET_DISTANCE_SQUARED =
            STALE_HOME_FORGET_DISTANCE_BLOCKS * STALE_HOME_FORGET_DISTANCE_BLOCKS;

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
        String reason = invalidReason(
                level,
                mob,
                home
        );

        if (reason.equals(VALID)) {
            return true;
        }

        if (shouldForgetInvalidReason(reason)) {
            remove(mob);
        }

        return false;
    }

    public static String invalidReason(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        if (level == null || mob == null || home == null) {
            return MISSING;
        }

        if (home.type() == RetoldAnimalHomeType.NONE) {
            return NONE;
        }

        if (!home.dimension().equals(level.dimension())) {
            return WRONG_DIMENSION;
        }

        RetoldAnimalHomeType expectedType = homeTypeFor(mob);

        if (home.type() != expectedType) {
            return WRONG_TYPE_PREFIX + expectedType.name().toLowerCase();
        }

        if (isOutsideBuildHeight(level, home.pos())) {
            return OUTSIDE_BUILD_HEIGHT;
        }

        if (isStaleUnusedFarHome(level, mob, home)) {
            return STALE_UNUSED_FAR;
        }

        return VALID;
    }

    public static boolean isChunkLoaded(
            ServerLevel level,
            RetoldAnimalHomeMemory home
    ) {
        if (level == null || home == null) {
            return false;
        }

        BlockPos pos = home.pos();

        return level.hasChunk(
                SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ())
        );
    }

    public static double distanceSquaredToHome(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        if (mob == null || home == null) {
            return -1.0D;
        }

        return mob.blockPosition().distSqr(home.pos());
    }

    public static int currentHomeMemberCount(
            ServerLevel level,
            PathfinderMob reference,
            RetoldAnimalHomeMemory home
    ) {
        if (level == null || reference == null || home == null) {
            return 0;
        }

        double radius = RetoldAnimalSocialGroups.homeSeparationBlocks(reference);
        Set<PathfinderMob> counted = Collections.newSetFromMap(new IdentityHashMap<>());
        int count = 0;

        for (Map.Entry<PathfinderMob, RetoldAnimalHomeMemory> entry : HOMES.entrySet()) {
            PathfinderMob candidate = entry.getKey();

            if (
                    isCurrentHomeMember(
                            level,
                            reference,
                            candidate,
                            entry.getValue(),
                            home
                    )
                            && counted.add(candidate)
            ) {
                count++;
            }
        }

        List<PathfinderMob> members = RetoldAiScanCache.nearbyAt(
                level,
                home.pos(),
                PathfinderMob.class,
                radius,
                level.getGameTime(),
                HOME_MEMBER_POSITION_SCAN_CACHE_TICKS
        );

        for (PathfinderMob member : members) {
            if (
                    isCurrentHomeMember(
                            level,
                            reference,
                            member,
                            home
                    )
                            && counted.add(member)
            ) {
                count++;
            }
        }

        return count;
    }

    public static RetoldAnimalHomeType homeTypeFor(PathfinderMob mob) {
        if (RetoldMobRules.isWolf(mob)) {
            return RetoldAnimalHomeType.WOLF_DEN;
        }

        if (RetoldMobRules.isDolphin(mob)) {
            return RetoldAnimalHomeType.DOLPHIN_POD_RANGE;
        }

        if (RetoldMobRules.isHungryGrazer(mob)) {
            return RetoldAnimalHomeType.HERD_RANGE;
        }

        if (RetoldMobRules.isPig(mob)) {
            return RetoldAnimalHomeType.FORAGING_RANGE;
        }

        if (RetoldMobRules.isChicken(mob)) {
            return RetoldAnimalHomeType.ROOST;
        }

        if (RetoldMobRules.isRabbit(mob)) {
            return RetoldAnimalHomeType.WARREN;
        }

        if (RetoldMobRules.isFox(mob)) {
            return RetoldAnimalHomeType.FOX_DEN;
        }

        if (RetoldMobRules.isCat(mob)) {
            return RetoldAnimalHomeType.CAT_TERRITORY;
        }

        if (RetoldMobRules.isOcelot(mob)) {
            return RetoldAnimalHomeType.OCELOT_TERRITORY;
        }

        if (RetoldMobRules.isPandaBamboo(mob)) {
            return RetoldAnimalHomeType.PANDA_BAMBOO_GROVE;
        }

        if (RetoldMobRules.isSnifferForager(mob)) {
            return RetoldAnimalHomeType.SNIFFER_FORAGING_RANGE;
        }

        if (RetoldMobRules.isArmadilloDefensive(mob)) {
            return RetoldAnimalHomeType.ARMADILLO_SCRUB_RANGE;
        }

        if (RetoldMobRules.isTurtleBeach(mob)) {
            return RetoldAnimalHomeType.TURTLE_BEACH;
        }

        if (RetoldMobRules.isAmphibianForager(mob)) {
            return RetoldAnimalHomeType.AMPHIBIAN_WETLAND;
        }

        if (RetoldMobRules.isAquaticHelperPredator(mob)) {
            return RetoldAnimalHomeType.AXOLOTL_WATER_RANGE;
        }

        return RetoldAnimalHomeType.NONE;
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
        List<PathfinderMob> cappedMembers = cappedMembers(
                leader,
                members
        );

        RetoldAnimalHomeMemory existing = firstValidHome(level, leader, cappedMembers, gameTime);

        if (existing != null) {
            assignPackHome(
                    level,
                    leader,
                    cappedMembers,
                    existing,
                    gameTime
            );
            return existing;
        }

        RetoldAnimalHomeType type = homeTypeFor(leader);

        if (type == RetoldAnimalHomeType.NONE) {
            return null;
        }

        if (hasNearbyCompatibleGroupHome(
                level,
                leader,
                cappedMembers,
                fallbackPos
        )) {
            return null;
        }

        RetoldAnimalHomeMemory created = new RetoldAnimalHomeMemory(
                type,
                level.dimension(),
                fallbackPos,
                gameTime
        );
        assignPackHome(
                level,
                leader,
                cappedMembers,
                created,
                gameTime
        );
        return created;
    }

    public static RetoldAnimalHomeMemory replacePackHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            BlockPos pos,
            long gameTime
    ) {
        if (level == null || leader == null || pos == null) {
            return null;
        }

        RetoldAnimalHomeType type = homeTypeFor(leader);

        if (type == RetoldAnimalHomeType.NONE) {
            return null;
        }

        List<PathfinderMob> cappedMembers = cappedMembers(
                leader,
                members
        );
        RetoldAnimalHomeMemory replacement = new RetoldAnimalHomeMemory(
                type,
                level.dimension(),
                pos,
                gameTime
        );

        assignPackHome(
                level,
                leader,
                cappedMembers,
                replacement,
                gameTime
        );

        return replacement;
    }

    public static void remove(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        HOMES.remove(mob);
        mob.removeData(RetoldAttachments.ANIMAL_HOME_DATA.get());
    }

    public static boolean hasSameValidHomeAs(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        return hasSameValidHome(
                level,
                mob,
                home
        );
    }

    private static RetoldAnimalHomeMemory firstValidHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            long gameTime
    ) {
        RetoldAnimalHomeMemory leaderHome = validHome(level, leader, leader, gameTime);

        if (leaderHome != null) {
            return leaderHome;
        }

        for (PathfinderMob member : members) {
            RetoldAnimalHomeMemory memberHome = validHome(level, leader, member, gameTime);

            if (memberHome != null) {
                return memberHome;
            }
        }

        return null;
    }

    private static RetoldAnimalHomeMemory validHome(
            ServerLevel level,
            PathfinderMob leader,
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

        if (
                mob != leader
                        && currentHomeMemberCount(
                        level,
                        leader,
                        home
                ) >= RetoldAnimalSocialGroups.maxHomeGroupSize(leader)
        ) {
            return null;
        }

        home.markUsed(gameTime);
        return home;
    }

    private static void assignPackHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        int maxGroupSize = RetoldAnimalSocialGroups.maxHomeGroupSize(leader);
        int memberCount = currentHomeMemberCount(
                level,
                leader,
                home
        );

        if (!hasSameValidHome(level, leader, home)) {
            if (memberCount >= maxGroupSize) {
                return;
            }

            remember(leader, home, gameTime);
            memberCount++;
        } else {
            markUsed(leader, gameTime);
        }

        for (PathfinderMob member : members) {
            if (memberCount >= maxGroupSize) {
                break;
            }

            if (hasSameValidHome(level, member, home)) {
                markUsed(member, gameTime);
                continue;
            }

            remember(member, home, gameTime);
            memberCount++;
        }
    }

    private static boolean isCurrentHomeMember(
            ServerLevel level,
            PathfinderMob reference,
            PathfinderMob candidate,
            RetoldAnimalHomeMemory home
    ) {
        if (candidate == null || !candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (candidate.level() != level) {
            return false;
        }

        if (
                candidate != reference
                        && !RetoldAnimalSocialGroups.canShareHomeOrRange(
                        reference,
                        candidate
                )
        ) {
            return false;
        }

        return hasSameValidHome(
                level,
                candidate,
                home
        );
    }

    private static boolean isCurrentHomeMember(
            ServerLevel level,
            PathfinderMob reference,
            PathfinderMob candidate,
            RetoldAnimalHomeMemory candidateHome,
            RetoldAnimalHomeMemory targetHome
    ) {
        if (candidate == null || !candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (candidate.level() != level) {
            return false;
        }

        if (
                candidate != reference
                        && !RetoldAnimalSocialGroups.canShareHomeOrRange(
                        reference,
                        candidate
                )
        ) {
            return false;
        }

        return sameHome(candidateHome, targetHome)
                && invalidReason(
                level,
                candidate,
                candidateHome
        ).equals(VALID);
    }

    private static boolean hasSameValidHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        RetoldAnimalHomeMemory current = get(mob);

        return sameHome(current, home)
                && invalidReason(
                level,
                mob,
                current
        ).equals(VALID);
    }

    private static boolean sameHome(
            RetoldAnimalHomeMemory first,
            RetoldAnimalHomeMemory second
    ) {
        if (first == null || second == null) {
            return false;
        }

        return first.type() == second.type()
                && first.dimension().equals(second.dimension())
                && first.pos().equals(second.pos());
    }

    private static boolean hasNearbyCompatibleGroupHome(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            BlockPos fallbackPos
    ) {
        if (level == null || leader == null || fallbackPos == null) {
            return false;
        }

        double separation = RetoldAnimalSocialGroups.homeSeparationBlocks(leader);
        double separationSquared = separation * separation;
        for (PathfinderMob candidate : RetoldAiScanCache.nearby(
                level,
                leader,
                PathfinderMob.class,
                separation,
                level.getGameTime(),
                HOME_OWNER_SCAN_CACHE_TICKS
        )) {
            if (isNearbyCompatibleGroupHomeOwner(
                    level,
                    leader,
                    members,
                    candidate,
                    fallbackPos,
                    separationSquared
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isNearbyCompatibleGroupHomeOwner(
            ServerLevel level,
            PathfinderMob leader,
            List<PathfinderMob> members,
            PathfinderMob candidate,
            BlockPos fallbackPos,
            double separationSquared
    ) {
        if (candidate == null || candidate == leader) {
            return false;
        }

        if (members != null && members.contains(candidate)) {
            return false;
        }

        if (!RetoldAnimalSocialGroups.canShareHomeOrRange(leader, candidate)) {
            return false;
        }

        RetoldAnimalHomeMemory candidateHome = get(candidate);

        if (!isValidFor(level, candidate, candidateHome)) {
            return false;
        }

        return candidateHome.pos().distSqr(fallbackPos) < separationSquared;
    }

    private static List<PathfinderMob> cappedMembers(
            PathfinderMob leader,
            List<PathfinderMob> members
    ) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        int maxMembers = Math.max(
                0,
                RetoldAnimalSocialGroups.maxHomeGroupSize(leader) - 1
        );

        if (members.size() <= maxMembers) {
            return members;
        }

        return List.copyOf(
                members.subList(
                        0,
                        maxMembers
                )
        );
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

    private static boolean isOutsideBuildHeight(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.isOutsideBuildHeight(pos);
    }

    private static boolean isStaleUnusedFarHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home
    ) {
        if (home.lastUsedAt() <= 0L) {
            return false;
        }

        if (level.getGameTime() - home.lastUsedAt() <= STALE_HOME_UNUSED_TICKS) {
            return false;
        }

        return distanceSquaredToHome(mob, home) > STALE_HOME_FORGET_DISTANCE_SQUARED;
    }

    private static boolean shouldForgetInvalidReason(String reason) {
        return reason.equals(NONE)
                || reason.equals(WRONG_DIMENSION)
                || reason.startsWith(WRONG_TYPE_PREFIX)
                || reason.equals(OUTSIDE_BUILD_HEIGHT)
                || reason.equals(STALE_UNUSED_FAR);
    }
}
