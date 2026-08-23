package cz.xefensor.retold.worldgen.portal;

import cz.xefensor.retold.api.world.RetoldWorldProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.BlockUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Bounded loaded-chunk owner for permanent Overworld Nether-portal drain. */
public final class RetoldNetherPortalDrainEvents {
    static final int OUTER_RADIUS = 16;
    static final int INNER_RADIUS = 8;
    static final int MAX_OUTER_RADIUS = 48;
    static final int PULSE_CHANGES_PER_TRAVEL = 12;
    static final int MAX_QUEUED_PULSE_CHANGES = 96;

    private static final int BASE_PORTAL_AREA = 6;
    private static final int BACKGROUND_INTERVAL_TICKS = 40;
    private static final int MAX_CHUNK_INDEXES_PER_TICK = 2;
    private static final int MAX_BACKGROUND_PORTALS_PER_INTERVAL = 16;
    private static final int MAX_PULSE_CHANGES_PER_TICK = 8;
    private static final int MAX_PULSE_CHANGES_PER_PORTAL_TICK = 2;
    private static final int LAVA_SCAN_INTERVAL_TICKS = 5;
    private static final int MAX_LAVA_CHUNKS_PER_SCAN_TICK = 2;
    private static final int MAX_ORDERED_PROBES_PER_CHANGE = 48;
    private static final int MAX_PORTAL_SIZE = 21;
    private static final int OFFSET_COORDINATE_BITS = 7;
    private static final int OFFSET_COORDINATE_MASK =
            (1 << OFFSET_COORDINATE_BITS) - 1;
    private static final int OFFSET_COORDINATE_KEY_BITS =
            OFFSET_COORDINATE_BITS * 3;
    private static final long[] ORDERED_DRAIN_OFFSETS =
            createOrderedDrainOffsets();

    private static final Map<ResourceKey<Level>, PortalTracker> TRACKERS =
            new HashMap<>();

    private RetoldNetherPortalDrainEvents() {
    }

