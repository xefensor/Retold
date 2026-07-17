package cz.xefensor.retold.territory;

import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PathfinderMob;

import java.util.List;
import java.util.Map;

public final class RetoldTerritoryWitnesses {
    private RetoldTerritoryWitnesses() {
    }

    public static boolean hasWitnessForIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos,
            double witnessRadiusBlocks
    ) {
        if (faction == null || player == null) {
            return false;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return false;
        }

        long gameTime = level.getGameTime();
        double witnessRadiusSquared = witnessRadiusBlocks * witnessRadiusBlocks;

        List<PathfinderMob> possibleWitnesses = RetoldAiScanCache.nearby(
                level,
                player,
                PathfinderMob.class,
                witnessRadiusBlocks,
                gameTime,
                RetoldTerritoryConstants.WARNING_ILLEGAL_WITNESS_SCAN_CACHE_TICKS
        );

        for (PathfinderMob witness : possibleWitnesses) {
            if (
                    witness.isAlive()
                            && RetoldFactionMembers.isMemberOf(witness, faction)
                            && witness.distanceToSqr(
                            actionPos.getX() + 0.5D,
                            actionPos.getY() + 0.5D,
                            actionPos.getZ() + 0.5D
                    ) <= witnessRadiusSquared
                            && RetoldAiTargets.isVisibleTo(witness, player)
            ) {
                return true;
            }
        }

        return false;
    }

    public static void alertWitnessesAfterIllegalAction(
            ServerLevel level,
            RetoldFaction faction,
            ServerPlayer player,
            BlockPos actionPos,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            double witnessRadiusBlocks
    ) {
        if (faction == null || player == null) {
            return;
        }

        if (RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return;
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.get(faction);

        if (config == null) {
            return;
        }

        if (!RetoldTerritoryDetector.isInAllowedDimension(level, config)) {
            return;
        }

        long gameTime = level.getGameTime();
        double witnessRadiusSquared = witnessRadiusBlocks * witnessRadiusBlocks;

        List<PathfinderMob> witnesses = RetoldAiScanCache.nearby(
                level,
                player,
                PathfinderMob.class,
                witnessRadiusBlocks,
                gameTime,
                RetoldTerritoryConstants.WARNING_ILLEGAL_WITNESS_SCAN_CACHE_TICKS
        );

        for (PathfinderMob witness : witnesses) {
            if (
                    !witness.isAlive()
                            || !RetoldFactionMembers.isMemberOf(witness, faction)
                            || witness.distanceToSqr(
                            actionPos.getX() + 0.5D,
                            actionPos.getY() + 0.5D,
                            actionPos.getZ() + 0.5D
                    ) > witnessRadiusSquared
                            || !RetoldAiTargets.isVisibleTo(witness, player)
            ) {
                continue;
            }

            if (!RetoldTerritoryRules.canUseNearbyTerritoryBehavior(
                    level,
                    witness,
                    config,
                    gameTime
            )) {
                continue;
            }

            RetoldTerritoryMobState state = mobStates.computeIfAbsent(
                    witness,
                    ignored -> new RetoldTerritoryMobState()
            );

            state.territoryContext = RetoldTerritoryRules.refreshCachedMatchingContext(
                    state,
                    level,
                    witness,
                    config,
                    gameTime
            );

            if (state.territoryContext == null) {
                continue;
            }

            if (!RetoldTerritoryTargetSelector.isPossibleIntruder(level, witness, player, config, gameTime)) {
                continue;
            }

            if (state.hasStartedAttack) {
                continue;
            }

            RetoldTerritoryController.setWarningTarget(
                    state,
                    witness,
                    player,
                    gameTime
            );

            if (RetoldTerritoryReputation.shouldAttack(state.territoryContext, player)) {
                RetoldTerritoryCombat.startAttackOnTarget(
                        level,
                        witness,
                        state,
                        config,
                        player,
                        gameTime,
                        RetoldTargetSource.TERRITORY_ATTACK
                );
            }
        }
    }
}
