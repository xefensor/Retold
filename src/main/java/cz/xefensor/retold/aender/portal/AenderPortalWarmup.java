package cz.xefensor.retold.aender.portal;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Uses the survival portal charge as useful loading time for the destination.
 */
public final class AenderPortalWarmup {
    public static final int MAX_PREPARATION_TICKS = 100;

    private static final int SAFE_CORE_RADIUS = 2;
    private static final int MAX_CHUNKS_PER_TICK = 32;
    private static final int MAX_VISIBILITY_CHECKS_PER_TICK = 128;
    private static final long MAX_WORK_NANOS_PER_TICK = 12_000_000L;
    private static final long ABANDONED_STATE_TICKS = 1_200L;

    private static final Map<UUID, WarmupState> STATES = new HashMap<>();

    private AenderPortalWarmup() {
    }

    public static void tick(ServerLevel sourceLevel, ServerPlayer player, int chargeTicks) {
        AenderPortalLogic.AenderWarmupTarget target =
                AenderPortalLogic.getAenderWarmupTarget(sourceLevel, player);

        if (target == null) {
            return;
        }

        ServerLevel aender = target.level();
        int centerChunkX = target.center().getX() >> 4;
        int centerChunkZ = target.center().getZ() >> 4;
        int radius = AenderRealityTickEvents.arrivalPreparationRadius(aender);
        long realityEpoch = AenderVolatility.currentRealityEpoch();
        long gameTime = sourceLevel.getGameTime();

        WarmupState state = STATES.get(player.getUUID());

        if (state == null || !state.matches(centerChunkX, centerChunkZ, radius, realityEpoch)) {
            state = new WarmupState(centerChunkX, centerChunkZ, radius, realityEpoch);
            STATES.put(player.getUUID(), state);
        }

        if (state.lastWorkGameTime == gameTime) {
            return;
        }

        state.lastWorkGameTime = gameTime;
        state.lastSeenGameTime = gameTime;

        /*
         * The ticket lets Minecraft load/generate the whole destination view on
         * its chunk workers while the player is still charging the portal. Adding
         * it again refreshes the vanilla portal-ticket timeout if charging pauses.
         */
        aender.getChunkSource().addTicketWithRadius(
                TicketType.PORTAL,
                new ChunkPos(state.centerChunkX, state.centerChunkZ),
                radius
        );

        long workBudgetNanos = AenderRealityTickEvents.adaptiveRegenerationBudget(
                aender,
                MAX_WORK_NANOS_PER_TICK
        );

        if (workBudgetNanos <= 0L) {
            return;
        }

        long startedAt = System.nanoTime();
        int visibilityChecks = Math.min(state.pendingVisibility.size(), MAX_VISIBILITY_CHECKS_PER_TICK);

        while (visibilityChecks-- > 0 && !state.pendingVisibility.isEmpty()) {
            long packed = state.pendingVisibility.remove();
            int chunkX = (int) packed;
            int chunkZ = (int) (packed >> 32);

            if (!AenderRealityTickEvents.prepareArrivalVisibilityChunk(aender, chunkX, chunkZ)) {
                state.pendingVisibility.add(packed);
            }

            if (System.nanoTime() - startedAt >= workBudgetNanos) {
                break;
            }
        }

        if (!state.pendingVisibility.isEmpty()
                || System.nanoTime() - startedAt >= workBudgetNanos) {
            return;
        }

        int portalTime = player.portalProcess == null ? 0 : player.portalProcess.getPortalTime();
        int ticksRemaining = Math.max(1, chargeTicks - portalTime);
        int desiredThisTick = Math.max(1, divideCeil(state.pending.size(), ticksRemaining));
        int opportunisticThisTick = Math.max(1, (int) (workBudgetNanos / 500_000L));
        desiredThisTick = Math.max(desiredThisTick, opportunisticThisTick);
        desiredThisTick = Math.min(desiredThisTick, MAX_CHUNKS_PER_TICK);

        int prepared = 0;
        int attempts = state.pending.size();

        while (prepared < desiredThisTick && attempts-- > 0 && !state.pending.isEmpty()) {
            long packed = state.pending.remove();
            int chunkX = (int) packed;
            int chunkZ = (int) (packed >> 32);

            if (AenderRealityTickEvents.prepareLoadedArrivalChunk(aender, chunkX, chunkZ)) {
                state.pendingCore.remove(packed);
                prepared++;
            } else {
                // The ticket's asynchronous load has not completed yet; revisit it later.
                state.pending.add(packed);
            }

            if (System.nanoTime() - startedAt >= workBudgetNanos) {
                break;
            }
        }

        if (state.pending.isEmpty() && !state.loggedCompletion) {
            state.loggedCompletion = true;
            Retold.LOGGER.debug(
                    "Finished preparing the Aender view for {} during portal charge",
                    player.getGameProfile().name()
            );
        }

        if (state.arrivalReady() && target.portalCandidate() != null
                && AenderPortalShape.findComplete(aender, target.portalCandidate().centerBlock()).isEmpty()) {
            AenderPortalData.get(aender).remove(aender, target.portalCandidate());
            STATES.remove(player.getUUID());
            return;
        }

        if ((gameTime & 255L) == 0L) {
            STATES.entrySet().removeIf(entry -> gameTime - entry.getValue().lastSeenGameTime > ABANDONED_STATE_TICKS);
        }
    }

    public static void finish(Entity entity) {
        STATES.remove(entity.getUUID());
    }

    public static boolean isArrivalReady(Entity entity) {
        WarmupState state = STATES.get(entity.getUUID());
        return state != null && state.arrivalReady();
    }

    public static void clear() {
        STATES.clear();
    }

    private static int divideCeil(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    private static final class WarmupState {
        private final int centerChunkX;
        private final int centerChunkZ;
        private final int radius;
        private final long realityEpoch;
        private final Queue<Long> pending = new ArrayDeque<>();
        private final Set<Long> pendingCore = new HashSet<>();
        private final Queue<Long> pendingVisibility = new ArrayDeque<>();
        private long lastWorkGameTime = Long.MIN_VALUE;
        private long lastSeenGameTime;
        private boolean loggedCompletion;

        private WarmupState(int centerChunkX, int centerChunkZ, int radius, long realityEpoch) {
            this.centerChunkX = centerChunkX;
            this.centerChunkZ = centerChunkZ;
            this.radius = radius;
            this.realityEpoch = realityEpoch;

            for (int ring = 0; ring <= radius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                            long packed = pack(centerChunkX + dx, centerChunkZ + dz);
                            pending.add(packed);
                            pendingVisibility.add(packed);

                            if (ring <= SAFE_CORE_RADIUS) {
                                pendingCore.add(packed);
                            }
                        }
                    }
                }
            }
        }

        private boolean arrivalReady() {
            return pendingVisibility.isEmpty() && pendingCore.isEmpty();
        }

        private boolean matches(int centerChunkX, int centerChunkZ, int radius, long realityEpoch) {
            return this.centerChunkX == centerChunkX
                    && this.centerChunkZ == centerChunkZ
                    && this.radius == radius
                    && this.realityEpoch == realityEpoch;
        }

        private static long pack(int x, int z) {
            return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
        }
    }
}
