package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Set;

public final class RetoldTerritoryIllegalActionEvents {
    private static final Set<Identifier> ILLEGAL_CONTAINER_BLOCKS = Set.of(
            id("chest"),
            id("trapped_chest"),
            id("barrel"),
            id("shulker_box"),
            id("white_shulker_box"),
            id("orange_shulker_box"),
            id("magenta_shulker_box"),
            id("light_blue_shulker_box"),
            id("yellow_shulker_box"),
            id("lime_shulker_box"),
            id("pink_shulker_box"),
            id("gray_shulker_box"),
            id("light_gray_shulker_box"),
            id("cyan_shulker_box"),
            id("purple_shulker_box"),
            id("blue_shulker_box"),
            id("brown_shulker_box"),
            id("green_shulker_box"),
            id("red_shulker_box"),
            id("black_shulker_box")
    );

    private RetoldTerritoryIllegalActionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!isIllegalContainer(state.getBlock())) {
            return;
        }

        addVisibleIllegalSuspicion(
                level,
                player,
                pos,
                IllegalActionType.STEALING
        );
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();

        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();

        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        RetoldFaction victimFaction = RetoldFactionMembers.getFaction(victim);

        if (victimFaction == null || victimFaction == RetoldFaction.PLAYER) {
            return;
        }

        RetoldTerritoryContext territoryContext = RetoldTerritoryDetector.getContextAt(
                level,
                victim.blockPosition()
        );

        if (territoryContext == null || territoryContext.faction() != victimFaction) {
            return;
        }

        RetoldTerritoryReputation.addAttackSuspicion(
                territoryContext,
                player,
                level.getGameTime()
        );

        RetoldTerritoryWitnesses.alertWitnessesAfterIllegalAction(
                level,
                territoryContext.faction(),
                player,
                victim.blockPosition(),
                RetoldTerritoryMobStates.states(),
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        );
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        addVisibleIllegalSuspicion(
                level,
                player,
                event.getPos(),
                IllegalActionType.BLOCK_BREAK
        );
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity attacker = event.getSource().getEntity();

        if (!(attacker instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity victim = event.getEntity();

        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }

        RetoldFaction victimFaction = RetoldFactionMembers.getFaction(victim);

        if (victimFaction == null || victimFaction == RetoldFaction.PLAYER) {
            return;
        }

        RetoldTerritoryContext territoryContext = RetoldTerritoryDetector.getContextAt(
                level,
                victim.blockPosition()
        );

        if (territoryContext == null || territoryContext.faction() != victimFaction) {
            return;
        }

        RetoldTerritoryReputation.addKillSuspicion(
                territoryContext,
                player,
                level.getGameTime()
        );

        RetoldTerritoryWitnesses.alertWitnessesAfterIllegalAction(
                level,
                territoryContext.faction(),
                player,
                victim.blockPosition(),
                RetoldTerritoryMobStates.states(),
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        );
    }

    private static void addVisibleIllegalSuspicion(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            IllegalActionType actionType
    ) {
        RetoldTerritoryContext territoryContext = RetoldTerritoryDetector.getContextAt(
                level,
                pos
        );

        if (territoryContext == null) {
            return;
        }

        RetoldFaction territoryFaction = territoryContext.faction();

        if (!RetoldTerritoryWitnesses.hasWitnessForIllegalAction(
                level,
                territoryFaction,
                player,
                pos,
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        )) {
            return;
        }

        long gameTime = level.getGameTime();

        if (actionType == IllegalActionType.STEALING) {
            RetoldTerritoryReputation.addStealingSuspicion(
                    territoryContext,
                    player,
                    gameTime
            );
        } else if (actionType == IllegalActionType.BLOCK_BREAK) {
            RetoldTerritoryReputation.addBlockBreakSuspicion(
                    territoryContext,
                    player,
                    gameTime
            );
        }

        RetoldTerritoryWitnesses.alertWitnessesAfterIllegalAction(
                level,
                territoryFaction,
                player,
                pos,
                RetoldTerritoryMobStates.states(),
                RetoldTerritoryConstants.ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS
        );
    }

    private static boolean isIllegalContainer(Block block) {
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
        return ILLEGAL_CONTAINER_BLOCKS.contains(blockId);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private enum IllegalActionType {
        STEALING,
        BLOCK_BREAK
    }
}
