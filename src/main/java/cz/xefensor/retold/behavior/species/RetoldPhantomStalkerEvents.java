package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldPhantomStalkerEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int STALK_SCAN_CACHE_TICKS = 6;
    private static final int STALK_CONTROL_TICKS = 20 * 5;
    private static final int STALK_PRIORITY = RetoldAiPriorities.SPECIAL_STALK;
    private static final int CIRCLE_BEFORE_SWOOP_TICKS = 20;
    private static final int SWOOP_TIMEOUT_TICKS = 60;
    private static final int FAILED_SWOOP_RETRY_TICKS = 30;
    private static final int STRIKE_RECOVERY_TICKS = 20 * 8;
    private static final String STRIKE_RECOVERY_REASON = "phantom_recovery";
    private static final double SWOOP_SPEED = 0.9D;
    private static final double SWOOP_STEERING = 0.65D;
    private static final double STRIKE_BOX_INFLATION = 0.65D;
    private static final double COLLISION_LOOKAHEAD_TICKS = 2.0D;
    // Applied after vanilla's bounded Phantom-spawner cadence and outer gamerule checks.
    static final int SPAWN_RARITY_ATTEMPTS = 16;
    static final int SPAWN_GROUP_SIZE = 1;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 42.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;

    private static final double TARGET_KEEP_RADIUS_BLOCKS = 56.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;

    private static final Map<Phantom, SwoopState> SWOOP_STATES =
            new WeakHashMap<>();

    private static final Map<Phantom, Integer> NEXT_STALK_AT =
            new WeakHashMap<>();

    private RetoldPhantomStalkerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Phantom phantom)) {
            return;
        }

        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isPhantomStalker(phantom)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                phantom,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        LivingEntity target = phantom.getTarget();

        if (isRecovering(phantom)) {
            clearTargetDuringRecovery(phantom, target);
            return;
        }

        if (isValidStalkTarget(level, phantom, target, TARGET_KEEP_RADIUS_SQUARED)) {
            keepTarget(
                    phantom,
                    target,
                    gameTime
            );
            return;
        }

        clearStalkTargetIfOwned(
                phantom,
                target
        );

        if (!canAcquireTarget(level, phantom)) {
            return;
        }

        LivingEntity newTarget = findBestStalkTarget(
                level,
                phantom
        );

        if (newTarget == null) {
            return;
        }

        keepTarget(
                phantom,
                newTarget,
                gameTime
        );
    }

    public static void onPlayerSpawnPhantoms(PlayerSpawnPhantomsEvent event) {
        if (event.getResult() != PlayerSpawnPhantomsEvent.Result.DEFAULT
                || !(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        BlockPos playerPos = player.blockPosition();

        if (!isEligibleSpawnContext(level, playerPos)) {
            event.setResult(PlayerSpawnPhantomsEvent.Result.DENY);
            return;
        }

        applySpawnPolicy(
                event,
                level,
                playerPos,
                level.getRandom().nextInt(SPAWN_RARITY_ATTEMPTS),
                level.getRandom().nextFloat() * 3.0F
        );
    }

    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !(event.getSource().getEntity() instanceof Phantom phantom)) {
            return;
        }

        onSuccessfulAttack(
                phantom,
                event.getEntity()
        );
    }

    static void applySpawnPolicy(
            PlayerSpawnPhantomsEvent event,
            ServerLevel level,
            BlockPos playerPos,
            int rarityRoll,
            float difficultyRoll
    ) {
        if (event.getResult() != PlayerSpawnPhantomsEvent.Result.DEFAULT) {
            return;
        }

        PlayerSpawnPhantomsEvent.Result result = resolveSpawnResult(
                level.dimensionType().hasSkyLight(),
                level.canSeeSky(playerPos),
                isStalkingTime(level),
                rarityRoll,
                level.getCurrentDifficultyAt(playerPos).isHarderThan(difficultyRoll)
        );

        if (result == PlayerSpawnPhantomsEvent.Result.ALLOW) {
            event.setPhantomsToSpawn(SPAWN_GROUP_SIZE);
        }

        event.setResult(result);
    }

    static LivingEntity findBestStalkTarget(
            ServerLevel level,
            Mob phantom
    ) {
        List<LivingEntity> candidates = RetoldAiScanCache.nearby(
                level,
                phantom,
                LivingEntity.class,
                TARGET_SEARCH_RADIUS_BLOCKS,
                level.getGameTime(),
                STALK_SCAN_CACHE_TICKS
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            if (!isValidStalkTarget(level, phantom, candidate, TARGET_SEARCH_RADIUS_SQUARED)) {
                continue;
            }

            double distanceSquared = phantom.distanceToSqr(candidate);

            if (distanceSquared > TARGET_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (isExposedToSky(level, candidate)) {
                score -= 180.0D;
            }

            if (RetoldAiSightCache.canSee(phantom, candidate, level.getGameTime())) {
                score -= 80.0D;
            }

            if (RetoldFactionMembers.isUndead(candidate)) {
                score += 300.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    static void keepTarget(
            Phantom phantom,
            LivingEntity target,
            long gameTime
    ) {
        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isValidStalkTarget(level, phantom, target, TARGET_KEEP_RADIUS_SQUARED)) {
            return;
        }

        if (isRecovering(phantom)) {
            clearTargetDuringRecovery(phantom, target);
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                STALK_PRIORITY,
                "phantom_stalker",
                gameTime,
                STALK_CONTROL_TICKS
        )) {
            return;
        }

        if (!RetoldBehaviorCombat.applyAttackTargetOrClearOwner(
                phantom,
                target,
                RetoldTargetSource.FACTION_COMBAT,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        )) {
            return;
        }

        SwoopState state = SWOOP_STATES.get(phantom);

        if (state == null || state.target() != target) {
            SWOOP_STATES.put(
                    phantom,
                    newSwoopState(
                            phantom,
                            target,
                            CIRCLE_BEFORE_SWOOP_TICKS
                    )
            );
        }
    }

    /**
     * Supplies the final flight-and-contact handoff for Retold-owned Phantom prey. Vanilla's
     * Phantom goals were written around their own player target selector and can circle a target
     * supplied by another owner without ever completing a bounding-box hit. This bounded step
     * keeps vanilla circling, sounds, and animation, then reinforces only the active dive and
     * records a normal Mob attack when the Phantom reaches its prey.
     */
    public static void applyOwnedAttackFlight(
            ServerLevel level,
            Phantom phantom
    ) {
        if (level == null || phantom == null) {
            return;
        }

        SwoopState state = SWOOP_STATES.get(phantom);

        if (state == null) {
            return;
        }

        LivingEntity target = phantom.getTarget();
        int attackTick = phantom.tickCount;

        if (target != state.target()
                || !RetoldBehaviorCombat.isValidEnemyTarget(
                phantom,
                target,
                TARGET_KEEP_RADIUS_SQUARED,
                false
        )) {
            SWOOP_STATES.remove(phantom);
            return;
        }

        if (target.getLastHurtByMob() == phantom
                && target.getLastHurtByMobTimestamp() > state.lastObservedHurtTimestamp()) {
            finishStrike(phantom, target);
            return;
        }

        if (attackTick < state.swoopAt()) {
            return;
        }

        if (attackTick > state.expiresAt()) {
            SWOOP_STATES.put(
                    phantom,
                    newSwoopState(
                            phantom,
                            target,
                            FAILED_SWOOP_RETRY_TICKS
                    )
            );
            return;
        }

        if (phantom.getBoundingBox()
                .inflate(STRIKE_BOX_INFLATION)
                .intersects(target.getBoundingBox())) {
            if (phantom.doHurtTarget(level, target)
                    && SWOOP_STATES.get(phantom) == state) {
                finishStrike(phantom, target);
            }
            return;
        }

        if (!phantom.getSensing().hasLineOfSight(target)) {
            return;
        }

        Vec3 towardTarget = new Vec3(
                target.getX(),
                target.getY(0.5D),
                target.getZ()
        ).subtract(phantom.position());

        if (towardTarget.lengthSqr() <= 0.0001D) {
            return;
        }

        Vec3 desiredMovement = towardTarget.normalize().scale(SWOOP_SPEED);
        Vec3 nextMovement = phantom.getDeltaMovement().lerp(
                desiredMovement,
                SWOOP_STEERING
        );

        if (!level.noCollision(
                phantom,
                phantom.getBoundingBox().move(
                        nextMovement.scale(COLLISION_LOOKAHEAD_TICKS)
                )
        )) {
            SWOOP_STATES.put(
                    phantom,
                    newSwoopState(
                            phantom,
                            target,
                            FAILED_SWOOP_RETRY_TICKS
                    )
            );
            return;
        }

        phantom.setDeltaMovement(nextMovement);
    }

    /**
     * Completes recovery on the exact successful hit, including hits performed by vanilla's
     * sweep goal between Retold flight callbacks.
     */
    public static void onSuccessfulAttack(
            Phantom phantom,
            LivingEntity target
    ) {
        if (phantom == null || target == null) {
            return;
        }

        SwoopState state = SWOOP_STATES.get(phantom);

        boolean ownsTarget = phantom.getTarget() == target
                && RetoldFactionTargetMemory.isOwnedByAny(
                        phantom,
                        target,
                        RetoldTargetSource.FACTION_COMBAT
                );

        if ((state != null && state.target() == target) || ownsTarget) {
            finishStrike(phantom, target);
        }
    }

    private static boolean canAcquireTarget(
            ServerLevel level,
            Mob phantom
    ) {
        if (!isStalkingTime(level)) {
            return false;
        }

        if (shouldBlockTargetDuringRecovery(phantom)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        );
    }

    private static boolean isRecovering(Phantom phantom) {
        return shouldBlockTargetDuringRecovery(phantom)
                || RetoldAiControl.isControlledAsByWithReason(
                phantom,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                STRIKE_RECOVERY_REASON
        );
    }

    public static boolean shouldBlockTargetDuringRecovery(Mob mob) {
        if (!(mob instanceof Phantom phantom)) {
            return false;
        }

        Integer nextStalkAt = NEXT_STALK_AT.get(phantom);

        if (nextStalkAt == null) {
            return false;
        }

        if (phantom.tickCount < nextStalkAt) {
            return true;
        }

        NEXT_STALK_AT.remove(phantom);
        return false;
    }

    private static void clearTargetDuringRecovery(
            Phantom phantom,
            LivingEntity target
    ) {
        SWOOP_STATES.remove(phantom);

        if (target == null) {
            return;
        }

        RetoldFactionTargetMemory.clearTargetIfOwnedBy(
                phantom,
                target,
                RetoldTargetSource.FACTION_COMBAT
        );

        if (phantom.getTarget() == target) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    phantom,
                    null
            );
        }
    }

    private static boolean isValidStalkTarget(
            ServerLevel level,
            Mob phantom,
            LivingEntity target,
            double maxDistanceSquared
    ) {
        if (!isStalkingTime(level)) {
            return false;
        }

        if (!RetoldBehaviorCombat.isValidEnemyTarget(
                phantom,
                target,
                maxDistanceSquared,
                false
        )) {
            return false;
        }

        if (!isExposedToSky(level, target) && !RetoldAiSightCache.canSee(phantom, target, phantom.level().getGameTime())) {
            return false;
        }

        return true;
    }

    private static boolean isStalkingTime(ServerLevel level) {
        return RetoldAnimalDailyRhythm.isNight(level)
                || level.isRaining();
    }

    private static boolean isEligibleSpawnContext(
            ServerLevel level,
            BlockPos playerPos
    ) {
        return level.dimensionType().hasSkyLight()
                && level.canSeeSky(playerPos)
                && isStalkingTime(level);
    }

    static PlayerSpawnPhantomsEvent.Result resolveSpawnResult(
            boolean hasSkyLight,
            boolean openSky,
            boolean nightOrStorm,
            int rarityRoll,
            boolean difficultyPassed
    ) {
        return hasSkyLight
                && openSky
                && nightOrStorm
                && rarityRoll == 0
                && difficultyPassed
                ? PlayerSpawnPhantomsEvent.Result.ALLOW
                : PlayerSpawnPhantomsEvent.Result.DENY;
    }

    private static boolean isExposedToSky(
            ServerLevel level,
            LivingEntity target
    ) {
        return level.canSeeSky(
                target.blockPosition()
        );
    }

    private static void clearStalkTargetIfOwned(
            Mob phantom,
            LivingEntity target
    ) {
        if (phantom instanceof Phantom typedPhantom) {
            SWOOP_STATES.remove(typedPhantom);
        }

        RetoldBehaviorCombat.clearAttackControlIfOwned(
                phantom,
                target,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RetoldTargetSource.FACTION_COMBAT
        );
    }

    private static void finishStrike(
            Phantom phantom,
            LivingEntity target
    ) {
        Vec3 retreat = phantom.position().subtract(target.position());

        if (retreat.lengthSqr() <= 0.0001D) {
            retreat = new Vec3(0.0D, 1.0D, 0.0D);
        } else {
            retreat = retreat.normalize();
        }

        phantom.setDeltaMovement(
                retreat.x() * 0.45D,
                Math.max(0.45D, retreat.y() * 0.45D),
                retreat.z() * 0.45D
        );
        NEXT_STALK_AT.put(phantom, phantom.tickCount + STRIKE_RECOVERY_TICKS);
        clearStalkTargetIfOwned(phantom, target);
        RetoldBehaviorCombat.claimAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                STALK_PRIORITY,
                STRIKE_RECOVERY_REASON,
                phantom.level().getGameTime(),
                STRIKE_RECOVERY_TICKS
        );
    }

    private static SwoopState newSwoopState(
            Phantom phantom,
            LivingEntity target,
            int delayTicks
    ) {
        int swoopAt = phantom.tickCount + Math.max(0, delayTicks);

        return new SwoopState(
                target,
                swoopAt,
                swoopAt + SWOOP_TIMEOUT_TICKS,
                target.getLastHurtByMobTimestamp()
        );
    }

    private record SwoopState(
            LivingEntity target,
            int swoopAt,
            int expiresAt,
            int lastObservedHurtTimestamp
    ) {
    }
}
