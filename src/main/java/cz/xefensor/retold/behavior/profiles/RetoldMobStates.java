package cz.xefensor.retold.behavior.profiles;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Mob;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldMobStates {
    private static final String PERSISTENT_KEY = "RetoldMobState";
    private static final long INACTIVE_STATE_CLEANUP_TICKS = 20L * 20L;

    private static final Map<Mob, RetoldMobState> STATES = new WeakHashMap<>();

    private RetoldMobStates() {
    }

    public static RetoldMobState getOrCreate(
            Mob mob,
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

    public static RetoldMobState get(Mob mob) {
        if (mob == null) {
            return null;
        }

        return STATES.get(mob);
    }

    public static void remove(Mob mob) {
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
        Iterator<Map.Entry<Mob, RetoldMobState>> iterator =
                STATES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Mob, RetoldMobState> entry = iterator.next();
            Mob mob = entry.getKey();
            RetoldMobState state = entry.getValue();

            if (
                    mob == null
                            || !mob.isAlive()
                            || mob.isRemoved()
                            || state == null
                            || gameTime - state.lastSeenAt() > INACTIVE_STATE_CLEANUP_TICKS
            ) {
                iterator.remove();
            }
        }
    }

    private static RetoldMobState loadState(Mob mob) {
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
            Mob mob,
            RetoldMobState state
    ) {
        WeakReference<Mob> mobReference = new WeakReference<>(mob);

        state.setSaveCallback(() -> {
            Mob referencedMob = mobReference.get();

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
