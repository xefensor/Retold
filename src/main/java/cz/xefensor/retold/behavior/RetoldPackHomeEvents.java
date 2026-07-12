package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldPackHomeEvents {
    private static final int THINK_INTERVAL_TICKS = 20;

    private RetoldPackHomeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isHomeReturningMob(mob)) {
            return;
        }

        if (!shouldThink(mob, level.getGameTime())) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
            home = RetoldPackHomeCreation.tryCreatePassiveHome(
                    level,
                    mob,
                    level.getGameTime()
            );

            if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
                return;
            }
        }

        RetoldPackHomeReturn.updateHomeReturn(
                level,
                mob,
                home,
                level.getGameTime()
        );
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static boolean isHomeReturningMob(PathfinderMob mob) {
        return RetoldPackAnimals.isValidPackAnimal(mob);
    }

}
