package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiControl {
    private static final Map<Mob, ControlState> CONTROLS = new WeakHashMap<>();

    private static final ThreadLocal<Boolean> NAVIGATION_BYPASS =
            ThreadLocal.withInitial(() -> false);

    private RetoldAiControl() {
    }

    public static void claim(
            Mob mob,
            RetoldAiControlMode mode,
            long gameTime,
            int durationTicks
    ) {
        if (mob == null || mode == null || mode == RetoldAiControlMode.NONE) {
            clear(mob);
            return;
        }

        if (!mob.isAlive() || mob.isRemoved()) {
            clear(mob);
            return;
        }

        CONTROLS.put(
                mob,
                new ControlState(
                        mode,
                        gameTime + Math.max(1, durationTicks)
                )
        );
    }

    public static void refresh(
            Mob mob,
            RetoldAiControlMode mode,
            long gameTime,
            int durationTicks
    ) {
        claim(
                mob,
                mode,
                gameTime,
                durationTicks
        );
    }

    public static void clear(Mob mob) {
        if (mob == null) {
            return;
        }

        CONTROLS.remove(mob);
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
        return CONTROLS.size();
    }

    public static boolean shouldBlockVanillaNavigation(Mob mob) {
        if (mob == null) {
            return false;
        }

        if (NAVIGATION_BYPASS.get()) {
            return false;
        }

        ControlState state = getActiveState(mob);

        if (state == null) {
            return false;
        }

        return switch (state.mode()) {
            case FEED, HUNT, ATTACK, FLEE, REGROUP, SHELTER, TERRITORY -> true;
            case NONE -> false;
        };
    }

    public static boolean shouldBlockVanillaTarget(
            Mob mob,
            LivingEntity target
    ) {
        if (mob == null || target == null) {
            return false;
        }

        ControlState state = getActiveState(mob);

        if (state == null) {
            return false;
        }

        return switch (state.mode()) {
            case FEED, FLEE, REGROUP, SHELTER -> true;
            case HUNT, ATTACK, TERRITORY, NONE -> false;
        };
    }

    public static boolean shouldBlockVanillaAggression(
            Mob mob,
            boolean aggressive
    ) {
        if (mob == null || !aggressive) {
            return false;
        }

        ControlState state = getActiveState(mob);

        if (state == null) {
            return false;
        }

        return switch (state.mode()) {
            case FEED, FLEE, REGROUP, SHELTER -> true;
            case HUNT, ATTACK, TERRITORY, NONE -> false;
        };
    }

    public static void withNavigationBypass(Runnable runnable) {
        if (runnable == null) {
            return;
        }

        boolean previous = NAVIGATION_BYPASS.get();
        NAVIGATION_BYPASS.set(true);

        try {
            runnable.run();
        } finally {
            NAVIGATION_BYPASS.set(previous);
        }
    }

    public static void cleanup(long gameTime) {
        Iterator<Map.Entry<Mob, ControlState>> iterator =
                CONTROLS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Mob, ControlState> entry = iterator.next();
            Mob mob = entry.getKey();
            ControlState state = entry.getValue();

            if (
                    mob == null
                            || !mob.isAlive()
                            || mob.isRemoved()
                            || state == null
                            || state.isExpired(gameTime)
            ) {
                iterator.remove();
            }
        }
    }

    private static ControlState getActiveState(Mob mob) {
        if (mob == null) {
            return null;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }

        return getActiveState(
                mob,
                level.getGameTime()
        );
    }

    private static ControlState getActiveState(
            Mob mob,
            long gameTime
    ) {
        ControlState state = CONTROLS.get(mob);

        if (state == null) {
            return null;
        }

        if (state.isExpired(gameTime)) {
            CONTROLS.remove(mob);
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