package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldActionFacing;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.event.TorchWeatherEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Low-priority village maintenance for torches extinguished by weather.
 * The indexed search is intentionally shared with the weather system so an
 * idle Villager never performs a broad block scan.
 */
public final class RetoldVillagerTorchRelighting {
    static final int HORIZONTAL_RADIUS = 8;
    static final int VERTICAL_RADIUS = 5;

    private static final int VILLAGE_RADIUS = 32;
    private static final int CAST_TICKS = 20;
    private static final int CONTROL_TICKS = 40;
    private static final int SUCCESS_COOLDOWN_TICKS = 40;
    private static final int EMPTY_SEARCH_COOLDOWN_TICKS = 80;
    static final int MAX_RELIGHTS_PER_RUN = 8;
    private static final int PATH_INTERVAL_TICKS = 8;
    private static final int PHYSICAL_ROUTE_TIMEOUT_TICKS = 200;
    private static final double MOVEMENT_SPEED = 0.5D;
    private static final double ACCESS_DISTANCE_SQUARED = 1.25D * 1.25D;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.below(
            RetoldAiPriorities.SEARCH,
            1
    );
    private static final String CONTROL_REASON = "relight_village_torch";
    private static final Map<Villager, CastState> CASTS = new WeakHashMap<>();
    private static final Map<Villager, Long> NEXT_SEARCH_AT = new WeakHashMap<>();

    private RetoldVillagerTorchRelighting() {
    }

    public static boolean requiresContinuousTick(Villager villager) {
        CastState cast = villager == null ? null : CASTS.get(villager);
        return cast != null
                && (cast.method() == RelightMethod.MAGIC
                || cast.actionAt() >= 0L);
    }

    public static void tick(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        CastState cast = CASTS.get(villager);

        if (!isUsable(level, villager)
                || !RetoldMobGriefing.canModifyBlocks(level, villager)
                || !canUseCurrentActivity(villager)
                || RetoldMobRules.hasEatDrive(
                villager,
                RetoldMobStates.getOrCreate(villager, gameTime)
        )
                || RetoldVillagerCommunalFood.hasUrgentVanillaActivity(villager)
                || RetoldBehaviorCoordinator.hasLiveTarget(villager)) {
            cancel(villager);
            return;
        }

        if (cast != null) {
            continueCast(level, villager, cast, gameTime);
            return;
        }

        if (gameTime < NEXT_SEARCH_AT.getOrDefault(villager, 0L)
                || RetoldAiControl.getMode(villager) != RetoldAiControlMode.NONE) {
            return;
        }

        StartResult result = tryStartCast(level, villager, gameTime, 0);

        if (result == StartResult.NO_TARGET) {
            scheduleNextSearch(villager, gameTime, EMPTY_SEARCH_COOLDOWN_TICKS);
        }
    }

    private static StartResult tryStartCast(
            ServerLevel level,
            Villager villager,
            long gameTime,
            int completedRelights
    ) {
        BlockPos villageAnchor = RetoldVillagerCommunalFoodSearch.villageAnchor(
                level,
                villager
        );

        if (villageAnchor == null
                || !TorchWeatherEvents.hasTrackedExtinguishedTorches(level)) {
            return StartResult.NO_TARGET;
        }

        if (!RetoldAiWorkBudget.tryUseBlockSearch(gameTime)) {
            RetoldBehaviorPerf.recordBlockSearchCache(false);
            RetoldBehaviorPerf.recordBlockSearchBudgetSkip();
            return StartResult.DEFERRED;
        }

        RetoldBehaviorPerf.recordBlockSearchCache(false);
        boolean usesFlintAndSteel = isNitwit(villager);
        BlockPos target = TorchWeatherEvents.findNearestExtinguishedTorch(
                level,
                villager.blockPosition(),
                HORIZONTAL_RADIUS,
                VERTICAL_RADIUS,
                pos -> pos.distSqr(villageAnchor)
                        <= VILLAGE_RADIUS * VILLAGE_RADIUS
                        && !TorchWeatherEvents.isPrecipitatingAt(level, pos)
                        && (!usesFlintAndSteel
                        || findPhysicalAccess(level, villager, pos) != null)
        );

        if (target == null) {
            return StartResult.NO_TARGET;
        }

        if (!claimControl(villager, gameTime)) {
            return StartResult.DEFERRED;
        }

        if (usesFlintAndSteel) {
            BlockPos access = findPhysicalAccess(level, villager, target);

            if (access == null) {
                cancel(villager);
                return StartResult.NO_TARGET;
            }

            CastState physicalUse = new CastState(
                    target.immutable(),
                    access.immutable(),
                    RelightMethod.FLINT_AND_STEEL,
                    -1L,
                    gameTime + PHYSICAL_ROUTE_TIMEOUT_TICKS,
                    ItemStack.EMPTY,
                    completedRelights
            );
            CASTS.put(villager, physicalUse);
            continueCast(level, villager, physicalUse, gameTime);
            return StartResult.STARTED;
        }

        villager.getNavigation().stop();
        face(villager, target);
        level.sendParticles(
                ParticleTypes.ENCHANT,
                villager.getX(),
                villager.getEyeY() - 0.2D,
                villager.getZ(),
                8,
                0.25D,
                0.35D,
                0.25D,
                0.05D
        );
        CASTS.put(
                villager,
                new CastState(
                        target.immutable(),
                        null,
                        RelightMethod.MAGIC,
                        gameTime + CAST_TICKS,
                        gameTime + CONTROL_TICKS,
                        ItemStack.EMPTY,
                        completedRelights
                )
        );
        return StartResult.STARTED;
    }

