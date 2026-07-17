package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldHeldFoodConsumptionEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int FEED_CONTROL_TICKS = 20 * 2;

    private RetoldHeldFoodConsumptionEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isMouthFoodMob(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        if (!RetoldBehaviorCoordinator.canFeedNow(mob)) {
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        if (!RetoldMobRules.hasEatDrive(
                mob,
                state
        )) {
            return;
        }

        if (shouldCarryHeldFoodHome(mob)) {
            return;
        }

        if (tryConsumeHeldFood(mob, state, gameTime, EquipmentSlot.MAINHAND)) {
            return;
        }

        tryConsumeHeldFood(
                mob,
                state,
                gameTime,
                EquipmentSlot.OFFHAND
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

    private static boolean isMouthFoodMob(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        return path.equals("fox")
                || path.equals("dolphin");
    }

    private static boolean shouldCarryHeldFoodHome(PathfinderMob mob) {
        if (!RetoldMobRules.isFox(mob)) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
            return false;
        }

        if (home.type() != RetoldAnimalHomeType.FOX_DEN) {
            return false;
        }

        return mob.blockPosition().distSqr(home.pos()) > 6.0D * 6.0D;
    }

    private static boolean tryConsumeHeldFood(
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime,
            EquipmentSlot slot
    ) {
        ItemStack stack = mob.getItemBySlot(
                slot
        );

        if (stack.isEmpty()) {
            return false;
        }

        if (!RetoldMobRules.canEatDroppedItem(mob, stack)) {
            return false;
        }

        consumeOneHeldFood(
                mob,
                state,
                gameTime,
                slot,
                stack
        );

        return true;
    }

    private static void consumeOneHeldFood(
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime,
            EquipmentSlot slot,
            ItemStack stack
    ) {
        int relief = RetoldMobRules.foodRelief(
                mob,
                RetoldMobRules.getItemPath(stack)
        );

        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.FEED,
                RetoldAiControlOwner.FOOD,
                gameTime,
                FEED_CONTROL_TICKS
        )) {
            return;
        }

        state.addHunger(
                -relief
        );
        state.markFed(gameTime);

        RetoldFeedingAnimations.play(mob);

        RetoldBehaviorTargets.setTargetAndAggression(mob, null, false);

        mob.setSprinting(false);
        mob.getNavigation().stop();

        stack.shrink(
                1
        );

        if (stack.isEmpty()) {
            mob.setItemSlot(
                    slot,
                    ItemStack.EMPTY
            );
        } else {
            mob.setItemSlot(
                    slot,
                    stack
            );
        }
    }

}
