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
import cz.xefensor.retold.behavior.food.RetoldFeedingAnimations;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldAmphibianForagerEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int AMPHIBIAN_SCAN_CACHE_TICKS = 6;
    private static final int WETLAND_BLOCK_SEARCH_CACHE_TICKS = 24;
    private static final int AMPHIBIAN_PATH_INTERVAL_TICKS = 6;
    private static final int HUNT_CONTROL_TICKS = 20 * 4;
    private static final int RETURN_CONTROL_TICKS = 20 * 5;

    private static final int HUNT_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.HUNT, 1);
    private static final int RETURN_PRIORITY = RetoldAiPriorities.above(RetoldAiPriorities.REGROUP, 2);

    private static final int WETLAND_SEARCH_HORIZONTAL_RADIUS = 14;
    private static final int WETLAND_SEARCH_VERTICAL_RADIUS = 4;
    private static final double WETLAND_MEMBER_SEARCH_RADIUS_BLOCKS = 12.0D;

    private static final double PREY_SEARCH_RADIUS_BLOCKS = 12.0D;
    private static final double PREY_SEARCH_RADIUS_SQUARED =
            PREY_SEARCH_RADIUS_BLOCKS * PREY_SEARCH_RADIUS_BLOCKS;

    private static final double FAR_FROM_WETLAND_BLOCKS = 20.0D;
    private static final double FAR_FROM_WETLAND_SQUARED =
            FAR_FROM_WETLAND_BLOCKS * FAR_FROM_WETLAND_BLOCKS;

    private static final double WETLAND_REACHED_BLOCKS = 4.0D;
    private static final double WETLAND_REACHED_SQUARED =
            WETLAND_REACHED_BLOCKS * WETLAND_REACHED_BLOCKS;

    private static final double BITE_DISTANCE_BLOCKS = 2.15D;
    private static final double BITE_DISTANCE_SQUARED =
            BITE_DISTANCE_BLOCKS * BITE_DISTANCE_BLOCKS;

    private static final float FROG_BITE_DAMAGE = 4.0F;
    private static final int BITE_HUNGER_RELIEF = 22;

    private static final double FROG_HUNT_SPEED = 1.08D;
    private static final double FROG_RETURN_SPEED = 0.78D;

    private RetoldAmphibianForagerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob frog)) {
            return;
        }

        if (!(frog.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isAmphibianForager(frog)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                frog,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        handleFrog(
                level,
                frog,
                gameTime
        );
    }

    private static void handleFrog(
            ServerLevel level,
            PathfinderMob frog,
            long gameTime
    ) {
        RetoldAnimalHomeMemory wetland = getOrCreateWetland(
                level,
                frog,
                gameTime
        );

        if (RetoldAiControl.isControlledAsBy(
                frog,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER
        )) {
            continueHunt(
                    level,
                    frog,
                    gameTime
            );
            return;
        }

        if (canStartHunt(frog, gameTime)) {
            LivingEntity prey = findBestPrey(
                    level,
                    frog
            );

            if (prey != null) {
                beginHunt(
                        frog,
                        prey,
                        gameTime
                );
                return;
            }
        }

        if (wetland == null) {
            return;
        }

        if (shouldReturnToWetland(level, frog, wetland)) {
            returnToWetland(
                    frog,
                    wetland.pos(),
                    gameTime
            );
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        frog,
                        RetoldAiControlMode.REGROUP,
                        RetoldAiControlOwner.AMPHIBIAN_FORAGER
                )
                        && frog.blockPosition().distSqr(wetland.pos()) <= WETLAND_REACHED_SQUARED
        ) {
            stopControl(frog);
            RetoldAnimalHomes.markUsed(
                    frog,
                    gameTime
            );
        }
    }

    private static RetoldAnimalHomeMemory getOrCreateWetland(
            ServerLevel level,
            PathfinderMob frog,
            long gameTime
    ) {
        RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(frog);

        if (RetoldAnimalHomes.isValidFor(level, frog, existing) && isWetland(level, existing.pos())) {
            RetoldAnimalHomes.markUsed(
                    frog,
                    gameTime
            );
            return existing;
        }

        BlockPos wetlandPos = findNearestWetland(
                level,
                frog
        );

        if (wetlandPos == null) {
            return null;
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                frog,
                findNearbyWetlandMembers(
                        level,
                        frog
                ),
                wetlandPos,
                gameTime
        );
    }

    private static List<PathfinderMob> findNearbyWetlandMembers(
            ServerLevel level,
            PathfinderMob frog
    ) {
        return RetoldAiScanCache.nearby(
                level,
                frog,
                PathfinderMob.class,
                WETLAND_MEMBER_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                AMPHIBIAN_SCAN_CACHE_TICKS
        ).stream()
                .filter(
                candidate -> candidate != frog
                        && RetoldAnimalSocialGroups.canShareHomeOrRange(
                        frog,
                        candidate
                )
        ).toList();
    }

    private static boolean canStartHunt(
            PathfinderMob frog,
            long gameTime
    ) {
        if (RetoldAiControl.isControlled(frog)) {
            return false;
        }

        if (RetoldBehaviorCoordinator.hasLiveTarget(frog)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                frog,
                gameTime
        );

        return RetoldMobRules.hasProfileHuntDrive(
                frog,
                state
        );
    }

    private static LivingEntity findBestPrey(
            ServerLevel level,
            PathfinderMob frog
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                frog,
                LivingEntity.class,
                PREY_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                AMPHIBIAN_SCAN_CACHE_TICKS
        );

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidPrey(frog, candidate)) {
                continue;
            }

            double distanceSquared = frog.distanceToSqr(candidate);

            if (distanceSquared > PREY_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (RetoldAiSightCache.canSee(frog, candidate, level.getGameTime())) {
                score -= 10.0D;
            }

            if (RetoldPreyTargeting.isSlimePrey(candidate)) {
                score -= 6.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        return best;
    }

    private static void beginHunt(
            PathfinderMob frog,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                frog,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER,
                HUNT_PRIORITY,
                "frog_forage_hunt",
                gameTime,
                HUNT_CONTROL_TICKS
        )) {
            return;
        }

        chasePrey(
                frog,
                prey,
                gameTime
        );
    }

    private static void continueHunt(
            ServerLevel level,
            PathfinderMob frog,
            long gameTime
    ) {
        LivingEntity prey = frog.getTarget();

        if (!isValidPrey(frog, prey)) {
            stopFailedHunt(
                    frog,
                    gameTime
            );
            return;
        }

        if (frog.distanceToSqr(prey) <= BITE_DISTANCE_SQUARED) {
            bitePrey(
                    level,
                    frog,
                    prey,
                    gameTime
            );
            return;
        }

        RetoldAiControl.refreshIfOwnedBy(
                frog,
                RetoldAiControlMode.HUNT,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER,
                gameTime,
                HUNT_CONTROL_TICKS
        );

        chasePrey(
                frog,
                prey,
                gameTime
        );
    }

    private static void chasePrey(
            PathfinderMob frog,
            LivingEntity prey,
            long gameTime
    ) {
        if (!RetoldBehaviorTargets.setAttackTargetOrClearOwner(
                frog,
                prey,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER
        )) {
            return;
        }

        frog.getLookControl().setLookAt(
                prey,
                30.0F,
                30.0F
        );

        RetoldBehaviorMovement.throttledMoveTo(
                frog,
                prey,
                FROG_HUNT_SPEED,
                gameTime,
                AMPHIBIAN_PATH_INTERVAL_TICKS,
                2.0D * 2.0D
        );
    }

    private static void bitePrey(
            ServerLevel level,
            PathfinderMob frog,
            LivingEntity prey,
            long gameTime
    ) {
        boolean hurt = prey.hurtServer(
                level,
                frog.damageSources().mobAttack(frog),
                FROG_BITE_DAMAGE
        );

        if (!hurt) {
            stopFailedHunt(
                    frog,
                    gameTime
            );
            return;
        }

        RetoldFeedingAnimations.play(frog);

        RetoldMobState state = RetoldMobStates.getOrCreate(
                frog,
                gameTime
        );

        state.addHunger(-BITE_HUNGER_RELIEF);
        state.markAte(gameTime);

        if (!prey.isAlive() || prey.isRemoved()) {
            state.markSuccessfulHunt(gameTime);
        }

        stopControl(frog);
    }

    private static void stopFailedHunt(
            PathfinderMob frog,
            long gameTime
    ) {
        RetoldMobStates.getOrCreate(
                frog,
                gameTime
        ).markFailedHunt(gameTime);

        stopControl(frog);
    }

    private static boolean shouldReturnToWetland(
            ServerLevel level,
            PathfinderMob frog,
            RetoldAnimalHomeMemory wetland
    ) {
        if (wetland == null) {
            return false;
        }

        if (frog.blockPosition().distSqr(wetland.pos()) <= WETLAND_REACHED_SQUARED) {
            return false;
        }

        boolean farFromWetland = frog.blockPosition().distSqr(wetland.pos()) >= FAR_FROM_WETLAND_SQUARED;
        boolean dry = !frog.isInWater() && !hasNearbyWater(level, frog.blockPosition(), 5);

        return farFromWetland || dry;
    }

    private static void returnToWetland(
            PathfinderMob frog,
            BlockPos wetland,
            long gameTime
    ) {
        RetoldBehaviorMovement.claimAndMoveToBlock(
                frog,
                wetland,
                RetoldAiControlMode.REGROUP,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER,
                RETURN_PRIORITY,
                "frog_wetland_return",
                gameTime,
                RETURN_CONTROL_TICKS,
                FROG_RETURN_SPEED,
                false
        );
    }

    private static void stopControl(PathfinderMob frog) {
        RetoldBehaviorTargets.setTargetAndAggression(frog, null, false);

        frog.setSprinting(false);
        frog.getNavigation().stop();

        RetoldAiControl.clearIfOwnedBy(
                frog,
                RetoldAiControlOwner.AMPHIBIAN_FORAGER
        );
    }

    private static boolean isValidPrey(
            PathfinderMob frog,
            LivingEntity prey
    ) {
        if (!RetoldPreyTargeting.isValidNonPlayerPreyCandidate(
                frog,
                prey
        )) {
            return false;
        }

        if (frog.distanceToSqr(prey) > PREY_SEARCH_RADIUS_SQUARED) {
            return false;
        }

        return RetoldPreyTargeting.isTinyWetlandPrey(prey)
                && (
                RetoldAiSightCache.canSee(frog, prey, frog.level().getGameTime())
                        || frog.distanceToSqr(prey) <= 16.0D
        );
    }

    private static BlockPos findNearestWetland(
            ServerLevel level,
            PathfinderMob frog
    ) {
        return RetoldBlockTargetSearch.findWetland(
                level,
                frog,
                WETLAND_SEARCH_HORIZONTAL_RADIUS,
                WETLAND_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                WETLAND_BLOCK_SEARCH_CACHE_TICKS
        );
    }

    private static boolean isWetland(
            ServerLevel level,
            BlockPos pos
    ) {
        return isWater(level, pos)
                && hasNearbyLand(level, pos, 4);
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

    private static boolean hasNearbyLand(
            ServerLevel level,
            BlockPos pos,
            int radius
    ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    mutable.set(
                            pos.getX() + dx,
                            pos.getY() + dy,
                            pos.getZ() + dz
                    );

                    if (!level.getBlockState(mutable).isAir() && !isWater(level, mutable)) {
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
