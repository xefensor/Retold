package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Comparator;
import java.util.List;

public final class RetoldAnimalHomeRepairEvents {
    private static final int THINK_INTERVAL_TICKS = 80;

    private RetoldAnimalHomeRepairEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!shouldThink(mob, level.getGameTime())) {
            return;
        }

        if (!RetoldAnimalSocialGroups.isSocialRecoveryMob(mob)) {
            return;
        }

        if (!canRepairHomeNow(mob)) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
            return;
        }

        int maxGroupSize = RetoldAnimalSocialGroups.maxHomeGroupSize(mob);
        int currentGroupSize = RetoldAnimalHomes.currentHomeMemberCount(
                level,
                mob,
                home
        );

        if (currentGroupSize <= maxGroupSize) {
            return;
        }

        repairOverflowHome(
                level,
                mob,
                home,
                maxGroupSize
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

    private static boolean canRepairHomeNow(PathfinderMob mob) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.REGROUP;
    }

    private static void repairOverflowHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            int maxGroupSize
    ) {
        double separation = RetoldAnimalSocialGroups.homeSeparationBlocks(mob);
        AABB area = new AABB(home.pos()).inflate(separation);

        List<PathfinderMob> members = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isRepairCandidate(
                        level,
                        mob,
                        candidate,
                        home
                )
        );

        members.sort(
                Comparator
                        .<PathfinderMob>comparingDouble(candidate -> candidate.blockPosition().distSqr(home.pos()))
                        .thenComparingInt(PathfinderMob::getId)
        );

        int index = members.indexOf(mob);

        if (index < maxGroupSize) {
            return;
        }

        RetoldAnimalHomes.remove(mob);

        if (RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.REGROUP)) {
            RetoldAiControl.clearIfControlledAs(mob, RetoldAiControlMode.REGROUP);
            mob.getNavigation().stop();
        }
    }

    private static boolean isRepairCandidate(
            ServerLevel level,
            PathfinderMob reference,
            PathfinderMob candidate,
            RetoldAnimalHomeMemory home
    ) {
        if (!RetoldBehaviorCoordinator.isUsableMob(candidate)) {
            return false;
        }

        if (candidate.level() != level) {
            return false;
        }

        if (
                candidate != reference
                        && !RetoldAnimalSocialGroups.canShareHomeOrRange(
                        reference,
                        candidate
                )
        ) {
            return false;
        }

        return RetoldAnimalHomes.hasSameValidHomeAs(
                level,
                candidate,
                home
        );
    }
}
