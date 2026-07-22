package cz.xefensor.retold.aender.stability;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks generator regions watched by at least one Aender player.
 */
final class AenderActiveRegionTracker {
    private final Set<Long> activeRegions = new HashSet<>();
    private boolean initialized;

    Set<Long> update(Set<Long> currentlyActive, boolean hasPlayers) {
        if (!hasPlayers) {
            clear();
            return Set.of();
        }

        // Dimension transfer can register a player before their first chunks are tracked.
        if (currentlyActive.isEmpty()) {
            return Set.of();
        }

        if (!initialized) {
            activeRegions.addAll(currentlyActive);
            initialized = true;
            return Set.of();
        }

        Set<Long> departed = new HashSet<>(activeRegions);
        departed.removeAll(currentlyActive);
        activeRegions.clear();
        activeRegions.addAll(currentlyActive);
        return Set.copyOf(departed);
    }

    void clear() {
        activeRegions.clear();
        initialized = false;
    }
}
