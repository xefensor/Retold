package cz.xefensor.retold.chronolith;

import cz.xefensor.retold.block.AenderChronolithBlock;
import cz.xefensor.retold.network.RetoldChronolithBeamPayload;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ChronolithController {
    private static final Map<UUID, ChronolithChannel> ACTIVE_CHANNELS = new HashMap<>();

    private ChronolithController() {
    }

    public static void toggle(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            ChronolithFeedback.playFailActivation(level, pos);
            return;
        }

        UUID playerId = player.getUUID();
        ChronolithChannel existing = ACTIVE_CHANNELS.get(playerId);

        if (existing != null && existing.isSameBlock(level.dimension(), pos)) {
            ACTIVE_CHANNELS.remove(playerId);
            stopChannel(level, existing, ChronolithStopReason.MANUAL);
            return;
        }

        if (isAnyOtherBlockActiveInDimension(level.dimension(), pos, playerId)) {
            ChronolithFeedback.playFailActivation(level, pos);
            return;
        }

        if (!canPayXp(player, 1)) {
            ChronolithFeedback.playFailActivation(level, pos);
            return;
        }

        if (existing != null) {
            ServerLevel oldLevel = level.getServer().getLevel(existing.dimension());
            if (oldLevel != null) {
                stopChannel(oldLevel, existing, ChronolithStopReason.REPLACED);
            }
        }

        ChronolithChannel newChannel = new ChronolithChannel(playerId, level.dimension(), pos.immutable());
        ACTIVE_CHANNELS.put(playerId, newChannel);

        setActive(level, pos, true);
        if (ChronolithTuning.CLEAR_WEATHER_ON_START) {
            clearWeather(level);
        }
        ChronolithFeedback.playStart(level, pos);
        syncChannelBeam(newChannel, true);
    }

    public static void tickLevel(ServerLevel level) {
        if (!level.dimension().equals(Level.OVERWORLD) || ACTIVE_CHANNELS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, ChronolithChannel>> iterator = ACTIVE_CHANNELS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ChronolithChannel> entry = iterator.next();
            ChronolithChannel channel = entry.getValue();

            if (!channel.dimension().equals(level.dimension())) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            ChronolithStopReason invalidReason = invalidReason(level, player, channel);

            if (invalidReason != null) {
                stopChannel(level, channel, invalidReason);
                iterator.remove();
                continue;
            }

            if (!canPayXp(player, 1)) {
                stopChannel(level, channel, ChronolithStopReason.OUT_OF_XP);
                iterator.remove();
                continue;
            }

            channel.incrementActiveTicks();
            float power = rampPower(channel.activeTicks());
            int addedTimeTicks = advanceTime(level, channel, power);

            tickFeedback(level, channel, power);

            if (!drainXp(player, channel, addedTimeTicks)) {
                stopChannel(level, channel, ChronolithStopReason.OUT_OF_XP);
                iterator.remove();
            }
        }
    }

    public static void stopForPlayerLogout(ServerPlayer player) {
        ChronolithChannel channel = ACTIVE_CHANNELS.remove(player.getUUID());

        if (channel == null || !(player.level() instanceof ServerLevel playerLevel)) {
            return;
        }

        ServerLevel channelLevel = playerLevel.getServer().getLevel(channel.dimension());
        if (channelLevel != null) {
            stopChannel(channelLevel, channel, ChronolithStopReason.PLAYER_LEFT);
        }
    }

    public static void stopAll(MinecraftServer server) {
        for (ChronolithChannel channel : ACTIVE_CHANNELS.values()) {
            ServerLevel level = server.getLevel(channel.dimension());

            if (level != null) {
                stopChannel(level, channel, ChronolithStopReason.SERVER_STOP);
            }
        }

        ACTIVE_CHANNELS.clear();
    }

    public static void stopAt(ServerLevel level, BlockPos pos) {
        Iterator<Map.Entry<UUID, ChronolithChannel>> iterator = ACTIVE_CHANNELS.entrySet().iterator();

        while (iterator.hasNext()) {
            ChronolithChannel channel = iterator.next().getValue();

            if (!channel.isSameBlock(level.dimension(), pos)) {
                continue;
            }

            stopChannel(level, channel, ChronolithStopReason.BLOCK_REMOVED);
            iterator.remove();
        }
    }

    public static void syncToPlayer(ServerPlayer player) {
        ResourceKey<Level> dimension = player.level().dimension();

        for (ChronolithChannel channel : ACTIVE_CHANNELS.values()) {
            if (!channel.dimension().equals(dimension)) {
                continue;
            }

            PacketDistributor.sendToPlayer(
                    player,
                    new RetoldChronolithBeamPayload(true, channel.playerId(), channel.pos())
            );
        }
    }

    private static boolean isAnyOtherBlockActiveInDimension(
            ResourceKey<Level> dimension,
            BlockPos pos,
            UUID currentPlayer
    ) {
        for (Map.Entry<UUID, ChronolithChannel> entry : ACTIVE_CHANNELS.entrySet()) {
            ChronolithChannel channel = entry.getValue();

            if (entry.getKey().equals(currentPlayer) && channel.isSameBlock(dimension, pos)) {
                continue;
            }

            if (channel.dimension().equals(dimension)) {
                return true;
            }
        }

        return false;
    }

    private static ChronolithStopReason invalidReason(ServerLevel level, ServerPlayer player, ChronolithChannel channel) {
        if (player == null || !player.isAlive() || player.level() != level) {
            return ChronolithStopReason.INTERRUPTED;
        }

        if (player.isShiftKeyDown()) {
            return ChronolithStopReason.MANUAL;
        }

        if (!level.getBlockState(channel.pos()).is(RetoldBlocks.DEV_TIME_ACCELERATOR)) {
            return ChronolithStopReason.BLOCK_REMOVED;
        }

        double x = channel.pos().getX() + 0.5D;
        double y = channel.pos().getY() + 0.5D;
        double z = channel.pos().getZ() + 0.5D;

        if (player.distanceToSqr(x, y, z) > ChronolithTuning.MAX_DISTANCE_SQR) {
            return ChronolithStopReason.INTERRUPTED;
        }

        return null;
    }

    private static int advanceTime(ServerLevel level, ChronolithChannel channel, float power) {
        channel.setTimeTickProgress(channel.timeTickProgress() + ChronolithTuning.TIME_TICKS_PER_SERVER_TICK * power);
        int addedTimeTicks = Mth.floor(channel.timeTickProgress());

        if (addedTimeTicks <= 0) {
            return 0;
        }

        channel.setTimeTickProgress(channel.timeTickProgress() - addedTimeTicks);
        runServerCommand(level, "time add " + addedTimeTicks);
        return addedTimeTicks;
    }

    private static boolean drainXp(ServerPlayer player, ChronolithChannel channel, int addedTimeTicks) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        channel.setXpDrainProgress(
                channel.xpDrainProgress() + addedTimeTicks / ChronolithTuning.ADDED_TIME_TICKS_PER_XP
        );

        while (channel.xpDrainProgress() >= 1.0F) {
            if (!consumeXp(player, 1)) {
                return false;
            }

            channel.setXpDrainProgress(channel.xpDrainProgress() - 1.0F);
        }

        return true;
    }

    private static void tickFeedback(ServerLevel level, ChronolithChannel channel, float power) {
        channel.incrementActiveSoundCooldown();
        if (channel.activeSoundCooldown() >= ChronolithTuning.ACTIVE_SOUND_INTERVAL_TICKS) {
            channel.resetActiveSoundCooldown();
            ChronolithFeedback.playActiveSound(level, channel.pos());
        }

        channel.incrementActiveParticleCooldown();
        if (channel.activeParticleCooldown() >= ChronolithTuning.ACTIVE_PARTICLE_INTERVAL_TICKS) {
            channel.resetActiveParticleCooldown();
            ChronolithFeedback.playActivePulse(level, channel.pos(), power);
        }
    }

    private static boolean consumeXp(ServerPlayer player, int amount) {
        if (!canPayXp(player, amount)) {
            return false;
        }

        player.giveExperiencePoints(-amount);
        return true;
    }

    private static boolean canPayXp(ServerPlayer player, int amount) {
        return player.getAbilities().instabuild || player.totalExperience >= amount;
    }

    private static float rampPower(int activeTicks) {
        return Mth.clamp(activeTicks / (float) ChronolithTuning.RAMP_UP_TICKS, 0.0F, 1.0F);
    }

    private static void clearWeather(ServerLevel level) {
        runServerCommand(level, "weather clear");
    }

    private static void runServerCommand(ServerLevel level, String command) {
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer()
                        .createCommandSourceStack()
                        .withLevel(level)
                        .withSuppressedOutput(),
                command
        );
    }

    private static void stopChannel(ServerLevel level, ChronolithChannel channel, ChronolithStopReason reason) {
        setActive(level, channel.pos(), false);
        ChronolithFeedback.playStop(level, channel.pos(), reason);
        syncChannelBeam(channel, false);
    }

    private static void setActive(ServerLevel level, BlockPos pos, boolean active) {
        if (!level.getBlockState(pos).is(RetoldBlocks.DEV_TIME_ACCELERATOR)) {
            return;
        }

        level.setBlock(
                pos,
                level.getBlockState(pos).setValue(AenderChronolithBlock.LIT, active),
                3
        );
    }

    private static void syncChannelBeam(ChronolithChannel channel, boolean active) {
        PacketDistributor.sendToAllPlayers(
                new RetoldChronolithBeamPayload(active, channel.playerId(), channel.pos())
        );
    }
}
