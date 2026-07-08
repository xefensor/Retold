package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.core.registries.BuiltInRegistries;
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

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        if (state.hunger() < RetoldMobRules.eatThreshold(mob)) {
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
        int offset = Math.floorMod(
                mob.getId(),
                THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean isMouthFoodMob(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        return path.equals("fox")
                || path.equals("dolphin");
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
                getItemPath(stack)
        );

        state.addHunger(
                -relief
        );

        RetoldAiControl.claim(
                mob,
                RetoldAiControlMode.FEED,
                gameTime,
                FEED_CONTROL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                false
        );

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

    private static String getItemPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(
                stack.getItem()
        ).getPath();
    }
}