    private static void continueCast(
            ServerLevel level,
            Villager villager,
            CastState cast,
            long gameTime
    ) {
        if (gameTime > cast.expiresAt()) {
            cancel(villager);
            scheduleNextSearch(
                    villager,
                    gameTime,
                    EMPTY_SEARCH_COOLDOWN_TICKS
            );
            return;
        }

        if (!RetoldAiControl.isControlledAsBy(
                villager,
                RetoldAiControlMode.SUPPORT,
                RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
        ) || !isValidTarget(level, villager, cast.target())
                || !RetoldMobGriefing.canPlaceBlock(
                level,
                villager,
                cast.target()
        )) {
            cancel(villager);
            return;
        }

        if (cast.method() == RelightMethod.FLINT_AND_STEEL) {
            continuePhysicalUse(level, villager, cast, gameTime);
            return;
        }

        villager.getNavigation().stop();
        face(villager, cast.target());

        if (!claimControl(villager, gameTime) || gameTime < cast.actionAt()) {
            return;
        }

        villager.swing(InteractionHand.MAIN_HAND);
        finish(
                level,
                villager,
                cast,
                gameTime,
                TorchWeatherEvents.relightMagically(level, cast.target())
        );
    }

    private static void continuePhysicalUse(
            ServerLevel level,
            Villager villager,
            CastState cast,
            long gameTime
    ) {
        if (!isValidPhysicalAccess(level, villager, cast.access())) {
            cancel(villager);
            scheduleNextSearch(
                    villager,
                    gameTime,
                    EMPTY_SEARCH_COOLDOWN_TICKS
            );
            return;
        }

        if (villager.distanceToSqr(Vec3.atBottomCenterOf(cast.access()))
                > ACCESS_DISTANCE_SQUARED) {
            if (!claimControl(villager, gameTime)) {
                cancel(villager);
                return;
            }

            RetoldBehaviorMovement.throttledMoveToExact(
                    villager,
                    cast.access(),
                    MOVEMENT_SPEED,
                    gameTime,
                    PATH_INTERVAL_TICKS,
                    1.5D * 1.5D
            );
            return;
        }

        villager.getNavigation().stop();
        face(villager, cast.target());

        if (cast.actionAt() < 0L) {
            ItemStack previousMainHand = villager.getMainHandItem().copy();
            showFlintAndSteel(villager);
            villager.swing(InteractionHand.MAIN_HAND);
            CASTS.put(
                    villager,
                    cast.startAction(
                            gameTime + CAST_TICKS,
                            previousMainHand
                    )
            );
            return;
        }

        if (!claimControl(villager, gameTime)) {
            return;
        }

        showFlintAndSteel(villager);

        if (gameTime < cast.actionAt()) {
            return;
        }

        villager.swing(InteractionHand.MAIN_HAND);
        finish(
                level,
                villager,
                cast,
                gameTime,
                TorchWeatherEvents.relightWithFlintAndSteel(
                        level,
                        cast.target()
                )
        );
    }

    private static void finish(
            ServerLevel level,
            Villager villager,
            CastState cast,
            long gameTime,
            boolean relit
    ) {
        CASTS.remove(villager);
        restoreVisual(villager, cast);
        clearOwnedMovement(villager);

        if (relit) {
            int completedRelights = cast.completedRelights() + 1;

            if (completedRelights < MAX_RELIGHTS_PER_RUN
                    && tryStartCast(
                    level,
                    villager,
                    gameTime,
                    completedRelights
            ) == StartResult.STARTED) {
                return;
            }
        }

        scheduleNextSearch(
                villager,
                gameTime,
                relit ? SUCCESS_COOLDOWN_TICKS : EMPTY_SEARCH_COOLDOWN_TICKS
        );
    }

