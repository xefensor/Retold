package cz.xefensor.retold.villager;

import cz.xefensor.retold.combat.RetoldAiTargets;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Applies witnessed village offenses through vanilla Villager gossip. */
final class RetoldVillageWitnessReputation {
    private static final double WITNESS_RADIUS = 16.0D;
    private static final double VILLAGE_ANCHOR_RADIUS_SQUARED =
            32.0D * 32.0D;
    private static final int UNHAPPY_TICKS = 40;

    private RetoldVillageWitnessReputation() {
    }

    static int report(
            ServerLevel level,
            ServerPlayer player,
            BlockPos actionPos,
            Offense offense
    ) {
        if (level == null
                || player == null
                || actionPos == null
                || offense == null
                || RetoldAiTargets.isInvalidPlayerTarget(player)) {
            return 0;
        }

        AABB bounds = new AABB(actionPos).inflate(WITNESS_RADIUS);
        List<Villager> witnesses = level.getEntitiesOfClass(
                Villager.class,
                bounds,
                villager -> isWitness(level, villager, player, actionPos)
        );

        for (Villager witness : witnesses) {
            witness.getGossips().add(
                    player.getUUID(),
                    offense.gossipType(),
                    offense.value()
            );
            witness.setUnhappyCounter(UNHAPPY_TICKS);
            level.broadcastEntityEvent(witness, (byte) 13);
            witness.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
        }

        return witnesses.size();
    }

    private static boolean isWitness(
            ServerLevel level,
            Villager villager,
            ServerPlayer player,
            BlockPos actionPos
    ) {
        if (!villager.isAlive()
                || villager.distanceToSqr(
                actionPos.getX() + 0.5D,
                actionPos.getY() + 0.5D,
                actionPos.getZ() + 0.5D
        ) > WITNESS_RADIUS * WITNESS_RADIUS
                || !RetoldAiTargets.isVisibleTo(villager, player)) {
            return false;
        }

        BlockPos villageAnchor = RetoldVillagerCommunalFoodSearch
                .villageAnchor(level, villager);
        return villageAnchor != null
                && villageAnchor.distSqr(actionPos)
                <= VILLAGE_ANCHOR_RADIUS_SQUARED;
    }

    enum Offense {
        STORAGE_THEFT(GossipType.MINOR_NEGATIVE, 25),
        STORAGE_BREAK(GossipType.MAJOR_NEGATIVE, 20),
        MATURE_CROP_THEFT(GossipType.MINOR_NEGATIVE, 25),
        CROP_VANDALISM(GossipType.MAJOR_NEGATIVE, 10),
        ANIMAL_KILLING(GossipType.MAJOR_NEGATIVE, 10);

        private final GossipType gossipType;
        private final int value;

        Offense(GossipType gossipType, int value) {
            this.gossipType = gossipType;
            this.value = value;
        }

        private GossipType gossipType() {
            return gossipType;
        }

        private int value() {
            return value;
        }
    }
}
