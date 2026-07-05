package cz.xefensor.retold.event;

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

import java.util.Set;

public final class RetoldIllegalActionEvents {
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

    private RetoldIllegalActionEvents() {
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
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
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

        RetoldTerritoryContext territoryContext = RetoldFactionTerritoryEvents.getTerritoryContextAt(
                level,
                victim.blockPosition()
        );

        if (territoryContext == null || territoryContext.faction() != victimFaction) {
            return;
        }

        RetoldIntruderReputation.addAttackSuspicion(
                territoryContext,
                player,
                level.getGameTime()
        );

        RetoldFactionTerritoryEvents.alertWitnessesAfterIllegalAction(
                level,
                territoryContext.faction(),
                player,
                victim.blockPosition()
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

        RetoldTerritoryContext territoryContext = RetoldFactionTerritoryEvents.getTerritoryContextAt(
                level,
                victim.blockPosition()
        );

        if (territoryContext == null || territoryContext.faction() != victimFaction) {
            return;
        }

        RetoldIntruderReputation.addKillSuspicion(
                territoryContext,
                player,
                level.getGameTime()
        );

        RetoldFactionTerritoryEvents.alertWitnessesAfterIllegalAction(
                level,
                territoryContext.faction(),
                player,
                victim.blockPosition()
        );
    }

    private static void addVisibleIllegalSuspicion(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            IllegalActionType actionType
    ) {
        RetoldTerritoryContext territoryContext = RetoldFactionTerritoryEvents.getTerritoryContextAt(
                level,
                pos
        );

        if (territoryContext == null) {
            return;
        }

        RetoldFaction territoryFaction = territoryContext.faction();

        if (!RetoldFactionTerritoryEvents.hasWitnessForIllegalAction(
                level,
                territoryFaction,
                player,
                pos
        )) {
            return;
        }

        long gameTime = level.getGameTime();

        if (actionType == IllegalActionType.STEALING) {
            RetoldIntruderReputation.addStealingSuspicion(
                    territoryContext,
                    player,
                    gameTime
            );
        } else if (actionType == IllegalActionType.BLOCK_BREAK) {
            RetoldIntruderReputation.addBlockBreakSuspicion(
                    territoryContext,
                    player,
                    gameTime
            );
        }

        RetoldFactionTerritoryEvents.alertWitnessesAfterIllegalAction(
                level,
                territoryFaction,
                player,
                pos
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