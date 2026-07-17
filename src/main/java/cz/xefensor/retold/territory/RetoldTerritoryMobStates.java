package cz.xefensor.retold.territory;

import net.minecraft.world.entity.PathfinderMob;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldTerritoryMobStates {
    private static final Map<PathfinderMob, RetoldTerritoryMobState> MOB_STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private RetoldTerritoryMobStates() {
    }

    public static Map<PathfinderMob, RetoldTerritoryMobState> states() {
        return MOB_STATES;
    }

    public static RetoldTerritoryMobState get(PathfinderMob mob) {
        return MOB_STATES.get(mob);
    }

    public static RetoldTerritoryMobState getOrCreate(PathfinderMob mob) {
        return MOB_STATES.computeIfAbsent(
                mob,
                ignored -> new RetoldTerritoryMobState()
        );
    }

    public static void remove(PathfinderMob mob) {
        RetoldTerritoryMobState state = MOB_STATES.remove(mob);

        if (state != null) {
            RetoldTerritoryStateMachine.deactivate(mob, state, mob.level().getGameTime());
        }
    }

    public static void clearMobState(PathfinderMob mob) {
        remove(mob);
    }
}