    @SubscribeEvent
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            tracker(level).pendingChunks.add(chunkKey(event.getPos()));
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD) {
            tracker(level).pendingChunks.add(event.getChunk().getPos().pack());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }

        PortalTracker tracker = TRACKERS.get(level.dimension());
        if (tracker == null) {
            return;
        }

        long unloadedChunk = event.getChunk().getPos().pack();
        tracker.pendingChunks.remove(unloadedChunk);

        Iterator<Map.Entry<Long, PortalRecord>> iterator =
                tracker.portals.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, PortalRecord> entry = iterator.next();
            if (ChunkPos.containing(entry.getValue().minCorner()).pack()
                    == unloadedChunk) {
                iterator.remove();
                tracker.pulseWork.remove(entry.getKey());
                tracker.queuedPulses.remove(entry.getKey());
                tracker.progress.remove(entry.getKey());
                tracker.lavaResistance.remove(entry.getKey());
                tracker.queuedLavaScans.remove(entry.getKey());
                tracker.lavaScanQueue.removeIf(
                        candidate -> candidate.equals(entry.getKey())
                );
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }

        PortalTracker tracker = TRACKERS.get(level.dimension());
        if (tracker == null) {
            return;
        }

        indexPendingChunks(level, tracker);
        if (level.getGameTime() % LAVA_SCAN_INTERVAL_TICKS == 0L) {
            processLavaResistanceScans(level, tracker);
        }
        processPulseWork(level, tracker);

        if (level.getGameTime() % BACKGROUND_INTERVAL_TICKS == 0L) {
            processBackgroundDrain(level, tracker);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TRACKERS.clear();
    }

    public static void queueTravelPulse(ServerLevel level, BlockPos portalPos) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        PortalRecord record = registerPortalNear(level, portalPos);
        if (record == null) {
            tracker(level).pendingChunks.add(chunkKey(portalPos));
            return;
        }

        PortalTracker tracker = tracker(level);
        long key = record.minCorner().asLong();
        int pending = tracker.pulseWork.getOrDefault(key, 0);
        tracker.pulseWork.put(
                key,
                Math.min(
                        MAX_QUEUED_PULSE_CHANGES,
                        pending + PULSE_CHANGES_PER_TRAVEL
                )
        );

        if (tracker.queuedPulses.add(key)) {
            tracker.pulseQueue.addLast(key);
        }
    }

    static boolean drainAt(
            ServerLevel level,
            BlockPos portalCenter,
            BlockPos target
    ) {
        return transformAt(
                level,
                portalCenter,
                target,
                DrainPhase.DRAIN,
                OUTER_RADIUS
        );
    }

    static boolean corruptAt(
            ServerLevel level,
            BlockPos portalCenter,
            BlockPos target
    ) {
        return transformAt(
                level,
                portalCenter,
                target,
                DrainPhase.CORRUPT,
                INNER_RADIUS
        );
    }

    private static boolean transformAt(
            ServerLevel level,
            BlockPos portalCenter,
            BlockPos target,
            DrainPhase phase,
            int radius
    ) {
        int dx = target.getX() - portalCenter.getX();
        int dy = target.getY() - portalCenter.getY();
        int dz = target.getZ() - portalCenter.getZ();
        int distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared == 0
                || distanceSquared > radius * radius
                || level.getChunkSource().getChunkNow(
                target.getX() >> 4,
                target.getZ() >> 4
        ) == null) {
            return false;
        }

        BlockState source = level.getBlockState(target);
        BlockState transformed = phase == DrainPhase.DRAIN
                ? RetoldNetherPortalDrainTransforms.resolveDrain(
                        level,
                        target,
                        source
                )
                : RetoldNetherPortalDrainTransforms.resolveCorruption(
                        level,
                        target,
                        source
                );

        if (transformed == null
                || transformed == source
                || !RetoldWorldProtection.canDrainAroundNetherPortal(
                level,
                target
        )) {
            return false;
        }

        return level.setBlock(target, transformed, Block.UPDATE_ALL);
    }

    static int pendingPulseWorkForTest(ServerLevel level, BlockPos portalPos) {
        PortalRecord record = resolvePortal(level, portalPos);
        if (record == null) {
            return 0;
        }

        PortalTracker tracker = TRACKERS.get(level.dimension());
        return tracker == null
                ? 0
                : tracker.pulseWork.getOrDefault(record.minCorner().asLong(), 0);
    }

    static void clearForTests() {
        TRACKERS.clear();
    }

    private static void indexPendingChunks(
            ServerLevel level,
            PortalTracker tracker
    ) {
        Iterator<Long> iterator = tracker.pendingChunks.iterator();
        int indexed = 0;

        while (iterator.hasNext() && indexed < MAX_CHUNK_INDEXES_PER_TICK) {
            long packedChunk = iterator.next();
            iterator.remove();
            ChunkPos chunkPos = ChunkPos.unpack(packedChunk);
            LevelChunk chunk = level.getChunkSource().getChunkNow(
                    chunkPos.x(),
                    chunkPos.z()
            );

            if (chunk == null) {
                continue;
            }

            Set<Long> coveredPortalBlocks = new HashSet<>();
            chunk.findBlocks(
                    state -> state.is(Blocks.NETHER_PORTAL),
                    (pos, state) -> {
                        if (coveredPortalBlocks.contains(pos.asLong())) {
                            return;
                        }

                        PortalRecord record = registerPortalAt(level, pos);
                        if (record != null) {
                            record.addPortalBlocksTo(coveredPortalBlocks);
                        }
                    }
            );
            indexed++;
        }
    }

    private static void processBackgroundDrain(
            ServerLevel level,
            PortalTracker tracker
    ) {
        int visits = Math.min(
                MAX_BACKGROUND_PORTALS_PER_INTERVAL,
                tracker.backgroundQueue.size()
        );

        for (int visit = 0; visit < visits; visit++) {
            Long key = tracker.backgroundQueue.removeFirst();
            PortalRecord record = tracker.portals.get(key);

            if (record == null || !record.isActive(level)) {
                removePortal(tracker, key);
                continue;
            }

            tryNextOutwardTransformation(level, tracker, key, record);
            tracker.backgroundQueue.addLast(key);
        }
    }

    private static void processLavaResistanceScans(
            ServerLevel level,
            PortalTracker tracker
    ) {
        int visits = Math.min(
                MAX_LAVA_CHUNKS_PER_SCAN_TICK,
                tracker.lavaScanQueue.size()
        );

        for (int visit = 0; visit < visits; visit++) {
            Long key = tracker.lavaScanQueue.removeFirst();
            tracker.queuedLavaScans.remove(key);
            PortalRecord record = tracker.portals.get(key);
            PortalResistance resistance = tracker.lavaResistance.get(key);

            if (record == null
                    || resistance == null
                    || !record.isActive(level)) {
                removePortal(tracker, key);
                continue;
            }

            resistance.scanNextChunk(level);
            queueLavaScan(tracker, key);
        }
    }

    private static void processPulseWork(
            ServerLevel level,
            PortalTracker tracker
    ) {
        int remainingBudget = MAX_PULSE_CHANGES_PER_TICK;
        int visits = tracker.pulseQueue.size();

        while (remainingBudget > 0
                && visits-- > 0
                && !tracker.pulseQueue.isEmpty()) {
            Long key = tracker.pulseQueue.removeFirst();
            tracker.queuedPulses.remove(key);
            PortalRecord record = tracker.portals.get(key);
            int work = tracker.pulseWork.getOrDefault(key, 0);

            if (record == null || work <= 0 || !record.isActive(level)) {
                removePortal(tracker, key);
                continue;
            }

            PortalResistance resistance = tracker.lavaResistance.get(key);
            if (resistance == null || !resistance.hasSnapshot()) {
                tracker.pulseQueue.addLast(key);
                tracker.queuedPulses.add(key);
                continue;
            }

            int attempts = Math.min(
                    Math.min(work, MAX_PULSE_CHANGES_PER_PORTAL_TICK),
                    remainingBudget
            );
            for (int attempt = 0; attempt < attempts; attempt++) {
                tryNextOutwardTransformation(level, tracker, key, record);
            }

            remainingBudget -= attempts;
            work -= attempts;
            if (work > 0) {
                tracker.pulseWork.put(key, work);
                tracker.pulseQueue.addLast(key);
                tracker.queuedPulses.add(key);
            } else {
                tracker.pulseWork.remove(key);
            }
        }
    }

    private static boolean tryNextOutwardTransformation(
            ServerLevel level,
            PortalTracker tracker,
            long portalKey,
            PortalRecord portal
    ) {
        PortalResistance resistance = tracker.lavaResistance.get(portalKey);
        if (resistance == null
                || !resistance.allowsSpread(portal.area())) {
            return false;
        }

        BlockPos center = portal.center();
        PortalProgress progress = tracker.progress.computeIfAbsent(
                portalKey,
                ignored -> new PortalProgress(portal)
        );
        progress.resetIfComplete();
        DrainPhase phase = progress.nextPhase();

        for (int probe = 0; probe < MAX_ORDERED_PROBES_PER_CHANGE; probe++) {
            if (!progress.hasCandidate(phase)) {
                break;
            }

            BlockPos target = targetAt(
                    center,
                    progress.nextOffsetIndex(phase)
            );
            if (target.getY() < level.getMinY()
                    || target.getY() >= level.getMaxY()) {
                continue;
            }

            boolean transformed = phase == DrainPhase.DRAIN
                    ? transformAt(
                            level,
                            center,
                            target,
                            phase,
                            portal.outerRadius()
                    )
                    : transformAt(
                            level,
                            center,
                            target,
                            phase,
                            portal.innerRadius()
                    );
            if (transformed) {
                progress.completeTurn(phase);
                return true;
            }
        }

        progress.completeTurn(phase);
        return false;
    }

    static int offsetCountForRadiusForTest(int radius) {
        return countOffsetsWithin(radius);
    }

    static int offsetDistanceSquaredForTest(int index) {
        return distanceSquaredAt(index);
    }

    static int outerRadiusForPortalForTest(int width, int height) {
        return outerRadiusForPortal(width, height);
    }

    static boolean corruptionFitsBehindDeathFrontForTest(
            int deathDistanceSquared,
            int corruptionDistanceSquared
    ) {
        return corruptionFitsBehindDeathFront(
                deathDistanceSquared,
                corruptionDistanceSquared
        );
    }

    static int lavaSourcesInAffectedAreaForTest(
            ServerLevel level,
            BlockPos center,
            int radius
    ) {
        int minChunkX = (center.getX() - radius) >> 4;
        int maxChunkX = (center.getX() + radius) >> 4;
        int minChunkZ = (center.getZ() - radius) >> 4;
        int maxChunkZ = (center.getZ() + radius) >> 4;
        int count = 0;

        for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                count += countLavaSourcesInChunk(
                        level,
                        center,
                        radius,
                        chunkX,
                        chunkZ
                );
            }
        }

        return count;
    }

    static int allowedSpreadAttemptsForTest(
            int portalArea,
            int lavaSources,
            int attempts
    ) {
        LinearSpreadLimiter limiter = new LinearSpreadLimiter();
        int allowed = 0;

        for (int attempt = 0; attempt < attempts; attempt++) {
            if (limiter.allows(portalArea, lavaSources)) {
                allowed++;
            }
        }

        return allowed;
    }

    private static long[] createOrderedDrainOffsets() {
        int maximumDistanceSquared = MAX_OUTER_RADIUS * MAX_OUTER_RADIUS;
        int[] distanceCounts = new int[maximumDistanceSquared + 1];

        for (int y = -MAX_OUTER_RADIUS; y <= MAX_OUTER_RADIUS; y++) {
            for (int z = -MAX_OUTER_RADIUS; z <= MAX_OUTER_RADIUS; z++) {
                for (int x = -MAX_OUTER_RADIUS; x <= MAX_OUTER_RADIUS; x++) {
                    int distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared > 0
                            && distanceSquared <= maximumDistanceSquared) {
                        distanceCounts[distanceSquared]++;
                    }
                }
            }
        }

        int totalOffsets = 0;
        int[] nextIndex = new int[distanceCounts.length];
        for (int distanceSquared = 1;
             distanceSquared < distanceCounts.length;
             distanceSquared++) {
            nextIndex[distanceSquared] = totalOffsets;
            totalOffsets += distanceCounts[distanceSquared];
        }

        long[] ordered = new long[totalOffsets];
        for (int y = -MAX_OUTER_RADIUS; y <= MAX_OUTER_RADIUS; y++) {
            for (int z = -MAX_OUTER_RADIUS; z <= MAX_OUTER_RADIUS; z++) {
                for (int x = -MAX_OUTER_RADIUS; x <= MAX_OUTER_RADIUS; x++) {
                    int distanceSquared = x * x + y * y + z * z;
                    if (distanceSquared > 0
                            && distanceSquared <= maximumDistanceSquared) {
                        ordered[nextIndex[distanceSquared]++] = packOffset(
                                x,
                                y,
                                z,
                                distanceSquared
                        );
                    }
                }
            }
        }

        return ordered;
    }

    private static long packOffset(
            int x,
            int y,
            int z,
            int distanceSquared
    ) {
        long coordinateKey = (long) (y + MAX_OUTER_RADIUS)
                << OFFSET_COORDINATE_BITS * 2
                | (long) (z + MAX_OUTER_RADIUS)
                << OFFSET_COORDINATE_BITS
                | x + MAX_OUTER_RADIUS;
        return (long) distanceSquared << OFFSET_COORDINATE_KEY_BITS
                | coordinateKey;
    }

    private static BlockPos targetAt(BlockPos center, int index) {
        long packed = ORDERED_DRAIN_OFFSETS[index];
        int x = (int) (packed & OFFSET_COORDINATE_MASK)
                - MAX_OUTER_RADIUS;
        int z = (int) (packed >> OFFSET_COORDINATE_BITS
                & OFFSET_COORDINATE_MASK) - MAX_OUTER_RADIUS;
        int y = (int) (packed >> OFFSET_COORDINATE_BITS * 2
                & OFFSET_COORDINATE_MASK) - MAX_OUTER_RADIUS;
        return center.offset(x, y, z);
    }

    private static int distanceSquaredAt(int index) {
        return (int) (ORDERED_DRAIN_OFFSETS[index]
                >> OFFSET_COORDINATE_KEY_BITS);
    }

    private static int countOffsetsWithin(int radius) {
        int low = 0;
        int high = ORDERED_DRAIN_OFFSETS.length;
        int maximumDistanceSquared = radius * radius;

        while (low < high) {
            int middle = (low + high) >>> 1;
            if (distanceSquaredAt(middle) <= maximumDistanceSquared) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }

        return low;
    }

    private static int outerRadiusForPortal(int width, int height) {
        int area = width * height;
        return Math.min(
                MAX_OUTER_RADIUS,
                OUTER_RADIUS + Math.max(0, area - BASE_PORTAL_AREA)
        );
    }

    private static boolean corruptionFitsBehindDeathFront(
            int deathDistanceSquared,
            int corruptionDistanceSquared
    ) {
        return corruptionDistanceSquared * 4 <= deathDistanceSquared;
    }

    private static int countLavaSourcesInChunk(
            ServerLevel level,
            BlockPos center,
            int radius,
            int chunkX,
            int chunkZ
    ) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }

        int[] count = {0};
        int radiusSquared = radius * radius;
        chunk.findBlocks(
                state -> state.getFluidState().is(FluidTags.LAVA)
                        && state.getFluidState().isSource(),
                (pos, state) -> {
                    int dx = pos.getX() - center.getX();
                    int dy = pos.getY() - center.getY();
                    int dz = pos.getZ() - center.getZ();
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        count[0]++;
                    }
                }
        );
        return count[0];
    }

    private static @Nullable PortalRecord registerPortalNear(
            ServerLevel level,
            BlockPos pos
    ) {
        PortalRecord direct = registerPortalAt(level, pos);
        if (direct != null) {
            return direct;
        }

        for (BlockPos candidate : BlockPos.withinManhattan(pos, 2, 2, 2)) {
            if (level.getBlockState(candidate).is(Blocks.NETHER_PORTAL)) {
                return registerPortalAt(level, candidate);
            }
        }

        return null;
    }

    private static @Nullable PortalRecord registerPortalAt(
            ServerLevel level,
            BlockPos portalPos
    ) {
        PortalRecord record = resolvePortal(level, portalPos);
        if (record == null) {
            return null;
        }

        PortalTracker tracker = tracker(level);
        long key = record.minCorner().asLong();
        PortalRecord existing = tracker.portals.get(key);
        if (existing == null) {
            tracker.portals.put(key, record);
            tracker.backgroundQueue.addLast(key);
            tracker.progress.put(key, new PortalProgress(record));
            tracker.lavaResistance.put(key, new PortalResistance(record));
            queueLavaScan(tracker, key);
        } else if (!existing.equals(record)) {
            tracker.portals.put(key, record);
            tracker.progress.put(key, new PortalProgress(record));
            tracker.lavaResistance.put(key, new PortalResistance(record));
            queueLavaScan(tracker, key);
        }

        return tracker.portals.get(key);
    }

    private static @Nullable PortalRecord resolvePortal(
            ServerLevel level,
            BlockPos portalPos
    ) {
        BlockState portalState = level.getBlockState(portalPos);
        if (!portalState.is(Blocks.NETHER_PORTAL)) {
            return null;
        }

        Direction.Axis axis = portalState.getValue(NetherPortalBlock.AXIS);
        if (!portalSearchAreaLoaded(level, portalPos, axis)) {
            return null;
        }

        BlockUtil.FoundRectangle rectangle = BlockUtil.getLargestRectangleAround(
                portalPos,
                axis,
                MAX_PORTAL_SIZE,
                Direction.Axis.Y,
                MAX_PORTAL_SIZE,
                candidate -> level.getBlockState(candidate)
                        .getOptionalValue(NetherPortalBlock.AXIS)
                        .filter(candidateAxis -> candidateAxis == axis)
                        .isPresent()
                        && level.getBlockState(candidate).is(Blocks.NETHER_PORTAL)
        );
        Direction widthDirection = axis == Direction.Axis.X
                ? Direction.EAST
                : Direction.SOUTH;
        BlockPos center = rectangle.minCorner
                .relative(widthDirection, (rectangle.axis1Size - 1) / 2)
                .above((rectangle.axis2Size - 1) / 2);

        int width = rectangle.axis1Size;
        int height = rectangle.axis2Size;
        int outerRadius = outerRadiusForPortal(width, height);
        return new PortalRecord(
                rectangle.minCorner.immutable(),
                center.immutable(),
                axis,
                width,
                height,
                outerRadius,
                outerRadius / 2
        );
    }

    private static boolean portalSearchAreaLoaded(
            ServerLevel level,
            BlockPos pos,
            Direction.Axis axis
    ) {
        int minChunk = axis == Direction.Axis.X
                ? (pos.getX() - MAX_PORTAL_SIZE) >> 4
                : (pos.getZ() - MAX_PORTAL_SIZE) >> 4;
        int maxChunk = axis == Direction.Axis.X
                ? (pos.getX() + MAX_PORTAL_SIZE) >> 4
                : (pos.getZ() + MAX_PORTAL_SIZE) >> 4;
        int fixedChunk = axis == Direction.Axis.X
                ? pos.getZ() >> 4
                : pos.getX() >> 4;

        for (int varyingChunk = minChunk;
             varyingChunk <= maxChunk;
             varyingChunk++) {
            int chunkX = axis == Direction.Axis.X
                    ? varyingChunk
                    : fixedChunk;
            int chunkZ = axis == Direction.Axis.X
                    ? fixedChunk
                    : varyingChunk;
            if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                return false;
            }
        }

        return true;
    }

    private static void removePortal(PortalTracker tracker, long key) {
        tracker.portals.remove(key);
        tracker.pulseWork.remove(key);
        tracker.queuedPulses.remove(key);
        tracker.progress.remove(key);
        tracker.lavaResistance.remove(key);
        tracker.queuedLavaScans.remove(key);
        tracker.lavaScanQueue.removeIf(candidate -> candidate == key);
    }

    private static void queueLavaScan(PortalTracker tracker, long key) {
        if (tracker.queuedLavaScans.add(key)) {
            tracker.lavaScanQueue.addLast(key);
        }
    }

    private static PortalTracker tracker(ServerLevel level) {
        return TRACKERS.computeIfAbsent(
                level.dimension(),
                ignored -> new PortalTracker()
        );
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static final class PortalTracker {
        private final Map<Long, PortalRecord> portals = new HashMap<>();
        private final ArrayDeque<Long> backgroundQueue = new ArrayDeque<>();
        private final Map<Long, Integer> pulseWork = new HashMap<>();
        private final ArrayDeque<Long> pulseQueue = new ArrayDeque<>();
        private final Set<Long> queuedPulses = new HashSet<>();
        private final Set<Long> pendingChunks = new LinkedHashSet<>();
        private final Map<Long, PortalProgress> progress = new HashMap<>();
        private final Map<Long, PortalResistance> lavaResistance =
                new HashMap<>();
        private final ArrayDeque<Long> lavaScanQueue = new ArrayDeque<>();
        private final Set<Long> queuedLavaScans = new HashSet<>();
    }

    private enum DrainPhase {
        DRAIN,
        CORRUPT
    }

    private static final class PortalProgress {
        private final int drainLimit;
        private final int corruptionLimit;
        private int drainCursor;
        private int corruptionCursor;
        private boolean preferCorruption;

        private PortalProgress(PortalRecord portal) {
            drainLimit = countOffsetsWithin(portal.outerRadius());
            corruptionLimit = countOffsetsWithin(portal.innerRadius());
        }

        private void resetIfComplete() {
            if (drainCursor < drainLimit
                    || corruptionCursor < corruptionLimit) {
                return;
            }

            drainCursor = 0;
            corruptionCursor = 0;
            preferCorruption = false;
        }

        private DrainPhase nextPhase() {
            if (drainCursor >= drainLimit) {
                return DrainPhase.CORRUPT;
            }
            if (preferCorruption && hasCandidate(DrainPhase.CORRUPT)) {
                return DrainPhase.CORRUPT;
            }
            return DrainPhase.DRAIN;
        }

        private boolean hasCandidate(DrainPhase phase) {
            if (phase == DrainPhase.DRAIN) {
                return drainCursor < drainLimit;
            }
            if (corruptionCursor >= corruptionLimit
                    || drainCursor == 0) {
                return false;
            }

            int deathFrontDistanceSquared = distanceSquaredAt(
                    drainCursor - 1
            );
            int corruptionDistanceSquared = distanceSquaredAt(
                    corruptionCursor
            );
            return corruptionFitsBehindDeathFront(
                    deathFrontDistanceSquared,
                    corruptionDistanceSquared
            );
        }

        private int nextOffsetIndex(DrainPhase phase) {
            if (phase == DrainPhase.DRAIN) {
                return drainCursor++;
            }
            return corruptionCursor++;
        }

        private void completeTurn(DrainPhase phase) {
            preferCorruption = phase == DrainPhase.DRAIN;
        }
    }

    private static final class PortalResistance {
        private final BlockPos center;
        private final int radius;
        private final int minChunkX;
        private final int maxChunkX;
        private final int minChunkZ;
        private final int maxChunkZ;
        private final LinearSpreadLimiter limiter = new LinearSpreadLimiter();
        private int nextChunkX;
        private int nextChunkZ;
        private int scanCount;
        private int lavaSources;
        private boolean hasSnapshot;

        private PortalResistance(PortalRecord portal) {
            center = portal.center();
            radius = portal.outerRadius();
            minChunkX = (center.getX() - radius) >> 4;
            maxChunkX = (center.getX() + radius) >> 4;
            minChunkZ = (center.getZ() - radius) >> 4;
            maxChunkZ = (center.getZ() + radius) >> 4;
            nextChunkX = minChunkX;
            nextChunkZ = minChunkZ;
        }

        private void scanNextChunk(ServerLevel level) {
            scanCount += countLavaSourcesInChunk(
                    level,
                    center,
                    radius,
                    nextChunkX,
                    nextChunkZ
            );

            nextChunkX++;
            if (nextChunkX <= maxChunkX) {
                return;
            }

            nextChunkX = minChunkX;
            nextChunkZ++;
            if (nextChunkZ <= maxChunkZ) {
                return;
            }

            lavaSources = scanCount;
            hasSnapshot = true;
            scanCount = 0;
            nextChunkZ = minChunkZ;
        }

        private boolean allowsSpread(int portalArea) {
            return hasSnapshot && limiter.allows(portalArea, lavaSources);
        }

        private boolean hasSnapshot() {
            return hasSnapshot;
        }
    }

    private static final class LinearSpreadLimiter {
        private int credit;

        private boolean allows(int portalArea, int lavaSources) {
            int activeShare = portalArea - Math.min(lavaSources, portalArea);
            if (activeShare <= 0) {
                credit = 0;
                return false;
            }

            credit += activeShare;
            if (credit < portalArea) {
                return false;
            }

            credit -= portalArea;
            return true;
        }
    }

    private record PortalRecord(
            BlockPos minCorner,
            BlockPos center,
            Direction.Axis axis,
            int width,
            int height,
            int outerRadius,
            int innerRadius
    ) {
        private int area() {
            return width * height;
        }

        private boolean isActive(ServerLevel level) {
            return level.getChunkSource().getChunkNow(
                    center.getX() >> 4,
                    center.getZ() >> 4
            ) != null
                    && level.getBlockState(center).is(Blocks.NETHER_PORTAL)
                    && level.getBlockState(center).getValue(
                    NetherPortalBlock.AXIS
            ) == axis;
        }

        private void addPortalBlocksTo(Set<Long> positions) {
            Direction widthDirection = axis == Direction.Axis.X
                    ? Direction.EAST
                    : Direction.SOUTH;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    positions.add(
                            minCorner.relative(widthDirection, x)
                                    .above(y)
                                    .asLong()
                    );
                }
            }
        }
    }
}
