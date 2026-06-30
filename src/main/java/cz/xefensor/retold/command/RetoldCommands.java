package cz.xefensor.retold.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.network.RetoldEndSkySeedSyncPayload;
import cz.xefensor.retold.sky.RetoldEndSkyData;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldCommands {
    private RetoldCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("retold")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("stage")
                                .then(Commands.literal("get")
                                        .executes(context -> getStage(context.getSource()))
                                )
                                .then(Commands.literal("set")
                                        .then(Commands.argument("stage", IntegerArgumentType.integer(1, 3))
                                                .executes(context -> setStage(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "stage")
                                                ))
                                        )
                                )
                        )
                        .then(Commands.literal("sky")
                                .then(Commands.literal("get")
                                        .executes(context -> getEndSkySeed(context.getSource()))
                                )
                                .then(Commands.literal("randomize")
                                        .executes(context -> randomizeEndSkySeed(context.getSource()))
                                )
                        )
        );
    }

    private static int getStage(CommandSourceStack source) {
        ServerLevel level = source.getLevel();

        RetoldWorldStage stage = RetoldWorldData.get(level).getStage();

        source.sendSuccess(
                () -> Component.literal("Current Retold stage: " + stage.getId()),
                false
        );

        return stage.getId();
    }

    private static int setStage(CommandSourceStack source, int stageId) {
        ServerLevel level = source.getLevel();

        RetoldWorldStage stage = RetoldWorldStage.getStageFromId(stageId);
        RetoldStageManager.setStage(level, stage);

        source.sendSuccess(
                () -> Component.literal("Set Retold stage to: " + stage.getId()),
                true
        );

        return stage.getId();
    }

    private static int getEndSkySeed(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long seed = RetoldEndSkyData.get(level).getSeed();

        source.sendSuccess(
                () -> Component.literal("Current Retold End sky seed: " + seed),
                false
        );

        return 1;
    }

    private static int randomizeEndSkySeed(CommandSourceStack source) {
        ServerLevel level = source.getLevel();

        RetoldEndSkyData data = RetoldEndSkyData.get(level);
        data.randomizeSeed();

        long seed = data.getSeed();

        PacketDistributor.sendToAllPlayers(
                new RetoldEndSkySeedSyncPayload(seed)
        );

        source.sendSuccess(
                () -> Component.literal("Randomized Retold End sky seed: " + seed),
                true
        );

        return 1;
    }
}