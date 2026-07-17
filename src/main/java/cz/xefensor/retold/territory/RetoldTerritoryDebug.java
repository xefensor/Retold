package cz.xefensor.retold.territory;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;

public final class RetoldTerritoryDebug {
    private RetoldTerritoryDebug() {
    }

    public static boolean hasWarningDebug(PathfinderMob mob) {
        return RetoldTerritoryMobStates.get(mob) != null
                || RetoldTerritoryConfigs.getForEntity(mob) != null;
    }

    public static String shortWarningText(
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return "unknown";
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);
        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        if (config == null && state == null) {
            return "none";
        }

        RetoldTerritoryContext context = resolveContext(
                level,
                mob,
                config,
                state
        );
        LivingEntity warningTarget = state == null ? null : state.warningTarget;
        RetoldWarningLevel levelName = warningLevel(
                context,
                warningTarget
        );
        int suspicion = suspicion(
                context,
                warningTarget
        );

        return "ctx="
                + contextText(context)
                + " target="
                + entityText(warningTarget)
                + " level="
                + levelName
                + " susp="
                + suspicion
                + "/"
                + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + " flow="
                + (state == null ? RetoldTerritoryFlowState.INACTIVE : state.flowState)
                + " pulses="
                + (state == null ? 0 : state.warningPulses)
                + " next="
                + nextTicksText(state == null ? 0L : state.nextWarningPulseAt, gameTime);
    }

    public static String fullWarningText(
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return "Territory warning: unknown level";
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);
        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        if (config == null && state == null) {
            return "Territory warning: none";
        }

        RetoldTerritoryContext context = resolveContext(
                level,
                mob,
                config,
                state
        );
        LivingEntity warningTarget = state == null ? null : state.warningTarget;
        LivingEntity attackTarget = state == null ? null : state.attackTarget;
        LivingEntity brainAttackTarget = RetoldAiTargets.getBrainAttackTargetSafely(mob);
        LivingEntity angryAtTarget = RetoldAiTargets.resolveAngryAt(level, mob);
        LivingEntity bestVisibleIntruder = bestVisibleIntruder(level, mob, config, gameTime);
        RetoldWarningLevel levelName = warningLevel(
                context,
                warningTarget
        );
        int suspicion = suspicion(
                context,
                warningTarget
        );

        return "Territory warning"
                + "\nStatus: " + statusText(
                level,
                mob,
                config,
                state,
                context,
                warningTarget,
                bestVisibleIntruder,
                levelName,
                gameTime
        )
                + "\nConfig faction: " + (config == null ? "none" : config.faction)
                + "\nContext: " + contextText(context)
                + "\nAllowed dimension: " + yesNo(config != null && RetoldTerritoryDetector.isInAllowedDimension(level, config))
                + "\nCan use territory: " + yesNo(config != null && RetoldTerritoryRules.canUseTerritoryBehavior(level, mob, config))
                + "\nNear territory: " + yesNo(config != null && RetoldTerritoryDetector.isNearTerritory(level, mob, config, gameTime))
                + "\nBest visible intruder: " + entityText(bestVisibleIntruder)
                + "\nNearest player check: " + nearestPlayerCheck(level, mob, config, gameTime)
                + "\nWarning target: " + entityText(warningTarget)
                + "\nAttack target: " + entityText(attackTarget)
                + "\nMob target: " + entityText(mob.getTarget())
                + "\nTarget owner: " + RetoldFactionTargetMemory.debugOwnershipText(mob)
                + "\nBrain attack target: " + entityText(brainAttackTarget)
                + "\nBrain angry at: " + entityText(angryAtTarget)
                + "\nWarning level: " + levelName
                + "\nSuspicion: " + suspicion + "/" + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + "\nFlow state: " + (state == null ? RetoldTerritoryFlowState.INACTIVE : state.flowState)
                + "\nFlow state age: "
                + ageTicksText(state == null ? -1L : state.flowStateEnteredAt, gameTime)
                + "\nCooldown remaining: "
                + nextTicksText(state == null ? 0L : state.cooldownUntil, gameTime)
                + "\nWarning pulses: " + (state == null ? 0 : state.warningPulses)
                + "\nWarned intruders: " + (state == null ? 0 : state.warnedIntruders.size())
                + "\nNext warning pulse: "
                + nextTicksText(state == null ? 0L : state.nextWarningPulseAt, gameTime)
                + "\nNext target recheck: "
                + nextTicksText(state == null ? 0L : state.nextTargetRecheckAt, gameTime)
                + "\nNext context recheck: "
                + nextTicksText(state == null ? 0L : state.nextTerritoryContextRecheckAt, gameTime)
                + "\nFinal warning age: "
                + ageTicksText(state == null ? -1L : state.finalWarningStartedAt, gameTime);
    }

    private static RetoldTerritoryContext resolveContext(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state
    ) {
        RetoldTerritoryContext context = state == null ? null : state.territoryContext;

        if (context != null) {
            return context;
        }

        if (config == null) {
            return null;
        }

        return RetoldTerritoryDetector.getContextAt(
                level,
                mob.blockPosition()
        );
    }

    private static LivingEntity bestVisibleIntruder(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        if (config == null) {
            return null;
        }

        return RetoldTerritoryTargetSelector.findBestWarningTarget(
                level,
                mob,
                config,
                RetoldTerritoryMobStates.states().get(mob),
                RetoldTerritoryMobStates.states(),
                gameTime
        );
    }

    private static String nearestPlayerCheck(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            long gameTime
    ) {
        List<ServerPlayer> players = level.getEntitiesOfClass(
                ServerPlayer.class,
                mob.getBoundingBox().inflate(RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS),
                player -> player != null && player.isAlive() && !player.isRemoved()
        );

        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            double distance = mob.distanceToSqr(player);

            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            return "none within " + (int) RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS + " blocks";
        }

        return entityText(nearest) + " "
                + playerWarningRejectionReason(
                level,
                mob,
                config,
                nearest,
                gameTime
        );
    }

    private static String playerWarningRejectionReason(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            ServerPlayer player,
            long gameTime
    ) {
        if (!RetoldAiTargets.isAliveInSameLevel(mob, player)) {
            return "rejected: not alive or different level";
        }

        if (mob.distanceToSqr(player) > RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS * RetoldTerritoryConstants.NOTICE_MOB_RADIUS_BLOCKS) {
            return "rejected: outside notice range";
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return "rejected: creative or spectator";
        }

        if (config == null) {
            return "rejected: no territory config";
        }

        if (!RetoldTerritoryDetector.isInAllowedDimension(level, config)) {
            return "rejected: wrong dimension";
        }

        if (!RetoldTerritoryRules.canUseTerritoryBehavior(level, mob, config)) {
            return "rejected: territory rules blocked";
        }

        if (!RetoldTerritoryDetector.isNearTerritory(level, mob, config, gameTime)) {
            return "rejected: mob not near territory";
        }

        if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, mob, player, config, gameTime)) {
            return "rejected: not a warning intruder for faction";
        }

        if (!RetoldAiTargets.isVisibleTo(mob, player)) {
            return "rejected: no line of sight";
        }

        return "accepted";
    }

    private static String statusText(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state,
            RetoldTerritoryContext context,
            LivingEntity warningTarget,
            LivingEntity bestVisibleIntruder,
            RetoldWarningLevel warningLevel,
            long gameTime
    ) {
        if (config == null) {
            return "no territory config for mob faction";
        }

        if (!RetoldTerritoryDetector.isInAllowedDimension(level, config)) {
            return "wrong dimension for " + config.faction;
        }

        if (!RetoldTerritoryRules.canUseTerritoryBehavior(level, mob, config)) {
            return "territory behavior blocked by rules";
        }

        if (!RetoldTerritoryDetector.isNearTerritory(level, mob, config, gameTime)) {
            return "not near tagged territory";
        }

        if (context == null || context.faction() != config.faction) {
            return "territory context missing";
        }

        if (state != null && state.isAttacking()) {
            return "territory attack active";
        }

        if (warningTarget == null) {
            if (bestVisibleIntruder == null) {
                return "no visible valid intruder";
            }

            return "valid intruder visible, waiting for warning target update";
        }

        if (warningLevel == RetoldWarningLevel.ATTACK) {
            return "attack threshold reached, waiting to start attack";
        }

        if (state != null && gameTime < state.nextWarningPulseAt) {
            return "warning active, next pulse in " + Math.max(0L, state.nextWarningPulseAt - gameTime) + "t";
        }

        return "warning active";
    }

    private static RetoldWarningLevel warningLevel(
            RetoldTerritoryContext context,
            LivingEntity warningTarget
    ) {
        if (context == null || warningTarget == null) {
            return RetoldWarningLevel.NONE;
        }

        return RetoldTerritoryReputation.getWarningLevel(
                context,
                warningTarget
        );
    }

    private static int suspicion(
            RetoldTerritoryContext context,
            LivingEntity warningTarget
    ) {
        if (context == null || warningTarget == null) {
            return 0;
        }

        return RetoldTerritoryReputation.getSuspicion(
                context,
                warningTarget
        );
    }

    private static String contextText(RetoldTerritoryContext context) {
        if (context == null) {
            return "none";
        }

        return context.debugName();
    }

    private static String entityText(Entity entity) {
        if (entity == null) {
            return "none";
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getPath() + " #" + entity.getId();
    }

    private static String nextTicksText(
            long timestamp,
            long gameTime
    ) {
        if (timestamp <= 0L) {
            return "none";
        }

        return Math.max(0L, timestamp - gameTime) + "t";
    }

    private static String ageTicksText(
            long timestamp,
            long gameTime
    ) {
        if (timestamp < 0L) {
            return "none";
        }

        return Math.max(0L, gameTime - timestamp) + "t";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