    private static boolean isValidTarget(
            ServerLevel level,
            Villager villager,
            BlockPos target
    ) {
        BlockPos villageAnchor = RetoldVillagerCommunalFoodSearch.villageAnchor(
                level,
                villager
        );

        if (villageAnchor == null
                || target == null
                || target.distSqr(villageAnchor)
                > VILLAGE_RADIUS * VILLAGE_RADIUS
                || TorchWeatherEvents.isPrecipitatingAt(level, target)
                || !TorchWeatherEvents.isExtinguishedTorch(
                level.getBlockState(target)
        )) {
            return false;
        }

        BlockPos center = villager.blockPosition();
        int dx = target.getX() - center.getX();
        int dy = Math.abs(target.getY() - center.getY());
        int dz = target.getZ() - center.getZ();
        return dy <= VERTICAL_RADIUS
                && dx * dx + dz * dz
                <= HORIZONTAL_RADIUS * HORIZONTAL_RADIUS;
    }

    private static boolean isUsable(
            ServerLevel level,
            Villager villager
    ) {
        return level != null
                && villager != null
                && villager.level() == level
                && villager.isAlive()
                && !villager.isRemoved()
                && !villager.isNoAi()
                && !villager.isBaby()
                && !villager.isSleeping()
                && villager.getTradingPlayer() == null;
    }

    private static boolean isNitwit(Villager villager) {
        return villager != null
                && villager.getVillagerData()
                .profession()
                .is(VillagerProfession.NITWIT);
    }

    static BlockPos findPhysicalAccess(
            ServerLevel level,
            Villager villager,
            BlockPos target
    ) {
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos candidate = target.relative(direction);

            if (!isValidPhysicalAccess(level, villager, candidate)) {
                continue;
            }

            double distanceSquared = villager.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isValidPhysicalAccess(
            ServerLevel level,
            Villager villager,
            BlockPos access
    ) {
        return access != null
                && villager.getNavigation().isStableDestination(access)
                && level.getBlockState(access)
                .getCollisionShape(level, access)
                .isEmpty()
                && level.getBlockState(access.above())
                .getCollisionShape(level, access.above())
                .isEmpty();
    }

    private static boolean canUseCurrentActivity(Villager villager) {
        var activity = villager.getBrain().getActiveNonCoreActivity();

        return activity.isEmpty()
                || activity.get() == Activity.IDLE
                || activity.get() == Activity.MEET
                || activity.get() == Activity.PLAY
                || activity.get() == Activity.WORK;
    }

    private static boolean claimControl(Villager villager, long gameTime) {
        return RetoldAiControl.tryClaim(
                villager,
                RetoldAiControlMode.SUPPORT,
                RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static void face(Villager villager, BlockPos target) {
        RetoldActionFacing.face(villager, Vec3.atCenterOf(target));
    }

    private static void showFlintAndSteel(Villager villager) {
        if (!villager.getMainHandItem().is(Items.FLINT_AND_STEEL)) {
            villager.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    Items.FLINT_AND_STEEL.getDefaultInstance()
            );
            villager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
    }

    private static void cancel(Villager villager) {
        if (villager == null) {
            return;
        }

        CastState cast = CASTS.remove(villager);
        restoreVisual(villager, cast);
        clearOwnedMovement(villager);
    }

    private static void restoreVisual(
            Villager villager,
            CastState cast
    ) {
        if (villager == null
                || cast == null
                || cast.method() != RelightMethod.FLINT_AND_STEEL
                || !villager.getMainHandItem().is(Items.FLINT_AND_STEEL)) {
            return;
        }

        villager.setItemInHand(
                InteractionHand.MAIN_HAND,
                cast.previousMainHand().copy()
        );
    }

    private static void clearOwnedMovement(Villager villager) {
        RetoldAiControl.clearIfOwnedBy(
                villager,
                RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
        );
        RetoldBehaviorMovement.stopOwnedMovement(
                villager,
                RetoldAiControlOwner.VILLAGER_TORCH_RELIGHT
        );
    }

    private static void scheduleNextSearch(
            Villager villager,
            long gameTime,
            int ticks
    ) {
        NEXT_SEARCH_AT.put(
                villager,
                gameTime + RetoldAiLod.cacheTicks(villager, ticks)
        );
    }

    private enum RelightMethod {
        MAGIC,
        FLINT_AND_STEEL
    }

    private enum StartResult {
        STARTED,
        NO_TARGET,
        DEFERRED
    }

    private record CastState(
            BlockPos target,
            BlockPos access,
            RelightMethod method,
            long actionAt,
            long expiresAt,
            ItemStack previousMainHand,
            int completedRelights
    ) {
        private CastState startAction(
                long newActionAt,
                ItemStack newPreviousMainHand
        ) {
            return new CastState(
                    target,
                    access,
                    method,
                    newActionAt,
                    expiresAt,
                    newPreviousMainHand.copy(),
                    completedRelights
            );
        }
    }
}
