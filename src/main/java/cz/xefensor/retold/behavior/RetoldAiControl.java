package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiControl {
    private static final Map<Mob, ControlState> CONTROLLED_MOBS = new WeakHashMap<>();
    private static final ThreadLocal<Boolean> NAVIGATION_BYPASS = ThreadLocal.withInitial(() -> false);

    private RetoldAiControl() {
    }

    public static void claim(
            Mob mob,
            RetoldAiControlMode mode,
            long gameTime,
            int ticks
    ) {
        if (mob == null || mode == null || mode == RetoldAiControlMode.NONE) {
            return;
        }

        CONTROLLED_MOBS.put(
                mob,
                new ControlState(
                        mode,
                        gameTime + Math.max(
                                1,
                                ticks
                        )
                )
        );
    }

    public static void refresh(
            Mob mob,
            RetoldAiControlMode mode,
            long gameTime,
            int ticks
    ) {
        claim(
                mob,
                mode,
                gameTime,
                ticks
        );
    }

    public static void clear(Mob mob) {
        if (mob == null) {
            return;
        }

        CONTROLLED_MOBS.remove(mob);
    }

    public static boolean isControlled(Mob mob) {
        return getActiveState(mob) != null;
    }

    public static boolean isControlledAs(
            Mob mob,
            RetoldAiControlMode mode
    ) {
        ControlState state = getActiveState(mob);

        return state != null
                && state.mode() == mode;
    }

    public static RetoldAiControlMode getMode(Mob mob) {
        ControlState state = getActiveState(mob);

        if (state == null) {
            return RetoldAiControlMode.NONE;
        }

        return state.mode();
    }

    public static int activeCount() {
        return CONTROLLED_MOBS.size();
    }

    public static boolean shouldBlockVanillaNavigation(Mob mob) {
        if (mob == null) {
            return false;
        }

        if (Boolean.TRUE.equals(NAVIGATION_BYPASS.get())) {
            return false;
        }

        RetoldAiControlMode mode = getMode(mob);

        return mode == RetoldAiControlMode.FEED
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.ATTACK
                || mode == RetoldAiControlMode.FLEE
                || mode == RetoldAiControlMode.REGROUP
                || mode == RetoldAiControlMode.SHELTER
                || mode == RetoldAiControlMode.TERRITORY;
    }

    public static boolean shouldBlockVanillaTarget(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null || target == null) {
            return false;
        }

        RetoldAiControlMode mode = getMode(mob);

        return mode == RetoldAiControlMode.FEED
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.FLEE
                || mode == RetoldAiControlMode.REGROUP
                || mode == RetoldAiControlMode.SHELTER;
    }

    public static boolean shouldBlockVanillaAggression(
            Mob mob,
            boolean aggressive
    ) {
        if (mob == null || !aggressive) {
            return false;
        }

        RetoldAiControlMode mode = getMode(mob);

        return mode == RetoldAiControlMode.FEED
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.FLEE
                || mode == RetoldAiControlMode.REGROUP
                || mode == RetoldAiControlMode.SHELTER;
    }

    public static void withNavigationBypass(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        boolean previous = Boolean.TRUE.equals(NAVIGATION_BYPASS.get());

        NAVIGATION_BYPASS.set(true);

        try {
            runnable.run();
        } finally {
            NAVIGATION_BYPASS.set(previous);
        }
    }

    public static void cleanup(long gameTime) {
        CONTROLLED_MOBS.entrySet().removeIf(entry -> {
            Mob mob = entry.getKey();
            ControlState state = entry.getValue();

            if (mob == null || state == null) {
                return true;
            }

            if (!mob.isAlive() || mob.isRemoved()) {
                return true;
            }

            return state.isExpired(gameTime);
        });
    }

    private static ControlState getActiveState(Mob mob) {
        if (mob == null) {
            return null;
        }

        ControlState state = CONTROLLED_MOBS.get(mob);

        if (state == null) {
            return null;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return state;
        }

        if (state.isExpired(level.getGameTime())) {
            CONTROLLED_MOBS.remove(mob);
            return null;
        }

        return state;
    }

    private record ControlState(
            RetoldAiControlMode mode,
            long expiresAt
    ) {
        public boolean isExpired(long gameTime) {
            return gameTime > expiresAt;
        }
    }
}