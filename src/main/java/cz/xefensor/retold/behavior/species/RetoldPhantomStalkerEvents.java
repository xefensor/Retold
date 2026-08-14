package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCombat;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldPhantomStalkerEvents {
    private static final int THINK_INTERVAL_TICKS = 12;
    private static final int STALK_SCAN_CACHE_TICKS = 6;
    private static final int STALK_CONTROL_TICKS = 20 * 5;
    private static final int STALK_PRIORITY = RetoldAiPriorities.SPECIAL_STALK;
    // Applied after vanilla's bounded Phantom-spawner cadence and outer gamerule checks.
    private static final int SPAWN_RARITY_ATTEMPTS = 8;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 42.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;

    private static final double TARGET_KEEP_RADIUS_BLOCKS = 56.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;

    private RetoldPhantomStalkerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Mob phantom)) {
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

        event.setResult(resolveSpawnResult(
                level.dimensionType().hasSkyLight(),
                level.canSeeSky(playerPos),
                isStalkingTime(level),
                rarityRoll,
                level.getCurrentDifficultyAt(playerPos).isHarderThan(difficultyRoll)
        ));
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

    private static void keepTarget(
            Mob phantom,
            LivingEntity target,
            long gameTime
    ) {
        if (!(phantom.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isValidStalkTarget(level, phantom, target, TARGET_KEEP_RADIUS_SQUARED)) {
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
    }

    private static boolean canAcquireTarget(
            ServerLevel level,
            Mob phantom
    ) {
        if (!isStalkingTime(level)) {
            return false;
        }

        return RetoldBehaviorCombat.canUseAttackControl(
                phantom,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        );
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
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                phantom,
                target,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RetoldTargetSource.FACTION_COMBAT
        );
    }
}
