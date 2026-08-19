package cz.xefensor.retold.ambient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class RetoldHorizonSchedule {
    static final int SAVE_VERSION = 1;
    static final long TICKS_PER_DAY = 24_000L;

    private final Map<UUID, Long> nextCueByPlayer = new HashMap<>();

    boolean scheduleIfAbsent(UUID playerId, long gameTime, long delayTicks) {
        validateDelay(delayTicks);

        if (nextCueByPlayer.containsKey(playerId)) {
            return false;
        }

        nextCueByPlayer.put(playerId, addWithoutOverflow(gameTime, delayTicks));
        return true;
    }

    boolean isDue(UUID playerId, long gameTime) {
        Long nextCue = nextCueByPlayer.get(playerId);
        return nextCue != null && gameTime >= nextCue;
    }

    void reschedule(UUID playerId, long gameTime, long delayTicks) {
        validateDelay(delayTicks);
        nextCueByPlayer.put(playerId, addWithoutOverflow(gameTime, delayTicks));
    }

    boolean capDelay(UUID playerId, long gameTime, long maximumDelayTicks) {
        validateDelay(maximumDelayTicks);
        Long nextCue = nextCueByPlayer.get(playerId);

        if (nextCue == null) {
            return false;
        }

        long latestAllowedCue = addWithoutOverflow(gameTime, maximumDelayTicks);

        if (nextCue <= latestAllowedCue) {
            return false;
        }

        nextCueByPlayer.put(playerId, latestAllowedCue);
        return true;
    }

    long nextCue(UUID playerId) {
        return nextCueByPlayer.getOrDefault(playerId, -1L);
    }

    SerializedState serialize() {
        List<PlayerEntry> players = nextCueByPlayer.entrySet().stream()
                .map(entry -> new PlayerEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PlayerEntry::playerId))
                .toList();
        return new SerializedState(SAVE_VERSION, players);
    }

    static RetoldHorizonSchedule fromSerializedState(SerializedState state) {
        if (state.version() != SAVE_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported horizon schedule version: " + state.version()
            );
        }

        RetoldHorizonSchedule schedule = new RetoldHorizonSchedule();
        Set<UUID> playerIds = new HashSet<>();

        for (PlayerEntry player : state.players()) {
            if (!playerIds.add(player.playerId())) {
                throw new IllegalArgumentException(
                        "Duplicate horizon schedule player: " + player.playerId()
                );
            }

            if (player.nextCueAt() < 0L) {
                throw new IllegalArgumentException(
                        "Negative horizon schedule tick for player: " + player.playerId()
                );
            }

            schedule.nextCueByPlayer.put(player.playerId(), player.nextCueAt());
        }

        return schedule;
    }

    static long intervalTicksForDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Horizon schedule days must be positive");
        }

        return days * TICKS_PER_DAY;
    }

    private static void validateDelay(long delayTicks) {
        if (delayTicks <= 0L) {
            throw new IllegalArgumentException("Horizon schedule delay must be positive");
        }
    }

    private static long addWithoutOverflow(long gameTime, long delayTicks) {
        if (gameTime > Long.MAX_VALUE - delayTicks) {
            return Long.MAX_VALUE;
        }

        return gameTime + delayTicks;
    }

    record PlayerEntry(UUID playerId, long nextCueAt) {
    }

    record SerializedState(int version, List<PlayerEntry> players) {
        SerializedState {
            players = List.copyOf(new ArrayList<>(players));
        }
    }
}
