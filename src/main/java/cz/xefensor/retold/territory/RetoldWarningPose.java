package cz.xefensor.retold.territory;

import cz.xefensor.retold.combat.RetoldAiTargets;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;

public final class RetoldWarningPose {
    private RetoldWarningPose() {
    }

    public static void updateWarningPose(
            PathfinderMob mob,
            LivingEntity warningTarget,
            RetoldWarningLevel warningLevel
    ) {
        if (warningLevel == RetoldWarningLevel.NONE || warningLevel == RetoldWarningLevel.NOTICED) {
            stopWarningPose(mob);
            return;
        }

        RetoldAiTargets.setAggression(mob, true);

        if (warningLevel != RetoldWarningLevel.FINAL_WARNING && warningLevel != RetoldWarningLevel.ATTACK) {
            return;
        }

        InteractionHand weaponHand = getProjectileWeaponHand(mob);

        if (weaponHand == null) {
            return;
        }

        ItemStack weaponStack = mob.getItemInHand(weaponHand);

        if (isCrossbowWeapon(weaponStack)) {
            updateWarningCrossbowPose(mob, weaponHand, weaponStack);
            return;
        }

        if (!mob.isUsingItem()) {
            mob.startUsingItem(weaponHand);
        }
    }

    public static void stopWarningPose(PathfinderMob mob) {
        if (mob.getTarget() != null) {
            return;
        }

        RetoldAiTargets.setAggression(mob, false);

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(false);
        }

        if (mob.isUsingItem()) {
            mob.stopUsingItem();
        }
    }

    private static void updateWarningCrossbowPose(
            PathfinderMob mob,
            InteractionHand weaponHand,
            ItemStack crossbowStack
    ) {
        if (isCrossbowCharged(crossbowStack)) {
            if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
                crossbowAttackMob.setChargingCrossbow(false);
            }

            if (mob.isUsingItem()) {
                mob.stopUsingItem();
            }

            return;
        }

        if (!mob.isUsingItem()) {
            mob.startUsingItem(weaponHand);
        }

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(true);
        }

        if (mob.getTicksUsingItem() < RetoldTerritoryConstants.WARNING_CROSSBOW_CHARGE_TICKS) {
            return;
        }

        forceLoadCrossbow(crossbowStack);

        if (mob instanceof CrossbowAttackMob crossbowAttackMob) {
            crossbowAttackMob.setChargingCrossbow(false);
        }

        if (mob.isUsingItem()) {
            mob.stopUsingItem();
        }
    }

    private static boolean forceLoadCrossbow(ItemStack crossbowStack) {
        if (!isCrossbowWeapon(crossbowStack)) {
            return false;
        }

        if (isCrossbowCharged(crossbowStack)) {
            return true;
        }

        ItemStackTemplate arrowTemplate = ItemStackTemplate.fromNonEmptyStack(
                Items.ARROW.getDefaultInstance()
        );

        crossbowStack.set(
                DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.of(arrowTemplate)
        );

        return true;
    }

    private static boolean isCrossbowWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isCrossbowCharged(ItemStack stack) {
        if (!isCrossbowWeapon(stack)) {
            return false;
        }

        ChargedProjectiles chargedProjectiles = stack.get(DataComponents.CHARGED_PROJECTILES);

        return chargedProjectiles != null && !chargedProjectiles.isEmpty();
    }

    private static InteractionHand getProjectileWeaponHand(PathfinderMob mob) {
        if (isProjectileWeapon(mob.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (isProjectileWeapon(mob.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }

    private static boolean isProjectileWeapon(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ProjectileWeaponItem;
    }
}
