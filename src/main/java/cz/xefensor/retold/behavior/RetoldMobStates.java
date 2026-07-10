package cz.xefensor.retold.behavior;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.PathfinderMob;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldMobStates {
    private static final String PERSISTENT_KEY = "RetoldMobState";

    private static final Map<PathfinderMob, RetoldMobState> STATES = new WeakHashMap<>();

    private RetoldMobStates() {
    }

    public static RetoldMobState getOrCreate(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldMobState state = STATES.get(mob);

        if (state == null) {
            state = loadState(mob);
            bindSaveCallback(
                    mob,
                    state
            );

            if (state.lastHungerTickAt() <= 0L) {
                state.markHungerTick(gameTime);
            }

            STATES.put(
                    mob,
                    state
            );
        }

        state.markSeen(gameTime);

        return state;
    }

    public static RetoldMobState get(PathfinderMob mob) {
        if (mob == null) {
            return null;
        }

        return STATES.get(mob);
    }

    public static void remove(PathfinderMob mob) {
        if (mob == null) {
            return;
        }

        STATES.remove(mob);
        mob.getPersistentData().remove(PERSISTENT_KEY);
    }

    public static int activeCount() {
        return STATES.size();
    }

    public static void cleanup(long gameTime) {
        Iterator<Map.Entry<PathfinderMob, RetoldMobState>> iterator =
                STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<PathfinderMob, RetoldMobState> entry = iterator.next();
            PathfinderMob mob = entry.getKey();
            RetoldMobState state = entry.getValue();

            if (
                    mob == null
                            || !mob.isAlive()
                            || mob.isRemoved()
                            || state == null
                            || gameTime - state.lastSeenAt() > 20L * 60L
            ) {
                iterator.remove();
            }
        }
    }

    private static RetoldMobState loadState(PathfinderMob mob) {
        if (mob == null) {
            return new RetoldMobState();
        }

        CompoundTag persistentData = mob.getPersistentData();

        if (!persistentData.contains(PERSISTENT_KEY)) {
            return new RetoldMobState();
        }

        return RetoldMobState.load(
                persistentData.getCompoundOrEmpty(PERSISTENT_KEY)
        );
    }

    private static void bindSaveCallback(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        WeakReference<PathfinderMob> mobReference = new WeakReference<>(mob);

        state.setSaveCallback(() -> {
            PathfinderMob referencedMob = mobReference.get();

            if (referencedMob == null || referencedMob.isRemoved()) {
                return;
            }

            referencedMob.getPersistentData().put(
                    PERSISTENT_KEY,
                    state.save()
            );
        });
    }
}
