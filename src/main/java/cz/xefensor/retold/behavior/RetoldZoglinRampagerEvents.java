package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;

public final class RetoldZoglinRampagerEvents {
    private static final int THINK_INTERVAL_TICKS = 10;
    private static final int RAMPAGE_CONTROL_TICKS = 20 * 4;
    private static final int RAMPAGE_PRIORITY = 74;

    private static final double TARGET_SEARCH_RADIUS_BLOCKS = 24.0D;
    private static final double TARGET_SEARCH_RADIUS_SQUARED =
            TARGET_SEARCH_RADIUS_BLOCKS * TARGET_SEARCH_RADIUS_BLOCKS;

    private static final double TARGET_KEEP_RADIUS_BLOCKS = 42.0D;
    private static final double TARGET_KEEP_RADIUS_SQUARED =
            TARGET_KEEP_RADIUS_BLOCKS * TARGET_KEEP_RADIUS_BLOCKS;

    private static final double RAMPAGE_SPEED = 1.16D;

    private RetoldZoglinRampagerEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob zoglin)) {
            return;
        }

        if (!(zoglin.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldMobRules.isZoglinRampager(zoglin)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!RetoldBehaviorTiming.shouldThink(
                zoglin,
                gameTime,
                THINK_INTERVAL_TICKS
        )) {
            return;
        }

        LivingEntity target = zoglin.getTarget();

        if (isValidRampageTarget(zoglin, target, TARGET_KEEP_RADIUS_SQUARED)) {
            continueRampage(
                    zoglin,
                    target,
                    gameTime
            );
            return;
        }

        clearRampageTargetIfOwned(
                zoglin,
                target
        );

        if (!canStartRampage(zoglin)) {
            return;
        }

        LivingEntity newTarget = findBestRampageTarget(
                level,
                zoglin
        );

        if (newTarget == null) {
            return;
        }

        continueRampage(
                zoglin,
                newTarget,
                gameTime
        );
    }

    private static LivingEntity findBestRampageTarget(
            ServerLevel level,
            PathfinderMob zoglin
    ) {
        AABB area = zoglin.getBoundingBox().inflate(TARGET_SEARCH_RADIUS_BLOCKS);
        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidRampageTarget(
                        zoglin,
                        candidate,
                        TARGET_SEARCH_RADIUS_SQUARED
                )
        );

        LivingEntity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = zoglin.distanceToSqr(candidate);

            if (distanceSquared > TARGET_SEARCH_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (zoglin.hasLineOfSight(candidate)) {
                score -= 28.0D;
            }

            if (candidate instanceof Player) {
                score -= 16.0D;
            }

            if (RetoldFactionMembers.isMemberOf(candidate, RetoldFaction.UNDEAD)) {
                score += 90.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private static void continueRampage(
            PathfinderMob zoglin,
            LivingEntity target,
            long gameTime
    ) {
        if (!isValidRampageTarget(zoglin, target, TARGET_KEEP_RADIUS_SQUARED)) {
            return;
        }

        if (!RetoldBehaviorCombat.claimAttackControl(
                zoglin,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RAMPAGE_PRIORITY,
                "zoglin_rampage",
                gameTime,
                RAMPAGE_CONTROL_TICKS
        )) {
            return;
        }

        RetoldBehaviorCombat.applyAttackTarget(
                zoglin,
                target,
                RetoldTargetSource.FACTION_COMBAT
        );

        RetoldAiControl.withNavigationBypass(() -> {
            zoglin.getNavigation().moveTo(
                    target,
                    RAMPAGE_SPEED
            );
        });
    }

    private static boolean canStartRampage(PathfinderMob zoglin) {
        return RetoldBehaviorCombat.canUseAttackControl(
                zoglin,
                RetoldAiControlOwner.SPECIAL_UNDEAD
        );
    }

    private static boolean isValidRampageTarget(
            PathfinderMob zoglin,
            LivingEntity target,
            double maxDistanceSquared
    ) {
        return RetoldBehaviorCombat.isValidEnemyTarget(
                zoglin,
                target,
                maxDistanceSquared,
                false
        );
    }

    private static void clearRampageTargetIfOwned(
            PathfinderMob zoglin,
            LivingEntity target
    ) {
        RetoldBehaviorCombat.clearAttackControlIfOwned(
                zoglin,
                target,
                RetoldAiControlOwner.SPECIAL_UNDEAD,
                RetoldTargetSource.FACTION_COMBAT
        );
    }
}
