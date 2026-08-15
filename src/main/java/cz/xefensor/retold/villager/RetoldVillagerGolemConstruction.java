package cz.xefensor.retold.villager;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldActionFacing;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.golem.RetoldGolemAnimation;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.sensing.GolemSensor;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RetoldVillagerGolemConstruction {
    public static final int EMERALD_COST = 1;

    private static final String PERSISTENT_KEY =
            "RetoldVillagerGolemConstruction";
    private static final int SAVE_VERSION = 1;
    private static final int CACHE_TICKS = 100;
    private static final int STEP_TICKS = 40;
    private static final int CONTROL_TICKS = 20 * 5;
    private static final int CONTROL_PRIORITY = RetoldAiPriorities.SUPPORT;
    private static final double MOVEMENT_SPEED = 0.5D;
    private static final double ACCESS_DISTANCE_SQUARED = 1.25D * 1.25D;
    private static final String CONTROL_REASON = "construct_iron_golem";
    private static final ItemStack EMERALD = Items.EMERALD.getDefaultInstance();

    private RetoldVillagerGolemConstruction() {
    }

    public static void replaceVanillaSpawnAttempt(
            ServerLevel level,
            Villager builder,
            long timestamp,
            int villagersNeededToAgree
    ) {
        if (!isStageTwoOrLater(level)
                || !isUsable(level, builder)
                || !canConstructGolems(builder)
                || !builder.wantsToSpawnGolem(timestamp)
                || !RetoldMobGriefing.canModifyBlocks(level, builder)
                || hasNearbyConstruction(level, builder)) {
            return;
        }

        AABB searchBox = builder.getBoundingBox().inflate(10.0D);
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                searchBox
        );
        List<Villager> agreeingVillagers = nearbyVillagers.stream()
                .filter(villager -> villager.wantsToSpawnGolem(timestamp))
                .limit(5L)
                .toList();

        if (agreeingVillagers.size() < villagersNeededToAgree
                || !villageHasEmerald(level, builder, nearbyVillagers, timestamp)
                || !RetoldAiWorkBudget.tryUseBlockSearch(timestamp)) {
            return;
        }

        BuildState state = findBuildState(level, builder, timestamp);

        if (state == null) {
            return;
        }

        saveState(builder, state);
        nearbyVillagers.forEach(GolemSensor::golemDetected);
    }

    public static void tick(
            ServerLevel level,
            Villager builder,
            long gameTime
    ) {
        BuildState state = loadState(builder);

        if (state == null) {
            return;
        }

        if (!isStageTwoOrLater(level)
                || !isUsable(level, builder)
                || !canConstructGolems(builder)
                || !RetoldMobGriefing.canModifyBlocks(level, builder)) {
            abort(level, builder, state);
            return;
        }

        if (shouldPause(builder, gameTime)) {
            clearVisual(builder);
            clearOwnedMovement(builder);
            return;
        }

        if (!isStateValid(level, state)) {
            abort(level, builder, state);
            return;
        }

        if (builder.distanceToSqr(Vec3.atBottomCenterOf(state.access()))
                > ACCESS_DISTANCE_SQUARED) {
            showNextMaterial(builder, state.step());

            if (!claimControl(builder, gameTime)
                    || !RetoldBehaviorMovement.throttledMoveToExact(
                    builder,
                    state.access(),
                    MOVEMENT_SPEED,
                    gameTime,
                    8,
                    1.5D * 1.5D
            )) {
                clearOwnedMovement(builder);
            }
            return;
        }

        builder.getNavigation().stop();
        RetoldActionFacing.face(builder, Vec3.atCenterOf(state.center()));

        if (!claimControl(builder, gameTime) || gameTime < state.nextStepAt()) {
            showNextMaterial(builder, state.step());
            return;
        }

        advance(level, builder, state, gameTime);
    }

    public static boolean isProtectedBuildBlock(
            ServerLevel level,
            BlockPos pos
    ) {
        if (level == null || pos == null) {
            return false;
        }

        AABB searchBox = new AABB(pos).inflate(24.0D);

        for (Villager villager : level.getEntitiesOfClass(
                Villager.class,
                searchBox
        )) {
            BuildState state = loadState(villager);

            if (state != null && isPlacedPosition(state, pos)) {
                return true;
            }
        }

        return false;
    }

    static boolean isBuilding(Villager villager) {
        return loadState(villager) != null;
    }

    public static boolean requiresContinuousFacingTick(Villager villager) {
        BuildState state = villager == null ? null : loadState(villager);
        return state != null
                && villager.distanceToSqr(Vec3.atBottomCenterOf(state.access()))
                <= ACCESS_DISTANCE_SQUARED;
    }

    static boolean canConstructGolems(Villager villager) {
        if (villager == null) {
            return false;
        }

        var profession = villager.getVillagerData().profession();
        return profession.is(VillagerProfession.CLERIC)
                || profession.is(VillagerProfession.LIBRARIAN)
                || profession.is(VillagerProfession.ARMORER)
                || profession.is(VillagerProfession.TOOLSMITH)
                || profession.is(VillagerProfession.WEAPONSMITH);
    }

    static BuildState constructionState(Villager villager) {
        return loadState(villager);
    }

    public static boolean retainTradeEmerald(
            ServerLevel level,
            Villager villager,
            long gameTime
    ) {
        if (level == null || villager == null || villager.level() != level) {
            return false;
        }

        ItemStack remaining = villager.getInventory().addItem(EMERALD.copy());

        if (remaining.isEmpty()) {
            villager.getInventory().setChanged();
            return true;
        }

        BlockPos storage = RetoldVillagerCommunalFoodSearch.findForDeposit(
                level,
                villager,
                remaining,
                gameTime,
                CACHE_TICKS
        );

        if (storage == null) {
            return false;
        }

        var container = RetoldVillagerCommunalFoodSearch.containerAt(
                level,
                storage
        );

        if (container == null) {
            return false;
        }

        RetoldVillageContainerOwnership.SystemMutation ownershipMutation =
                RetoldVillageContainerOwnership.beginSystemMutation(container);

        try {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                if (!container.canPlaceItem(slot, remaining)) {
                    continue;
                }

                ItemStack stored = container.getItem(slot);

                if (stored.isEmpty()) {
                    container.setItem(slot, remaining.copy());
                    container.setChanged();
                    return true;
                }

                if (ItemStack.isSameItemSameComponents(stored, remaining)
                        && stored.getCount() < Math.min(
                        stored.getMaxStackSize(),
                        container.getMaxStackSize(stored)
                )) {
                    stored.grow(1);
                    container.setChanged();
                    return true;
                }
            }

            return false;
        } finally {
            RetoldVillageContainerOwnership.finishSystemMutation(
                    level,
                    ownershipMutation,
                    true
            );
        }
    }

    private static void advance(
            ServerLevel level,
            Villager builder,
            BuildState state,
            long gameTime
    ) {
        if (state.step() < 5) {
            BlockPos placePos = positionForStep(state, state.step());
            BlockState block = state.step() == 4
                    ? Blocks.PUMPKIN.defaultBlockState()
                    : Blocks.IRON_BLOCK.defaultBlockState();

            showNextMaterial(builder, state.step());
            level.setBlock(placePos, block, 3);
            builder.swing(InteractionHand.MAIN_HAND);
            saveState(
                    builder,
                    state.withStep(state.step() + 1, gameTime + STEP_TICKS)
            );
            return;
        }

        if (state.step() == 5) {
            if (!consumeEmerald(level, builder, gameTime)) {
                clearVisual(builder);
                return;
            }

            builder.setItemInHand(InteractionHand.MAIN_HAND, EMERALD.copy());
            builder.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            level.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    builder.getX(),
                    builder.getY() + 1.0D,
                    builder.getZ(),
                    8,
                    0.35D,
                    0.45D,
                    0.35D,
                    0.02D
            );
            saveState(builder, state.withStep(6, gameTime + STEP_TICKS));
            return;
        }

        AABB spawnBox = new AABB(state.center()).inflate(3.0D);
        Set<UUID> existingGolems = level.getEntitiesOfClass(
                IronGolem.class,
                spawnBox
        ).stream().map(IronGolem::getUUID).collect(Collectors.toSet());

        RetoldGolemAnimation.animateVillagerBuiltGolem(() -> level.setBlock(
                state.top(),
                Blocks.CARVED_PUMPKIN.defaultBlockState(),
                3
        ));

        IronGolem created = level.getEntitiesOfClass(
                IronGolem.class,
                spawnBox,
                golem -> !existingGolems.contains(golem.getUUID())
        ).stream().findFirst().orElse(null);

        if (created == null) {
            abort(level, builder, state);
            return;
        }

        created.setPlayerCreated(false);
        level.getEntitiesOfClass(
                Villager.class,
                builder.getBoundingBox().inflate(10.0D)
        ).forEach(GolemSensor::golemDetected);
        clearState(builder);
        clearVisual(builder);
        clearOwnedMovement(builder);
    }

    private static BuildState findBuildState(
            ServerLevel level,
            Villager builder,
            long gameTime
    ) {
        BlockPos origin = builder.blockPosition();

        for (int radius = 2; radius <= 8; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    for (int dy = -1; dy <= 1; dy++) {
                        BlockPos center = origin.offset(dx, dy, dz);

                        if (!isEmptyGolemSite(level, center)) {
                            continue;
                        }

                        BlockPos access = findAccess(level, builder, center);

                        if (access != null) {
                            return new BuildState(
                                    center.immutable(),
                                    access.immutable(),
                                    0,
                                    gameTime
                            );
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isEmptyGolemSite(
            ServerLevel level,
            BlockPos center
    ) {
        if (!level.getBlockState(center.below()).isFaceSturdy(
                level,
                center.below(),
                Direction.UP
        )) {
            return false;
        }

        BlockPos[] requiredAir = {
                center,
                center.above(),
                center.above().west(),
                center.above().east(),
                center.above(2),
                center.west(),
                center.east(),
                center.above(2).west(),
                center.above(2).east()
        };

        for (BlockPos pos : requiredAir) {
            if (level.isOutsideBuildHeight(pos)
                    || !level.getBlockState(pos).isAir()) {
                return false;
            }
        }

        return true;
    }

    private static BlockPos findAccess(
            ServerLevel level,
            Villager builder,
            BlockPos center
    ) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = center.relative(direction, 2);

            if (!builder.getNavigation().isStableDestination(candidate)
                    || !level.getBlockState(candidate).getCollisionShape(
                    level,
                    candidate
            ).isEmpty()
                    || !level.getBlockState(candidate.above())
                    .getCollisionShape(level, candidate.above()).isEmpty()) {
                continue;
            }

            double distance = builder.distanceToSqr(
                    Vec3.atBottomCenterOf(candidate)
            );

            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }

        return best;
    }

    private static boolean villageHasEmerald(
            ServerLevel level,
            Villager builder,
            List<Villager> nearbyVillagers,
            long gameTime
    ) {
        for (Villager villager : nearbyVillagers) {
            if (villager.getInventory().hasAnyOf(java.util.Set.of(Items.EMERALD))) {
                return true;
            }
        }

        return RetoldVillagerCommunalFoodSearch.findWithItem(
                level,
                builder,
                EMERALD,
                gameTime,
                CACHE_TICKS
        ) != null;
    }

    private static boolean consumeEmerald(
            ServerLevel level,
            Villager builder,
            long gameTime
    ) {
        for (Villager villager : level.getEntitiesOfClass(
                Villager.class,
                builder.getBoundingBox().inflate(10.0D)
        )) {
            var inventory = villager.getInventory();

            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);

                if (!stack.is(Items.EMERALD)) {
                    continue;
                }

                inventory.removeItem(slot, EMERALD_COST);
                inventory.setChanged();
                return true;
            }
        }

        BlockPos storage = RetoldVillagerCommunalFoodSearch.findWithItem(
                level,
                builder,
                EMERALD,
                gameTime,
                CACHE_TICKS
        );
        return storage != null
                && RetoldVillagerCommunalFoodSearch.takeOne(
                level,
                builder,
                storage,
                EMERALD
        ) == EMERALD_COST;
    }

    private static boolean shouldPause(Villager builder, long gameTime) {
        RetoldMobState mobState = RetoldMobStates.getOrCreate(builder, gameTime);
        var activity = builder.getBrain().getActiveNonCoreActivity();

        return RetoldMobRules.hasEatDrive(builder, mobState)
                || RetoldVillagerCommunalFood.hasUrgentVanillaActivity(builder)
                || builder.isSleeping()
                || builder.getTradingPlayer() != null
                || RetoldBehaviorCoordinator.hasLiveTarget(builder)
                || activity.isPresent()
                && activity.get() != Activity.IDLE
                && activity.get() != Activity.MEET
                && activity.get() != Activity.WORK;
    }

    private static boolean claimControl(Villager builder, long gameTime) {
        return RetoldAiControl.tryClaim(
                builder,
                RetoldAiControlMode.SUPPORT,
                RetoldAiControlOwner.VILLAGER_GOLEM_CONSTRUCTION,
                CONTROL_PRIORITY,
                CONTROL_REASON,
                gameTime,
                CONTROL_TICKS
        );
    }

    private static boolean hasNearbyConstruction(
            ServerLevel level,
            Villager builder
    ) {
        return level.getEntitiesOfClass(
                Villager.class,
                builder.getBoundingBox().inflate(32.0D),
                RetoldVillagerGolemConstruction::isBuilding
        ).stream().findAny().isPresent();
    }

    private static boolean isStateValid(
            ServerLevel level,
            BuildState state
    ) {
        for (int step = 0; step < 4; step++) {
            BlockState actual = level.getBlockState(positionForStep(state, step));

            if (step < state.step()) {
                if (!actual.is(Blocks.IRON_BLOCK)) {
                    return false;
                }
            } else if (!actual.isAir()) {
                return false;
            }
        }

        BlockState top = level.getBlockState(state.top());

        if (state.step() <= 4) {
            return top.isAir();
        }

        return top.is(Blocks.PUMPKIN);
    }

    private static boolean isPlacedPosition(
            BuildState state,
            BlockPos pos
    ) {
        for (int step = 0; step < Math.min(4, state.step()); step++) {
            if (positionForStep(state, step).equals(pos)) {
                return true;
            }
        }

        return state.step() > 4 && state.top().equals(pos);
    }

    private static BlockPos positionForStep(BuildState state, int step) {
        return switch (step) {
            case 0 -> state.center();
            case 1 -> state.center().above();
            case 2 -> state.center().above().west();
            case 3 -> state.center().above().east();
            case 4 -> state.top();
            default -> state.center();
        };
    }

    private static void showNextMaterial(Villager builder, int step) {
        ItemStack visual = step < 4
                ? Items.IRON_BLOCK.getDefaultInstance()
                : step == 4
                ? Items.PUMPKIN.getDefaultInstance()
                : step >= 6
                ? EMERALD.copy()
                : ItemStack.EMPTY;
        builder.setItemInHand(InteractionHand.MAIN_HAND, visual);
        builder.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static void clearVisual(Villager builder) {
        ItemStack held = builder.getMainHandItem();

        if (held.is(Items.IRON_BLOCK)
                || held.is(Items.PUMPKIN)
                || held.is(Items.EMERALD)) {
            builder.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }

    private static void clearOwnedMovement(Villager builder) {
        RetoldAiControl.clearIfOwnedBy(
                builder,
                RetoldAiControlOwner.VILLAGER_GOLEM_CONSTRUCTION
        );
        RetoldBehaviorMovement.stopOwnedMovement(
                builder,
                RetoldAiControlOwner.VILLAGER_GOLEM_CONSTRUCTION
        );
    }

    private static void abort(
            ServerLevel level,
            Villager builder,
            BuildState state
    ) {
        for (int step = 0; step < Math.min(4, state.step()); step++) {
            BlockPos pos = positionForStep(state, step);

            if (level.getBlockState(pos).is(Blocks.IRON_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        if (state.step() > 4
                && (level.getBlockState(state.top()).is(Blocks.PUMPKIN)
                || level.getBlockState(state.top()).is(Blocks.CARVED_PUMPKIN))) {
            level.setBlock(state.top(), Blocks.AIR.defaultBlockState(), 3);
        }

        clearState(builder);
        clearVisual(builder);
        clearOwnedMovement(builder);
    }

    private static boolean isUsable(ServerLevel level, Villager builder) {
        return level != null
                && builder != null
                && builder.level() == level
                && builder.isAlive()
                && !builder.isRemoved()
                && !builder.isNoAi()
                && !builder.isBaby();
    }

    private static boolean isStageTwoOrLater(ServerLevel level) {
        return level != null
                && RetoldWorldData.get(level).getStage().getId()
                >= RetoldWorldStage.STAGE_2.getId();
    }

    private static BuildState loadState(Villager builder) {
        if (builder == null) {
            return null;
        }

        CompoundTag tag = builder.getPersistentData()
                .getCompoundOrEmpty(PERSISTENT_KEY);

        if (tag.isEmpty() || tag.getIntOr("version", 0) != SAVE_VERSION) {
            return null;
        }

        return new BuildState(
                new BlockPos(
                        tag.getIntOr("x", builder.blockPosition().getX()),
                        tag.getIntOr("y", builder.blockPosition().getY()),
                        tag.getIntOr("z", builder.blockPosition().getZ())
                ),
                new BlockPos(
                        tag.getIntOr("accessX", builder.blockPosition().getX()),
                        tag.getIntOr("accessY", builder.blockPosition().getY()),
                        tag.getIntOr("accessZ", builder.blockPosition().getZ())
                ),
                tag.getIntOr("step", 0),
                tag.getLongOr("nextStepAt", 0L)
        );
    }

    private static void saveState(Villager builder, BuildState state) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", SAVE_VERSION);
        tag.putInt("x", state.center().getX());
        tag.putInt("y", state.center().getY());
        tag.putInt("z", state.center().getZ());
        tag.putInt("accessX", state.access().getX());
        tag.putInt("accessY", state.access().getY());
        tag.putInt("accessZ", state.access().getZ());
        tag.putInt("step", state.step());
        tag.putLong("nextStepAt", state.nextStepAt());
        builder.getPersistentData().put(PERSISTENT_KEY, tag);
    }

    private static void clearState(Villager builder) {
        builder.getPersistentData().remove(PERSISTENT_KEY);
    }

    record BuildState(
            BlockPos center,
            BlockPos access,
            int step,
            long nextStepAt
    ) {
        BlockPos top() {
            return center.above(2);
        }

        BuildState withStep(int newStep, long newNextStepAt) {
            return new BuildState(center, access, newStep, newNextStepAt);
        }
    }
}
