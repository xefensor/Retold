package cz.xefensor.retold.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import cz.xefensor.retold.command.showcase.RetoldShowcaseWorld;
import cz.xefensor.retold.network.RetoldEndSkySeedSyncPayload;
import cz.xefensor.retold.sky.RetoldEndSkyData;
import cz.xefensor.retold.stage.RetoldStageManager;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.villager.RetoldVillageReputationStatus;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldCommands {
    private RetoldCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("retold")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("testworld")
                                .executes(context -> createTestWorld(context.getSource(), null))
                                .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                        .executes(context -> createTestWorld(context.getSource(),
                                                BlockPosArgument.getBlockPos(context, "origin"))))
                        )
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
                        .then(Commands.literal("village")
                                .then(Commands.literal("status")
                                        .executes(context -> getVillageStatus(
                                                context.getSource()
                                        ))
                                )
                        )
        );
    }

    private static int createTestWorld(CommandSourceStack source, BlockPos requestedOrigin) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("Run this command as a player in an Overworld test world."));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos origin = requestedOrigin;
        if (origin == null) {
            RetoldShowcaseWorld.Plan layout = RetoldShowcaseWorld.plan(BlockPos.ZERO);
            origin = new BlockPos(player.getBlockX() - layout.width() / 2, level.getMaxY() - 18,
                    player.getBlockZ() - layout.depth() / 2);
        }
        RetoldShowcaseWorld.Plan plan = RetoldShowcaseWorld.plan(origin);
        RetoldShowcaseWorld.BuildResult result = RetoldShowcaseWorld.build(level, plan);
        if (!result.created()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        BlockPos entrance = origin.offset(plan.width() / 2, 1, 2);
        player.teleportTo(level, entrance.getX() + 0.5D, entrance.getY(), entrance.getZ() + 0.5D,
                java.util.Set.of(), 0.0F, 0.0F, true);
        source.sendSuccess(() -> Component.literal(result.message() + " Entrance: " + entrance.toShortString()
                + ". Exhibits are passive; world stage and game rules are unchanged."), false);
        return 1;
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

    private static int getVillageStatus(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.literal(
                            "This command must be run by a player."
                    )
            );
            return 0;
        }

        RetoldVillageReputationStatus.Snapshot status =
                RetoldVillageReputationStatus.inspect(
                        source.getLevel(),
                        player
                );

        if (!status.hasVillage()) {
            source.sendSuccess(
                    () -> Component.literal(
                            "No loaded village-context Villagers found within "
                                    + RetoldVillageReputationStatus.QUERY_RADIUS
                                    + " blocks."
                    ),
                    false
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Village standing: "
                                + status.standing().displayName()
                                + "; villagers=" + status.villagerCount()
                                + ", reputation average="
                                + status.averageReputation()
                                + ", worst=" + status.worstReputation()
                                + ", best=" + status.bestReputation()
                                + ", negative=" + status.negativeVillagers()
                                + ", golem hostility="
                                + (status.hasGolemHostilityRisk()
                                ? "possible" : "no")
                ),
                false
        );

        return status.villagerCount();
    }

}
