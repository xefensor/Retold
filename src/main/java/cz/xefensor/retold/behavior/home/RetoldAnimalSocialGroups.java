package cz.xefensor.retold.behavior.home;

import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.world.entity.PathfinderMob;

public final class RetoldAnimalSocialGroups {
    private static final int DEFAULT_HOME_GROUP_SIZE = 4;

    private RetoldAnimalSocialGroups() {
    }

    public static boolean canShareHomeOrRange(
            PathfinderMob first,
            PathfinderMob second
    ) {
        if (!basicCompatible(first, second)) {
            return false;
        }

        RetoldMobProfileType profileType = RetoldMobRules.profileType(first);

        if (profileType != RetoldMobRules.profileType(second)) {
            return false;
        }

        return socialGroup(first).equals(
                socialGroup(second)
        );
    }

    public static boolean canRecoverWith(
            PathfinderMob mob,
            PathfinderMob candidate
    ) {
        return canShareHomeOrRange(
                mob,
                candidate
        );
    }

    public static boolean isSocialRecoveryMob(PathfinderMob mob) {
        return RetoldMobRules.canUseOrdinaryLifeSystems(mob)
                && (
                RetoldMobRules.isHungryGrazer(mob)
                        || RetoldMobRules.isSmallForager(mob)
                        || RetoldMobRules.isPackPredator(mob)
                        || RetoldMobRules.isAquaticPredator(mob)
        );
    }

    public static int maxHomeGroupSize(PathfinderMob mob) {
        if (mob == null) {
            return DEFAULT_HOME_GROUP_SIZE;
        }

        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        return switch (path) {
            case "wolf" -> 5;
            case "dolphin" -> 6;
            case "cow", "mooshroom" -> 7;
            case "sheep" -> 9;
            case "goat" -> 6;
            case "horse", "donkey", "mule" -> 5;
            case "llama", "trader_llama" -> 6;
            case "camel" -> 4;
            case "pig" -> 6;
            case "chicken" -> 10;
            case "rabbit" -> 7;
            case "turtle" -> 6;
            case "frog" -> 5;
            case "axolotl" -> 5;
            case "panda" -> 4;
            case "sniffer" -> 3;
            case "armadillo" -> 4;
            case "fox", "cat", "ocelot" -> 1;
            default -> DEFAULT_HOME_GROUP_SIZE;
        };
    }

    public static double homeSeparationBlocks(PathfinderMob mob) {
        if (mob == null) {
            return 18.0D;
        }

        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        return switch (path) {
            case "wolf" -> 24.0D;
            case "dolphin" -> 32.0D;
            case "cow", "mooshroom", "sheep", "goat" -> 22.0D;
            case "horse", "donkey", "mule" -> 32.0D;
            case "llama", "trader_llama" -> 28.0D;
            case "camel" -> 36.0D;
            case "pig" -> 18.0D;
            case "chicken" -> 14.0D;
            case "rabbit" -> 16.0D;
            case "turtle" -> 24.0D;
            case "frog" -> 18.0D;
            case "axolotl" -> 20.0D;
            case "panda" -> 22.0D;
            case "sniffer" -> 34.0D;
            case "armadillo" -> 18.0D;
            case "fox" -> 28.0D;
            case "cat" -> 18.0D;
            case "ocelot" -> 24.0D;
            default -> 18.0D;
        };
    }

    public static String socialGroup(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        if (
                path.equals("horse")
                        || path.equals("donkey")
                        || path.equals("mule")
        ) {
            return "equine";
        }

        if (
                path.equals("llama")
                        || path.equals("trader_llama")
        ) {
            return "llama";
        }

        return path;
    }

    private static boolean basicCompatible(
            PathfinderMob first,
            PathfinderMob second
    ) {
        if (first == null || second == null || first == second) {
            return false;
        }

        if (!first.isAlive() || first.isRemoved()) {
            return false;
        }

        if (!second.isAlive() || second.isRemoved()) {
            return false;
        }

        return first.level() == second.level();
    }
}
