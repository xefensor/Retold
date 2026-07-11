package cz.xefensor.retold.territory;

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

        List<PathfinderMob> possibleWitnesses = level.getEntitiesOfClass(
                PathfinderMob.class,
                player.getBoundingBox().inflate(witnessRadiusBlocks),
                mob -> mob.isAlive()
                        && RetoldFactionMembers.isMemberOf(mob, faction)
                        && mob.distanceToSqr(
                        actionPos.getX() + 0.5D,
                        actionPos.getY() + 0.5D,
                        actionPos.getZ() + 0.5D
                ) <= witnessRadiusBlocks * witnessRadiusBlocks
        );

        for (PathfinderMob witness : possibleWitnesses) {
            if (RetoldAiTargets.isVisibleTo(witness, player)) {
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

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.get(faction);

        if (config == null) {
            return;
        }

        if (!RetoldTerritoryDetector.isInAllowedDimension(level, config)) {
            return;
        }

        List<PathfinderMob> witnesses = level.getEntitiesOfClass(
                PathfinderMob.class,
                player.getBoundingBox().inflate(witnessRadiusBlocks),
                mob -> mob.isAlive()
                        && RetoldFactionMembers.isMemberOf(mob, faction)
                        && mob.distanceToSqr(
                        actionPos.getX() + 0.5D,
                        actionPos.getY() + 0.5D,
                        actionPos.getZ() + 0.5D
                ) <= witnessRadiusBlocks * witnessRadiusBlocks
                        && RetoldAiTargets.isVisibleTo(mob, player)
        );

        long gameTime = level.getGameTime();

        for (PathfinderMob witness : witnesses) {
            if (!RetoldTerritoryDetector.isNearTerritory(level, witness, config, gameTime)) {
                continue;
            }

            RetoldTerritoryMobState state = mobStates.computeIfAbsent(
                    witness,
                    ignored -> new RetoldTerritoryMobState()
            );

            state.territoryContext = RetoldTerritoryDetector.getContextAt(
                    level,
                    witness.blockPosition()
            );

            if (state.territoryContext == null || state.territoryContext.faction() != faction) {
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
