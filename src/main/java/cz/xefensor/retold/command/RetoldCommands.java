package cz.xefensor.retold.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.stage.RetoldStageManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

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
}