package cz.xefensor.retold.behavior;

public final class RetoldAiPriorities {
    public static final int NONE = 0;
    public static final int HOME_IDLE = 10;
    public static final int REST = 15;
    public static final int REGROUP = 20;
    public static final int SEARCH = 30;
    public static final int SUPPORT = 35;
    public static final int HUNT = 45;
    public static final int FEED = 55;
    public static final int FACTION_PRESSURE = 58;
    public static final int SPECIAL_RANGED = 62;
    public static final int SPECIAL_STALK = 64;
    public static final int DEFENSE = 68;
    public static final int FLEE = 75;
    public static final int SHELTER = 80;
    public static final int ATTACK = 85;
    public static final int TERRITORY = 95;

    private RetoldAiPriorities() {
    }

    public static int above(int priority, int amount) {
        return priority + Math.max(0, amount);
    }

    public static int below(int priority, int amount) {
        return Math.max(
                NONE,
                priority - Math.max(0, amount)
        );
    }

    public static int defaultFor(RetoldAiControlMode mode) {
        if (mode == null) {
            return NONE;
        }

        return switch (mode) {
            case NONE -> NONE;
            case REGROUP -> REGROUP;
            case SEARCH -> SEARCH;
            case SUPPORT -> SUPPORT;
            case HUNT -> HUNT;
            case FEED -> FEED;
            case FLEE -> FLEE;
            case SHELTER -> SHELTER;
            case ATTACK -> ATTACK;
            case TERRITORY -> TERRITORY;
        };
    }

    public static String describe(int priority) {
        if (priority <= NONE) {
            return "none";
        }

        if (priority < REST) {
            return "home_idle";
        }

        if (priority < REGROUP) {
            return "rest";
        }

        if (priority < SEARCH) {
            return "regroup";
        }

        if (priority < SUPPORT) {
            return "search";
        }

        if (priority < HUNT) {
            return "support";
        }

        if (priority < FEED) {
            return "hunt";
        }

        if (priority < FLEE) {
            return "feed_or_pressure";
        }

        if (priority < SHELTER) {
            return "flee";
        }

        if (priority < ATTACK) {
            return "shelter";
        }

        if (priority < TERRITORY) {
            return "attack";
        }

        return "territory";
    }
}
