package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldTerritoryGuardEvents {
    private static final String PERSISTENT_KEY = "RetoldGuardPost";
    private static final int SAVE_VERSION = 1;

    private static final Map<PathfinderMob, GuardPost> GUARD_POSTS = new WeakHashMap<>();

    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int RETURN_CONTROL_TICKS = 20 * 5;
    private static final int RETURN_PRIORITY = 95;
    private static final int DYNAMIC_POST_UPDATE_INTERVAL_TICKS = 20 * 30;
    private static final int STATIC_ANCHOR_REPAIR_INTERVAL_TICKS = 20 * 45;

    private static final double TARGET_LEASH_FROM_POST_BLOCKS = 40.0D;
    private static final double TARGET_LEASH_FROM_POST_SQUARED =
            TARGET_LEASH_FROM_POST_BLOCKS * TARGET_LEASH_FROM_POST_BLOCKS;

    private static final double GUARD_LEASH_FROM_POST_BLOCKS = 34.0D;
    private static final double GUARD_LEASH_FROM_POST_SQUARED =
            GUARD_LEASH_FROM_POST_BLOCKS * GUARD_LEASH_FROM_POST_BLOCKS;

    private static final double RETURN_START_DISTANCE_BLOCKS = 18.0D;
    private static final double RETURN_START_DISTANCE_SQUARED =
            RETURN_START_DISTANCE_BLOCKS * RETURN_START_DISTANCE_BLOCKS;

    private static final double RETURN_STOP_DISTANCE_BLOCKS = 3.0D;
    private static final double RETURN_STOP_DISTANCE_SQUARED =
            RETURN_STOP_DISTANCE_BLOCKS * RETURN_STOP_DISTANCE_BLOCKS;

    private static final double DYNAMIC_POST_MAX_DRIFT_BLOCKS = 10.0D;
    private static final double DYNAMIC_POST_MAX_DRIFT_SQUARED =
            DYNAMIC_POST_MAX_DRIFT_BLOCKS * DYNAMIC_POST_MAX_DRIFT_BLOCKS;

    private static final double DYNAMIC_POST_MIN_UPDATE_BLOCKS = 4.0D;
    private static final double DYNAMIC_POST_MIN_UPDATE_SQUARED =
            DYNAMIC_POST_MIN_UPDATE_BLOCKS * DYNAMIC_POST_MIN_UPDATE_BLOCKS;

    private static final double RETURN_SPEED = 0.78D;

    private static final int STATIC_ANCHOR_HORIZONTAL_RADIUS = 8;
    private static final int STATIC_ANCHOR_VERTICAL_RADIUS = 5;
    private static final int MONUMENT_ANCHOR_HORIZONTAL_RADIUS = 14;
    private static final int MONUMENT_ANCHOR_VERTICAL_RADIUS = 8;

    private RetoldTerritoryGuardEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob guard)) {
            return;
        }

        if (!(guard.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isGuardHandledHere(guard)) {
            GUARD_POSTS.remove(guard);
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(guard, gameTime)) {
            return;
        }

        GuardPost post = getOrCreatePost(guard);
        LivingEntity target = guard.getTarget();

        post = repairStaticStructureAnchorIfNeeded(
                guard,
                post,
                target,
                gameTime
        );

        if (target != null && !isValidTargetForGuard(guard, target)) {
            releaseTarget(guard, target);
            target = null;
        }

        if (target != null) {
            if (isPulledTooFarFromPost(guard, target, post)) {
                releaseTarget(guard, target);
                returnToPost(guard, post, gameTime);
            } else if (RetoldAiControl.isControlledAsBy(
                    guard,
                    RetoldAiControlMode.TERRITORY,
                    RetoldAiControlOwner.TERRITORY
            )) {
                RetoldAiControl.clearIfOwnedBy(
                        guard,
                        RetoldAiControlOwner.TERRITORY
                );
            }

            return;
        }

        updateDynamicGuardPostIfIdle(
                guard,
                post,
                gameTime
        );

        if (distanceSquaredToPost(guard, post) > RETURN_START_DISTANCE_SQUARED) {
            returnToPost(guard, post, gameTime);
            return;
        }

        if (
                RetoldAiControl.isControlledAsBy(
                        guard,
                        RetoldAiControlMode.TERRITORY,
                        RetoldAiControlOwner.TERRITORY
                )
                        && distanceSquaredToPost(guard, post) <= RETURN_STOP_DISTANCE_SQUARED
        ) {
            stopReturning(guard);
        }
    }

    public static String debugPostText(PathfinderMob guard) {
        if (guard == null) {
            return "none";
        }

        GuardPost post = getPost(guard);

        if (post == null) {
            return "none";
        }

        long gameTime = guard.level() instanceof ServerLevel level
                ? level.getGameTime()
                : 0L;

        return post.pos().toShortString()
                + " dynamic="
                + yesNo(post.dynamic())
                + " updated="
                + ticksAgoText(gameTime, post.updatedAt());
    }

    public static boolean hasGuardPost(PathfinderMob guard) {
        return getPost(guard) != null;
    }

    public static boolean setGuardPostToCurrentPosition(
            PathfinderMob guard,
            long gameTime
    ) {
        if (guard == null || !isGuardHandledHere(guard)) {
            return false;
        }

        savePost(
                guard,
                new GuardPost(
                        guard.blockPosition().immutable(),
                        isDynamicPostGuard(guard),
                        gameTime
                )
        );

        return true;
    }

    public static boolean clearGuardPost(PathfinderMob guard) {
        if (guard == null) {
            return false;
        }

        GUARD_POSTS.remove(guard);
        guard.getPersistentData().remove(PERSISTENT_KEY);
        return true;
    }

    private static boolean isGuardHandledHere(PathfinderMob guard) {
        return RetoldMobRules.isTerritoryGuard(guard)
                && !RetoldMobRules.isApexOrBoss(guard);
    }

    private static boolean shouldThink(
            PathfinderMob guard,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                guard,
                gameTime,
                THINK_INTERVAL_TICKS
        );
    }

    private static GuardPost getOrCreatePost(PathfinderMob guard) {
        GuardPost existing = getPost(guard);

        if (existing != null) {
            return existing;
        }

        BlockPos anchor = findStaticGuardAnchor(guard);

        GuardPost created = new GuardPost(
                anchor,
                isDynamicPostGuard(guard),
                guard.level() instanceof ServerLevel level
                        ? level.getGameTime()
                        : 0L
        );

        savePost(
                guard,
                created
        );

        return created;
    }

    private static BlockPos findStaticGuardAnchor(PathfinderMob guard) {
        if (!(guard.level() instanceof ServerLevel level)) {
            return guard.blockPosition().immutable();
        }

        if (!shouldUseNearbyStructureAnchor(guard)) {
            return guard.blockPosition().immutable();
        }

        BlockPos center = guard.blockPosition();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int horizontalRadius = staticAnchorHorizontalRadius(guard);
        int verticalRadius = staticAnchorVerticalRadius(guard);

        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                for (int dy = -verticalRadius; dy <= verticalRadius; dy++) {
                    mutable.set(
                            center.getX() + dx,
                            center.getY() + dy,
                            center.getZ() + dz
                    );

                    if (level.isOutsideBuildHeight(mutable)) {
                        continue;
                    }

                    if (!isStructureAnchorBlock(guard, level.getBlockState(mutable))) {
                        continue;
                    }

                    double score = dx * dx + dz * dz + Math.abs(dy) * 1.5D;

                    if (score < bestScore) {
                        bestScore = score;
                        best = mutable.immutable();
                    }
                }
            }
        }

        return best == null
                ? center.immutable()
                : best;
    }

    private static GuardPost getPost(PathfinderMob guard) {
        if (guard == null) {
            return null;
        }

        GuardPost existing = GUARD_POSTS.get(guard);

        if (existing != null) {
            return existing;
        }

        GuardPost loaded = loadPersistedPost(guard);

        if (loaded != null) {
            GUARD_POSTS.put(
                    guard,
                    loaded
            );
        }

        return loaded;
    }

    private static boolean isValidTargetForGuard(
            PathfinderMob guard,
            LivingEntity target
    ) {
        return target != null
                && target.isAlive()
                && !target.isRemoved()
                && target.level() == guard.level();
    }

    private static boolean isPulledTooFarFromPost(
            PathfinderMob guard,
            LivingEntity target,
            GuardPost post
    ) {
        if (post == null) {
            return false;
        }

        if (distanceSquaredToPost(guard, post) > GUARD_LEASH_FROM_POST_SQUARED) {
            return true;
        }

        return target.blockPosition().distSqr(post.pos()) > TARGET_LEASH_FROM_POST_SQUARED;
    }

    private static void updateDynamicGuardPostIfIdle(
            PathfinderMob guard,
            GuardPost post,
            long gameTime
    ) {
        if (post == null || !post.dynamic()) {
            return;
        }

        if (!isDynamicPostGuard(guard)) {
            return;
        }

        if (RetoldAiControl.isControlled(guard)) {
            return;
        }

        if (guard.getTarget() != null) {
            return;
        }

        if (gameTime - post.updatedAt() < DYNAMIC_POST_UPDATE_INTERVAL_TICKS) {
            return;
        }

        double distanceSquared = distanceSquaredToPost(guard, post);

        if (distanceSquared > DYNAMIC_POST_MAX_DRIFT_SQUARED) {
            return;
        }

        if (distanceSquared < DYNAMIC_POST_MIN_UPDATE_SQUARED) {
            savePost(
                    guard,
                    post.withUpdatedAt(gameTime)
            );
            return;
        }

        savePost(
                guard,
                new GuardPost(
                        guard.blockPosition().immutable(),
                        true,
                        gameTime
                )
        );
    }

    private static GuardPost repairStaticStructureAnchorIfNeeded(
            PathfinderMob guard,
            GuardPost post,
            LivingEntity target,
            long gameTime
    ) {
        if (post == null || post.dynamic()) {
            return post;
        }

        if (target != null && target.isAlive()) {
            return post;
        }

        if (!shouldUseNearbyStructureAnchor(guard)) {
            return post;
        }

        if (!(guard.level() instanceof ServerLevel level)) {
            return post;
        }

        if (gameTime - post.updatedAt() < STATIC_ANCHOR_REPAIR_INTERVAL_TICKS) {
            return post;
        }

        if (isStructureAnchorBlock(guard, level.getBlockState(post.pos()))) {
            GuardPost refreshed = post.withUpdatedAt(gameTime);
            savePost(
                    guard,
                    refreshed
            );
            return refreshed;
        }

        BlockPos anchor = findStaticGuardAnchor(guard);
        GuardPost repaired = new GuardPost(
                anchor,
                false,
                gameTime
        );

        savePost(
                guard,
                repaired
        );

        return repaired;
    }

    private static void releaseTarget(
            PathfinderMob guard,
            LivingEntity target
    ) {
        RetoldFactionTargetMemory.clearTargetIfOwnedByAny(
                guard,
                target,
                RetoldTargetSource.FACTION_ASSIST,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldTargetSource.TERRITORY_ATTACK,
                RetoldTargetSource.RETALIATION
        );

        RetoldBehaviorTargets.clearTargetAndAggression(guard, target, false);
    }

    private static void returnToPost(
            PathfinderMob guard,
            GuardPost post,
            long gameTime
    ) {
        if (post == null) {
            return;
        }

        RetoldBehaviorMovement.claimAndMoveToBlock(
                guard,
                post.pos(),
                RetoldAiControlMode.TERRITORY,
                RetoldAiControlOwner.TERRITORY,
                RETURN_PRIORITY,
                "return_to_guard_post",
                gameTime,
                RETURN_CONTROL_TICKS,
                RETURN_SPEED,
                false
        );
    }

    private static void stopReturning(PathfinderMob guard) {
        RetoldBehaviorMovement.stopOwnedMovement(
                guard,
                RetoldAiControlOwner.TERRITORY
        );
    }

    private static double distanceSquaredToPost(
            PathfinderMob guard,
            GuardPost post
    ) {
        if (guard == null || post == null) {
            return 0.0D;
        }

        return guard.blockPosition().distSqr(post.pos());
    }

    private static boolean isDynamicPostGuard(PathfinderMob guard) {
        String path = RetoldMobRules.getEntityTypePath(guard.getType());

        return path.equals("iron_golem")
                || path.equals("snow_golem");
    }

    private static boolean shouldUseNearbyStructureAnchor(PathfinderMob guard) {
        String path = RetoldMobRules.getEntityTypePath(guard.getType());

        return path.equals("wither_skeleton")
                || path.equals("blaze")
                || isOceanMonumentGuard(path);
    }

    private static boolean isStructureAnchorBlock(
            PathfinderMob guard,
            BlockState state
    ) {
        String path = RetoldMobRules.getEntityTypePath(guard.getType());

        if (path.equals("wither_skeleton") || path.equals("blaze")) {
            return state.is(Blocks.NETHER_BRICKS)
                    || state.is(Blocks.NETHER_BRICK_FENCE)
                    || state.is(Blocks.NETHER_BRICK_STAIRS)
                    || state.is(Blocks.NETHER_BRICK_SLAB)
                    || state.is(Blocks.CRACKED_NETHER_BRICKS)
                    || state.is(Blocks.CHISELED_NETHER_BRICKS)
                    || state.is(Blocks.RED_NETHER_BRICKS)
                    || state.is(Blocks.RED_NETHER_BRICK_STAIRS)
                    || state.is(Blocks.RED_NETHER_BRICK_SLAB)
                    || state.is(Blocks.RED_NETHER_BRICK_WALL)
                    || state.is(Blocks.NETHER_BRICK_WALL);
        }

        if (isOceanMonumentGuard(path)) {
            return state.is(Blocks.PRISMARINE)
                    || state.is(Blocks.PRISMARINE_BRICKS)
                    || state.is(Blocks.DARK_PRISMARINE)
                    || state.is(Blocks.SEA_LANTERN)
                    || state.is(Blocks.WET_SPONGE)
                    || state.is(Blocks.PRISMARINE_STAIRS)
                    || state.is(Blocks.PRISMARINE_SLAB)
                    || state.is(Blocks.PRISMARINE_WALL)
                    || state.is(Blocks.PRISMARINE_BRICK_STAIRS)
                    || state.is(Blocks.PRISMARINE_BRICK_SLAB)
                    || state.is(Blocks.DARK_PRISMARINE_STAIRS)
                    || state.is(Blocks.DARK_PRISMARINE_SLAB);
        }

        return false;
    }

    private static int staticAnchorHorizontalRadius(PathfinderMob guard) {
        return isOceanMonumentGuard(RetoldMobRules.getEntityTypePath(guard.getType()))
                ? MONUMENT_ANCHOR_HORIZONTAL_RADIUS
                : STATIC_ANCHOR_HORIZONTAL_RADIUS;
    }

    private static int staticAnchorVerticalRadius(PathfinderMob guard) {
        return isOceanMonumentGuard(RetoldMobRules.getEntityTypePath(guard.getType()))
                ? MONUMENT_ANCHOR_VERTICAL_RADIUS
                : STATIC_ANCHOR_VERTICAL_RADIUS;
    }

    private static boolean isOceanMonumentGuard(String path) {
        return path.equals("guardian")
                || path.equals("elder_guardian");
    }

    private static GuardPost loadPersistedPost(PathfinderMob guard) {
        if (guard == null) {
            return null;
        }

        CompoundTag persistentData = guard.getPersistentData();

        if (!persistentData.contains(PERSISTENT_KEY)) {
            return null;
        }

        CompoundTag tag = persistentData.getCompoundOrEmpty(PERSISTENT_KEY);

        if (tag.isEmpty()) {
            return null;
        }

        int x = tag.getInt("x").orElse(guard.blockPosition().getX());
        int y = tag.getInt("y").orElse(guard.blockPosition().getY());
        int z = tag.getInt("z").orElse(guard.blockPosition().getZ());
        boolean dynamic = tag.getBoolean("dynamic").orElse(isDynamicPostGuard(guard));
        long updatedAt = tag.getLong("updatedAt").orElse(0L);

        return new GuardPost(
                new BlockPos(x, y, z),
                dynamic,
                updatedAt
        );
    }

    private static void savePost(
            PathfinderMob guard,
            GuardPost post
    ) {
        if (guard == null || post == null) {
            return;
        }

        GUARD_POSTS.put(
                guard,
                post
        );

        CompoundTag tag = new CompoundTag();
        tag.putInt("version", SAVE_VERSION);
        tag.putInt("x", post.pos().getX());
        tag.putInt("y", post.pos().getY());
        tag.putInt("z", post.pos().getZ());
        tag.putBoolean("dynamic", post.dynamic());
        tag.putLong("updatedAt", post.updatedAt());

        guard.getPersistentData().put(
                PERSISTENT_KEY,
                tag
        );
    }

    private static String ticksAgoText(
            long gameTime,
            long timestamp
    ) {
        if (timestamp <= 0L || gameTime <= 0L) {
            return "never";
        }

        return Math.max(0L, gameTime - timestamp) + "t ago";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private record GuardPost(
            BlockPos pos,
            boolean dynamic,
            long updatedAt
    ) {
        private GuardPost withUpdatedAt(long gameTime) {
            return new GuardPost(
                    pos,
                    dynamic,
                    gameTime
            );
        }
    }
}
