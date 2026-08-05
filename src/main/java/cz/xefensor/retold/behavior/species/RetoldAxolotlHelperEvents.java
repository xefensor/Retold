package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.home.RetoldAnimalSocialGroups;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTargets;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldAxolotlHelperEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int AXOLOTL_SCAN_CACHE_TICKS = 6;
    private static final int WATER_RANGE_BLOCK_SEARCH_CACHE_TICKS = 24;
    private static final int AXOLOTL_PATH_INTERVAL_TICKS = 6;
    private static final int HUNT_CONTROL_TICKS = 20 * 4;
    private static final int RETURN_CONTROL_TICKS = 20 * 5;

    private static final int HUNT_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 3);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 4);

    private static final int WATER_RANGE_SEARCH_HORIZONTAL_RADIUS = 14;
    private static final int WATER_RANGE_SEARCH_VERTICAL_RADIUS = 5;
    private static final double WATER_RANGE_MEMBER_SEARCH_RADIUS_BLOCKS = 14.0D;

    private static final double PREY_SEARCH_RADIUS_BLOCKS = 14.0D;
    private static final double PREY_SEARCH_RADIUS_SQUARED =
            PREY_SEARCH_RADIUS_BLOCKS * PREY_SEARCH_RADIUS_BLOCKS;

    private static final double FAR_FROM_RANGE_BLOCKS = 22.0D;
    private static final double FAR_FROM_RANGE_SQUARED =
            FAR_FROM_RANGE_BLOCKS * FAR_FROM_RANGE_BLOCKS;

    private static final double RANGE_REACHED_BLOCKS = 4.0D;
    private static final double RANGE_REACHED_SQUARED =
            RANGE_REACHED_BLOCKS * RANGE_REACHED_BLOCKS;

    private static final double BITE_DISTANCE_BLOCKS = 2.35D;
    private static final double BITE_DISTANCE_SQUARED =
            BITE_DISTANCE_BLOCKS * BITE_DISTANCE_BLOCKS;
    private static final double GUARDIAN_DEFENSE_KEEP_RADIUS_BLOCKS = 36.0D;
    private static final double GUARDIAN_DEFENSE_KEEP_RADIUS_SQUARED =
            GUARDIAN_DEFENSE_KEEP_RADIUS_BLOCKS
                    * GUARDIAN_DEFENSE_KEEP_RADIUS_BLOCKS;

    private static final float AXOLOTL_BITE_DAMAGE = 4.0F;
    private static final int BITE_HUNGER_RELIEF = 24;

    private static final double AXOLOTL_HUNT_SPEED = 1.12D;
    private static final double AXOLOTL_RETURN_SPEED = 0.92D;

    private RetoldAxolotlHelperEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob axolotl)) {
            return;
        }

        if (!(axolotl.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isAquaticHelperPredator(axolotl)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                axolotl,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handleAxolotl(
                level,
                axolotl,
                gameTime
        );
    }

    private static void handleAxolotl(
            ServerLevel level,
            PathfinderMob axolotl,
            long gameTime
    ) {
        if (RetoldAiControl.isControlledAsBy(
                axolotl,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_HELPER
        )) {
            continueGuardianDefense(
                    level,
                    axolotl,
                    gameTime
            );
            return;
        }

        RetoldAnimalHomeMemory range = getOrCreateWaterRange(
                level,
                axolotl,
                gameTime
        );

        if (RetoldAiControl.isControlledAsBy(
                axolotl,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AQUATIC_HELPER
        )) {
            continueHunt(
                    level,
                    axolotl,
                    gameTime
            );
            return;
        }

        if (canStartHunt(axolotl, gameTime)) {
            LivingEntity prey = findBestPrey(
                    level,
                    axolotl,
                    gameTime
            );

            if (prey != null) {
                beginHunt(
                        axolotl,
                        prey,
                        gameTime
                );
                return;
            }
        }

        if (range == null) {
            return;
        }

        if (shouldReturnToRange(level, axolotl, range)) {
            returnToRange(
                    axolotl,
                    range.pos(),
                    gameTime
            );
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        axolotl,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.AQUATIC_HELPER
                )
                        && axolotl.blockPosition().distSqr(range.pos()) <= RANGE_REACHED_SQUARED
        ) {
            stopControl(axolotl);
            RetoldAnimalHomes.markUsed(
                    axolotl,
                    gameTime
            );
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateWaterRange(
            ServerLevel level,
            PathfinderMob axolotl,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(axolotl);

        if (RetoldAnimalHomes.isValidFor(level, axolotl, existing) && isWaterRange(level, existing.pos())) {
            RetoldAnimalHomes.markUsed(
                    axolotl,
                    gameTime
            );
            return existing;
        }

        BlockPos rangePos = findNearestWaterRange(
                level,
                axolotl
        );

        if (rangePos == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                axolotl,
                findNearbyRangeMembers(
                        level,
                        axolotl
                ),
                rangePos,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyRangeMembers(
            ServerLevel level,
            PathfinderMob axolotl
    ) {
        return RetoldAiScanCache.nearby(
                level,
                axolotl,
                PathfinderMob.class,
                WATER_RANGE_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                AXOLOTL_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != axolotl
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        axolotl,
                        candidate
                )
        ).toList();
    }

    private static boolean canStartHunt(
            PathfinderMob axolotl,
            long gameTime
    ) {
        if (RetoldAiControl.isControlled(axolotl)) {
            return false;
        }

        if (!axolotl.isInWater()) {
            return false;
        }

        if (isLowHealth(axolotl)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(axolotl)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                axolotl,
                gameTime
        );

        return RetoldMobRules.hasProfileHuntDrive(
                axolotl,
                state
        );
    }

    private static LivingEntity findBestPrey(
            ServerLevel level,
            PathfinderMob axolotl,
            long gameTime
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                axolotl,
                LivingEntity.class,
                PREY_SEARCH_RADIUS_BLOCKS,
                gameTime,
                AXOLOTL_SCAN_CACHE_TICKS
        );

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidPrey(axolotl, candidate, gameTime)) {
                continue;
            }

            double distanceSquared = axolotl.distanceToSqr(candidate);

            if (distanceSquared > PREY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(axolotl, candidate, gameTime)) {
                score -= 10.0D;
            }

            if (RetoldPreyTargeting.isFishPrey(candidate)) {
                score -= 8.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static void beginHunt(
            PathfinderMob axolotl,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                axolotl,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AQUATIC_HELPER,
                HUNT_PRIORITY,
                "axolotl_aquatic_hunt",
                gameTime,
                HUNT_CONTROL_TICKS
        )) {
            return;
        }

        chasePrey(
                axolotl,
                prey,
                gameTime
        );
    }

    static boolean beginGuardianDefense(
            PathfinderMob axolotl,
            Guardian guardian,
            RetoldTargetSource source,
            long gameTime
    ) {
        if (!RetoldMobRules.isAquaticHelperPredator(axolotl)
                || guardian == null
                || !guardian.isAlive()
                || guardian.isRemoved()
                || guardian.level() != axolotl.level()) {
            return false;
        }

        if (source != RetoldTargetSource.RETALIATION
                && source != RetoldTargetSource.FACTION_ASSIST) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                axolotl,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_HELPER,
                RetoldAiPriorities.ATTACK,
                source == RetoldTargetSource.RETALIATION
                        ? "axolotl_guardian_retaliation"
                        : "axolotl_guardian_assist",
                gameTime,
                HUNT_CONTROL_TICKS
        )) {
            return false;
        }

        if (!RetoldCombatTargets.applyAttackTarget(
                axolotl,
                guardian,
                source
        )) {
            RetoldAiControl.clearIfOwnedBy(
                    axolotl,
                    RetoldAiControlOwner.AQUATIC_HELPER
            );
            return false;
        }

        chaseGuardian(
                axolotl,
                guardian,
                gameTime
        );
        return true;
    }

    private static void continueGuardianDefense(
            ServerLevel level,
            PathfinderMob axolotl,
            long gameTime
    ) {
        LivingEntity target = axolotl.getTarget();

        if (!(target instanceof Guardian guardian)
                || !guardian.isAlive()
                || guardian.isRemoved()
                || guardian.level() != axolotl.level()
                || axolotl.distanceToSqr(guardian)
                > GUARDIAN_DEFENSE_KEEP_RADIUS_SQUARED
                || !RetoldFactionTargetMemory.isOwnedByAny(
                axolotl,
                guardian,
                RetoldTargetSource.RETALIATION,
                RetoldTargetSource.FACTION_ASSIST
        )) {
            stopControl(axolotl);
            return;
        }

        RetoldAiControl.refreshIfOwnedBy(
                axolotl,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.AQUATIC_HELPER,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        if (axolotl.distanceToSqr(guardian) <= BITE_DISTANCE_SQUARED) {
            biteGuardian(
                    level,
                    axolotl,
                    guardian
            );
            return;
        }

        chaseGuardian(
                axolotl,
                guardian,
                gameTime
        );
    }

    private static void chaseGuardian(
            PathfinderMob axolotl,
            Guardian guardian,
            long gameTime
    ) {
        axolotl.getLookControl().setLookAt(
                guardian,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                axolotl,
                guardian,
                AXOLOTL_HUNT_SPEED,
                gameTime,
                AXOLOTL_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void biteGuardian(
            ServerLevel level,
            PathfinderMob axolotl,
            Guardian guardian
    ) {
        guardian.hurtServer(
                level,
                axolotl.damageSources().mobAttack(axolotl),
                AXOLOTL_BITE_DAMAGE
        );

        if (!guardian.isAlive() || guardian.isRemoved()) {
            stopControl(axolotl);
        }
    }

    private static void continueHunt(
            ServerLevel level,
            PathfinderMob axolotl,
            long gameTime
    ) {
        LivingEntity prey = axolotl.getTarget();

        if (isLowHealth(axolotl) || !isValidPrey(axolotl, prey, gameTime)) {
            stopFailedHunt(
                    axolotl,
                    gameTime
            );
            return;
        }

        if (axolotl.distanceToSqr(prey) <= BITE_DISTANCE_SQUARED) {
            bitePrey(
                    level,
                    axolotl,
                    prey,
                    gameTime
            );
            return;
        }

        RetoldAiControl.refreshIfOwnedBy(
                axolotl,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AQUATIC_HELPER,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        chasePrey(
                axolotl,
                prey,
                gameTime
        );
    }

    private static void chasePrey(
            PathfinderMob axolotl,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                axolotl,
                prey,
                RetoldAiControlOwner.AQUATIC_HELPER
        )) {
            return;
        }

        axolotl.getLookControl().setLookAt(
                prey,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                axolotl,
                prey,
                AXOLOTL_HUNT_SPEED,
                gameTime,
                AXOLOTL_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void bitePrey(
            ServerLevel level,
            PathfinderMob axolotl,
            LivingEntity prey,
            long gameTime
    ) {
        boolean hurt = prey.hurtServer(
                level,
                axolotl.damageSources().mobAttack(axolotl),
                AXOLOTL_BITE_DAMAGE
        );

        if (!hurt) {
            stopFailedHunt(
                    axolotl,
                    gameTime
            );
            return;
        }

        Vec3 foodSource = prey.position();
        RetoldMobState state = RetoldMobStates.getOrCreate(
                axolotl,
                gameTime
        );

        if (prey.isAlive() && !prey.isRemoved()) {
            state.addHunger(-BITE_HUNGER_RELIEF);
            state.markAte(gameTime);
        }

        stopControl(axolotl);
        RetoldFeedingPose.begin(axolotl, foodSource, gameTime);
    }

    /**
     * Credits aquatic prey killed by either vanilla Axolotl combat or a lethal
     * Retold bite. Nonlethal Retold bites continue to grant their own partial
     * meal, while lethal bites are credited here once through LivingDeathEvent.
     */
    public static boolean recordKilledPrey(
            ServerLevel level,
            PathfinderMob axolotl,
            LivingEntity prey,
            long gameTime
    ) {
        if (level == null
                || axolotl == null
                || prey == null
                || axolotl.level() != level
                || !RetoldMobRules.isAquaticHelperPredator(axolotl)
                || !(RetoldPreyTargeting.isFishPrey(prey)
                || RetoldPreyTargeting.isSquidPrey(prey)
                || RetoldPreyTargeting.isDrownedPrey(prey))) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                axolotl,
                gameTime
        );
        state.addHunger(-BITE_HUNGER_RELIEF);
        state.markAte(gameTime);
        state.markSuccessfulHunt(gameTime);
        RetoldFeedingPose.begin(axolotl, prey.position(), gameTime);
        return true;
    }

    private static void stopFailedHunt(
            PathfinderMob axolotl,
            long gameTime
    ) {
        RetoldMobStates.getOrCreate(
                axolotl,
                gameTime
        ).markFailedHunt(gameTime);

        stopControl(axolotl);
    }

    private static boolean shouldReturnToRange(
            ServerLevel level,
            PathfinderMob axolotl,
            RetoldAnimalHomeMemory range
    ) {
        if (range == null) {
            return false;
        }

        if (axolotl.blockPosition().distSqr(range.pos()) <= RANGE_REACHED_SQUARED) {
            return false;
        }

        boolean farFromRange = axolotl.blockPosition().distSqr(range.pos()) >= FAR_FROM_RANGE_SQUARED;
        boolean dry = !axolotl.isInWater() && !hasNearbyWater(level, axolotl.blockPosition(), 5);

        return farFromRange || dry;
    }

    private static void returnToRange(
            PathfinderMob axolotl,
            BlockPos range,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                axolotl,
                range,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.AQUATIC_HELPER,
                RETURN_PRIORITY,
                "axolotl_water_range_return",
                gameTime,
                RETURN_CONTROL_TICKS,
                AXOLOTL_RETURN_SPEED,
                false
        );
    }

    private static void stopControl(PathfinderMob axolotl) {
        RetoldBehaviorTargets.setTargetAndAggression(axolotl, null, false);

        axolotl.setSprinting(false);
        axolotl.getNavigation().stop();

        RetoldAiControl.clearIfOwnedBy(
                axolotl,
                RetoldAiControlOwner.AQUATIC_HELPER
        );
    }

    private static boolean isValidPrey(
            PathfinderMob axolotl,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldPreyTargeting.isValidNonPlayerPreyCandidate(
                axolotl,
                prey
        )) {
            return false;
        }

        if (axolotl.distanceToSqr(prey) > PREY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        if (
                !RetoldAiSightCache.canSee(axolotl, prey, axolotl.level().getGameTime())
                        && axolotl.distanceToSqr(prey) > 20.0D
        ) {
            return false;
        }

        if (
                RetoldPreyTargeting.isFishPrey(prey)
                        || RetoldPreyTargeting.isSquidPrey(prey)
                        || RetoldPreyTargeting.isDrownedPrey(prey)
        ) {
            return true;
        }

        return false;
    }

    private static boolean isLowHealth(PathfinderMob axolotl) {
        return axolotl.getHealth() <= axolotl.getMaxHealth() * 0.45F;
    }

    private static BlockPos findNearestWaterRange(
            ServerLevel level,
            PathfinderMob axolotl
    ) {
        return RetoldBlockTargetSearch.findWaterRange(
                level,
                axolotl,
                WATER_RANGE_SEARCH_HORIZONTAL_RADIUS,
                WATER_RANGE_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                WATER_RANGE_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isWaterRange(
            ServerLevel level,
            BlockPos pos
    ) {
        return isWater(level, pos)
                && hasNearbyWater(level, pos, 4);
    }

    private static boolean hasNearbyWater(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (isWater(level, mutable)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isWater(
            ServerLevel level,
            BlockPos pos
    ) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }
}
