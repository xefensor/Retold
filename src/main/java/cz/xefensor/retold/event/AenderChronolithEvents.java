package cz.xefensor.retold.event;

import cz.xefensor.retold.block.AenderChronolithBlock;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AenderChronolithEvents {
    // --- Gameplay tuning ---

    // Player must stay within this distance from the accelerator.
    private static final double CHANNEL_RANGE_BLOCKS = 8.0D;
    private static final double MAX_DISTANCE_SQR = CHANNEL_RANGE_BLOCKS * CHANNEL_RANGE_BLOCKS;

    // Time added every server tick while channeling.
    // 20 server ticks per second -> 16 * 20 = 320 extra time ticks per real second.
    private static final int TIME_TICKS_PER_SERVER_TICK = 16;

    // Raw XP cost.
    // 1 XP every 5 ticks -> 4 XP per real second.
    private static final int XP_DRAIN_INTERVAL_TICKS = 5;
    private static final int XP_PER_DRAIN = 1;

    // Weather is cleared once on activation for now.
    private static final boolean CLEAR_WEATHER_ON_START = true;

    // --- Feedback tuning ---

    private static final int ACTIVE_SOUND_INTERVAL_TICKS = 20;
    private static final int EXPERIENCE_BEAM_CYCLE_TICKS = 20;

    private static final Map<UUID, Channel> ACTIVE_CHANNELS = new HashMap<>();

    private AenderChronolithEvents() {
    }

    public static void toggle(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            failActivation(level, pos);
            return;
        }

        UUID playerId = player.getUUID();
        Channel existing = ACTIVE_CHANNELS.get(playerId);

        // Same player clicked the same accelerator again -> stop.
        if (existing != null && existing.isSameBlock(level.dimension(), pos)) {
            ACTIVE_CHANNELS.remove(playerId);
            stopChannel(level, existing);
            return;
        }

        // Only one accelerator can affect this dimension at once.
        if (isAnyOtherBlockActiveInDimension(level.dimension(), pos, playerId)) {
            failActivation(level, pos);
            return;
        }

        // Cannot start without XP unless in creative.
        if (!player.getAbilities().instabuild && player.totalExperience <= 0) {
            failActivation(level, pos);
            return;
        }

        // If this player was channeling a different accelerator, turn the old one off.
        if (existing != null) {
            ServerLevel oldLevel = level.getServer().getLevel(existing.dimension);
            if (oldLevel != null) {
                stopChannel(oldLevel, existing);
            }
        }

        Channel newChannel = new Channel(level.dimension(), pos.immutable());
        ACTIVE_CHANNELS.put(playerId, newChannel);

        setActive(level, pos, true);
        if (CLEAR_WEATHER_ON_START) {
            clearWeather(level);
        }
        playStartSound(level, pos);
    }

    private static boolean isAnyOtherBlockActiveInDimension(
            ResourceKey<Level> dimension,
            BlockPos pos,
            UUID currentPlayer
    ) {
        for (Map.Entry<UUID, Channel> entry : ACTIVE_CHANNELS.entrySet()) {
            Channel channel = entry.getValue();

            // Same player clicking same block is handled earlier as "stop".
            if (entry.getKey().equals(currentPlayer) && channel.isSameBlock(dimension, pos)) {
                continue;
            }

            if (channel.dimension.equals(dimension)) {
                return true;
            }
        }

        return false;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (ACTIVE_CHANNELS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, Channel>> iterator = ACTIVE_CHANNELS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Channel> entry = iterator.next();
            UUID playerId = entry.getKey();
            Channel channel = entry.getValue();

            if (!channel.dimension.equals(level.dimension())) {
                continue;
            }

            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);

            if (!isChannelStillValid(level, player, channel)) {
                stopChannel(level, channel);
                iterator.remove();
                continue;
            }

            if (!player.getAbilities().instabuild && player.totalExperience <= 0) {
                stopChannel(level, channel);
                iterator.remove();
                continue;
            }

            // Smooth movement: time advances every server tick.
            advanceTime(level, TIME_TICKS_PER_SERVER_TICK);

            // Constant visual drain from player into the block.
            spawnExperienceOrbBeam(level, channel.pos, player, channel);

            channel.activeSoundCooldown++;

            if (channel.activeSoundCooldown >= ACTIVE_SOUND_INTERVAL_TICKS) {
                channel.activeSoundCooldown = 0;
                playActiveSound(level, channel.pos);
            }

            // XP drains less often, preserving the cost:
            // 1 XP every 5 ticks = 80 added time ticks.
            channel.drainCooldown++;

            if (channel.drainCooldown >= XP_DRAIN_INTERVAL_TICKS) {
                channel.drainCooldown = 0;

                if (!consumeXp(player, XP_PER_DRAIN)) {
                    stopChannel(level, channel);
                    iterator.remove();
                    continue;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Channel channel = ACTIVE_CHANNELS.remove(player.getUUID());

        if (channel == null) {
            return;
        }

        if (!(player.level() instanceof ServerLevel playerLevel)) {
            return;
        }

        ServerLevel channelLevel = playerLevel.getServer().getLevel(channel.dimension);

        if (channelLevel != null) {
            stopChannel(channelLevel, channel);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Channel channel : ACTIVE_CHANNELS.values()) {
            ServerLevel level = event.getServer().getLevel(channel.dimension);

            if (level != null) {
                stopChannel(level, channel);
            }
        }

        ACTIVE_CHANNELS.clear();
    }

    public static void stopAt(ServerLevel level, BlockPos pos) {
        Iterator<Map.Entry<UUID, Channel>> iterator = ACTIVE_CHANNELS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Channel> entry = iterator.next();
            Channel channel = entry.getValue();

            if (!channel.isSameBlock(level.dimension(), pos)) {
                continue;
            }

            stopChannel(level, channel);
            iterator.remove();
        }
    }

    private static boolean isChannelStillValid(ServerLevel level, ServerPlayer player, Channel channel) {
        if (player == null || !player.isAlive()) {
            return false;
        }

        if (player.level() != level) {
            return false;
        }

        if (player.isShiftKeyDown()) {
            return false;
        }

        if (!level.getBlockState(channel.pos).is(RetoldBlocks.DEV_TIME_ACCELERATOR)) {
            return false;
        }

        double x = channel.pos.getX() + 0.5D;
        double y = channel.pos.getY() + 0.5D;
        double z = channel.pos.getZ() + 0.5D;

        return player.distanceToSqr(x, y, z) <= MAX_DISTANCE_SQR;
    }

    private static boolean consumeXp(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (player.totalExperience < amount) {
            return false;
        }

        player.giveExperiencePoints(-amount);
        return true;
    }

    private static void advanceTime(ServerLevel level, int amount) {
        runServerCommand(level, "time add " + amount);
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

    private static void stopChannel(ServerLevel level, Channel channel) {
        setActive(level, channel.pos, false);
        playStopSound(level, channel.pos);
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

    private static void playStartSound(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.45F);
    }

    private static void playActiveSound(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 0.35F, 0.85F);
    }

    private static void playStopSound(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.6F, 0.8F);
    }

    private static void playSound(ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playSound(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                sound,
                SoundSource.BLOCKS,
                volume,
                pitch
        );
    }

    private static void failActivation(ServerLevel level, BlockPos pos) {
        playSound(level, pos, SoundEvents.BEACON_DEACTIVATE, 0.35F, 0.55F);

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.65D;
        double z = pos.getZ() + 0.5D;

        level.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                10,
                0.25D,
                0.18D,
                0.25D,
                0.015D
        );

        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                x,
                y,
                z,
                8,
                0.3D,
                0.2D,
                0.3D,
                0.02D
        );
    }

    private static void spawnExperienceOrbBeam(
            ServerLevel level,
            BlockPos pos,
            ServerPlayer player,
            Channel channel
    ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double startX = player.getX();
        double startY = player.getY() + 1.0D;
        double startZ = player.getZ();

        double endX = pos.getX() + 0.5D;
        double endY = pos.getY() + 0.75D;
        double endZ = pos.getZ() + 0.5D;

        double deltaX = endX - startX;
        double deltaY = endY - startY;
        double deltaZ = endZ - startZ;

        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (distance <= 0.0001D) {
            return;
        }

        channel.beamTicks++;

        // More segments = more continuous-looking beam.
        int segments = Math.max(18, (int) (distance * 6.0D));

        // Moves from player -> block.
        double flow = (channel.beamTicks % 20) / 20.0D;

        for (int i = 0; i <= segments; i++) {
            double baseT = i / (double) segments;

            // Tiny travelling wave. This affects brightness/size, not whether the beam exists.
            // So the beam stays constant instead of pulsing on/off.
            double wave = Math.sin((baseT - flow) * Math.PI * 2.0D * 4.0D);
            double brightness = 0.65D + wave * 0.25D;

            double arc = Math.sin(baseT * Math.PI) * 0.14D;

            double x = startX + deltaX * baseT;
            double y = startY + deltaY * baseT + arc;
            double z = startZ + deltaZ * baseT;

            // Very small jitter, otherwise it looks like a debug ray.
            x += random.nextDouble(-0.018D, 0.018D);
            y += random.nextDouble(-0.018D, 0.018D);
            z += random.nextDouble(-0.018D, 0.018D);

            float scale = (float) (0.45D + brightness * 0.25D);

            level.sendParticles(
                    randomExperienceDust(random, brightness, scale),
                    x,
                    y,
                    z,
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        // Extra intake shimmer at the block, so the direction has a clear endpoint.
        for (int i = 0; i < 5; i++) {
            level.sendParticles(
                    randomExperienceDust(random, 1.0D, 0.75F),
                    endX + random.nextDouble(-0.08D, 0.08D),
                    endY + random.nextDouble(-0.06D, 0.06D),
                    endZ + random.nextDouble(-0.08D, 0.08D),
                    1,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        }
    }

    private static DustParticleOptions randomExperienceDust(ThreadLocalRandom random, double brightness, float scale) {
        int red = clampColor((int) ((170 + random.nextInt(0, 45)) * brightness));
        int green = clampColor((int) ((230 + random.nextInt(0, 26)) * brightness));
        int blue = clampColor((int) ((35 + random.nextInt(0, 80)) * brightness));

        return new DustParticleOptions(rgb(red, green, blue), scale);
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static int rgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }

    private static boolean isBlockUsedByAnotherPlayer(
            ResourceKey<Level> dimension,
            BlockPos pos,
            UUID currentPlayer
    ) {
        for (Map.Entry<UUID, Channel> entry : ACTIVE_CHANNELS.entrySet()) {
            if (entry.getKey().equals(currentPlayer)) {
                continue;
            }

            if (entry.getValue().isSameBlock(dimension, pos)) {
                return true;
            }
        }

        return false;
    }

    private static final class Channel {
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private int drainCooldown;
        private int activeSoundCooldown;
        private int beamTicks;

        private Channel(ResourceKey<Level> dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }

        private boolean isSameBlock(ResourceKey<Level> dimension, BlockPos pos) {
            return this.dimension.equals(dimension) && this.pos.equals(pos);
        }
    }
}