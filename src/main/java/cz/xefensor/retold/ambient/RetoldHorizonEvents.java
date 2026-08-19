package cz.xefensor.retold.ambient;

import cz.xefensor.retold.network.RetoldHorizonCuePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldHorizonEvents {
    static final int MINIMUM_FIRST_INTERVAL_DAYS = 2;
    static final int MAXIMUM_FIRST_INTERVAL_DAYS = 7;
    static final int MINIMUM_RECURRING_INTERVAL_DAYS = 5;
    static final int MAXIMUM_RECURRING_INTERVAL_DAYS = 20;
    static final int MINIMUM_DURATION_TICKS = 5 * 20;
    static final int MAXIMUM_DURATION_TICKS = 10 * 20;

    private static final int CHECK_INTERVAL_TICKS = 20;

    private RetoldHorizonEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(player.level() instanceof ServerLevel level)
                || !player.isAlive()
                || player.isSpectator()) {
            return;
        }

        ServerLevel scheduleLevel = level.getServer().overworld();
        long gameTime = scheduleLevel.getGameTime();

        if (Math.floorMod(gameTime + player.getId(), CHECK_INTERVAL_TICKS) != 0L) {
            return;
        }

        RandomSource random = level.getRandom();
        RetoldHorizonData data = RetoldHorizonData.get(scheduleLevel);

        if (data.scheduleIfAbsent(player.getUUID(), gameTime, randomFirstIntervalTicks(random))) {
            return;
        }

        // Existing saves may still contain a next appearance from the former 10-100-day range.
        data.capDelay(
                player.getUUID(),
                gameTime,
                RetoldHorizonSchedule.intervalTicksForDays(MAXIMUM_RECURRING_INTERVAL_DAYS)
        );

        if (!data.isDue(player.getUUID(), gameTime)) {
            return;
        }

        data.reschedule(player.getUUID(), gameTime, randomRecurringIntervalTicks(random));
        sendCue(player, random);
    }

    private static void sendCue(ServerPlayer player, RandomSource random) {
        PacketDistributor.sendToPlayer(
                player,
                new RetoldHorizonCuePayload(
                        random.nextLong(),
                        random.nextIntBetweenInclusive(
                                MINIMUM_DURATION_TICKS,
                                MAXIMUM_DURATION_TICKS
                        )
                )
        );
    }

    static long randomFirstIntervalTicks(RandomSource random) {
        return RetoldHorizonSchedule.intervalTicksForDays(
                random.nextIntBetweenInclusive(
                        MINIMUM_FIRST_INTERVAL_DAYS,
                        MAXIMUM_FIRST_INTERVAL_DAYS
                )
        );
    }

    static long randomRecurringIntervalTicks(RandomSource random) {
        return RetoldHorizonSchedule.intervalTicksForDays(
                random.nextIntBetweenInclusive(
                        MINIMUM_RECURRING_INTERVAL_DAYS,
                        MAXIMUM_RECURRING_INTERVAL_DAYS
                )
        );
    }
}
