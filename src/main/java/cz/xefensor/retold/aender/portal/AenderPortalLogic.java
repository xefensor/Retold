package cz.xefensor.retold.aender.portal;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderVolatility;
import cz.xefensor.retold.aender.stability.AenderRealityTickEvents;
import cz.xefensor.retold.aender.stability.AenderStabilityData;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class AenderPortalLogic {
    private static final int TARGET_PORTAL_SIZE = 3;
    private static final int SEARCH_RADIUS_TO_AENDER = 128;
    private static final int SEARCH_RADIUS_TO_OVERWORLD = 16;
    private static final int CREATE_RADIUS = 16;

    private static final DustParticleOptions ACTIVATION_PARTICLE =
            new DustParticleOptions(0x7B24AD, 1.0F);

    private AenderPortalLogic() {
    }

    public static void activatePortal(ServerLevel level, AenderPortalShape shape) {
        shape.createPortalBlocks(level);
        AenderPortalData.get(level).register(level, shape);

        BlockPos center = shape.centerBlock();
        level.playSound(null, center, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.1F);
        level.sendParticles(
                ACTIVATION_PARTICLE,
                center.getX() + 0.5D,
                center.getY() + 0.4D,
                center.getZ() + 0.5D,
                48,
                shape.width() * 0.4D,
                0.15D,
                shape.depth() * 0.4D,
                0.04D
        );
    }

    public static @Nullable TeleportTransition getPortalDestination(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalEntryPos
    ) {
        ResourceKey<Level> destinationDimension = destinationDimension(currentLevel);

        if (destinationDimension == null || !isStageUnlocked(currentLevel)) {
            return null;
        }

        ServerLevel destinationLevel = currentLevel.getServer().getLevel(destinationDimension);

        if (destinationLevel == null) {
            Retold.LOGGER.warn("Could not use Aender portal because {} is not loaded", destinationDimension);
            return null;
        }

        BlockPos approximateExit = AenderPortalCoordinates.scaleAndClamp(
                currentLevel.dimension(),
                destinationLevel.getWorldBorder(),
                entity.position()
        );

        AenderPortalShape.findComplete(currentLevel, portalEntryPos)
                .ifPresent(shape -> AenderPortalData.get(currentLevel).register(currentLevel, shape));

        boolean enteringAender = destinationLevel.dimension() == RetoldAenderDimensions.AENDER;

        if (!enteringAender) {
            destinationLevel.getChunk(approximateExit.getX() >> 4, approximateExit.getZ() >> 4);
        } else if (!AenderPortalWarmup.isArrivalReady(entity)) {
            /*
             * Players using this portal wait for the asynchronous warm-up. This
             * bounded fallback remains for commands, other portal integrations,
             * and non-player entities that bypass the charging state machine.
             */
            AenderRealityTickEvents.prepareArrivalCore(destinationLevel, approximateExit);
        }

        AenderPortalShape exitPortal = findClosestPortal(destinationLevel, approximateExit)
                .orElseGet(() -> createExitPortal(destinationLevel, approximateExit));

        if (enteringAender) {
            AenderPortalWarmup.finish(entity);
        }

        return createTransition(currentLevel, destinationLevel, entity, portalEntryPos, exitPortal);
    }

    static @Nullable AenderWarmupTarget getAenderWarmupTarget(ServerLevel currentLevel, Entity entity) {
        if (currentLevel.dimension() != Level.OVERWORLD || !isStageUnlocked(currentLevel)) {
            return null;
        }

        ServerLevel aender = currentLevel.getServer().getLevel(RetoldAenderDimensions.AENDER);

        if (aender == null) {
            return null;
        }

        BlockPos approximateExit = AenderPortalCoordinates.scaleAndClamp(
                currentLevel.dimension(),
                aender.getWorldBorder(),
                entity.position()
        );

        AenderPortalShape portalCandidate = AenderPortalData.get(aender)
                .findNear(aender, approximateExit, SEARCH_RADIUS_TO_AENDER)
                .stream()
                .findFirst()
                .orElse(null);
        BlockPos preparationCenter = portalCandidate == null
                ? approximateExit
                : portalCandidate.centerBlock();

        return new AenderWarmupTarget(aender, preparationCenter, portalCandidate);
    }

    record AenderWarmupTarget(
            ServerLevel level,
            BlockPos center,
            @Nullable AenderPortalShape portalCandidate
    ) {
    }

    private static @Nullable ResourceKey<Level> destinationDimension(ServerLevel currentLevel) {
        if (currentLevel.dimension() == Level.OVERWORLD) {
            return RetoldAenderDimensions.AENDER;
        }

        if (currentLevel.dimension() == RetoldAenderDimensions.AENDER) {
            return Level.OVERWORLD;
        }

        return null;
    }

    private static boolean isStageUnlocked(ServerLevel level) {
        return RetoldWorldData.get(level).getStage().getId() >= RetoldWorldStage.STAGE_3.getId();
    }

    private static Optional<AenderPortalShape> findClosestPortal(ServerLevel level, BlockPos approximateExit) {
        int horizontalRadius = level.dimension() == RetoldAenderDimensions.AENDER
                ? SEARCH_RADIUS_TO_AENDER
                : SEARCH_RADIUS_TO_OVERWORLD;

        AenderPortalData portalData = AenderPortalData.get(level);

        for (AenderPortalShape candidate : portalData.findNear(level, approximateExit, horizontalRadius)) {
            if (!isPortalAreaLoaded(level, candidate)) {
                continue;
            }

            Optional<AenderPortalShape> found = AenderPortalShape.findComplete(level, candidate.centerBlock());

            if (found.isPresent()) {
                return found;
            }

            portalData.remove(level, candidate);
        }

        return findLoadedUnindexedPortal(level, approximateExit, horizontalRadius);
    }

    private static Optional<AenderPortalShape> findLoadedUnindexedPortal(
            ServerLevel level,
            BlockPos approximateExit,
            int horizontalRadius
    ) {
        int centerChunkX = approximateExit.getX() >> 4;
        int centerChunkZ = approximateExit.getZ() >> 4;
        int chunkRadius = Mth.ceil(horizontalRadius / 16.0D);
        double maxDistanceSqr = (double) horizontalRadius * horizontalRadius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkAccess chunk = level.getChunkSource().getChunkNow(centerChunkX + dx, centerChunkZ + dz);

                if (chunk == null) {
                    continue;
                }

                if (!isAenderChunkCurrent(level, chunk)) {
                    continue;
                }

                for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                    if (!chunk.getSections()[sectionIndex].maybeHas(state -> state.is(RetoldBlocks.AENDER_PORTAL))) {
                        continue;
                    }

                    int sectionMinY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;

                    for (int localX = 0; localX < 16; localX++) {
                        for (int localY = 0; localY < 16; localY++) {
                            for (int localZ = 0; localZ < 16; localZ++) {
                                pos.set(
                                        chunk.getPos().getMinBlockX() + localX,
                                        sectionMinY + localY,
                                        chunk.getPos().getMinBlockZ() + localZ
                                );

                                if (!chunk.getBlockState(pos).is(RetoldBlocks.AENDER_PORTAL)
                                        || horizontalDistanceSqr(pos, approximateExit) > maxDistanceSqr) {
                                    continue;
                                }

                                Optional<AenderPortalShape> found = AenderPortalShape.findComplete(level, pos);

                                if (found.isPresent()) {
                                    AenderPortalData.get(level).register(level, found.get());
                                    return found;
                                }
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean isPortalAreaLoaded(ServerLevel level, AenderPortalShape shape) {
        int minChunkX = Math.floorDiv(shape.minCorner().getX() - 1, 16);
        int maxChunkX = Math.floorDiv(shape.minCorner().getX() + shape.width(), 16);
        int minChunkZ = Math.floorDiv(shape.minCorner().getZ() - 1, 16);
        int maxChunkZ = Math.floorDiv(shape.minCorner().getZ() + shape.depth(), 16);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isAenderChunkCurrent(ServerLevel level, ChunkAccess chunk) {
        if (level.dimension() != RetoldAenderDimensions.AENDER) {
            return true;
        }

        if (AenderStabilityData.get(level).isStable(chunk.getPos())) {
            return true;
        }

        AenderVolatility.retainForChunk(chunk);

        if (AenderVolatility.needsRegeneration(chunk)) {
            AenderRealityTickEvents.enqueueIfNeeded(level, chunk);
            return false;
        }

        return true;
    }

    static AenderPortalShape createExitPortal(ServerLevel level, BlockPos approximateExit) {
        BlockPos origin = findExitOrigin(level, approximateExit);
        AenderPortalShape shape = new AenderPortalShape(origin, TARGET_PORTAL_SIZE, TARGET_PORTAL_SIZE);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState frame = RetoldBlocks.DEV_AENDER_PORTAL_FRAME.get().defaultBlockState();
        BlockState support = level.dimension() == RetoldAenderDimensions.AENDER
                ? RetoldBlocks.AENDER_STONE.get().defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();

        for (int x = -1; x <= TARGET_PORTAL_SIZE; x++) {
            for (int z = -1; z <= TARGET_PORTAL_SIZE; z++) {
                pos.set(origin).move(x, -1, z);

                if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                    level.setBlock(pos, support, 18);
                }

                for (int dy = 0; dy <= 3; dy++) {
                    pos.set(origin).move(x, dy, z);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }

        for (int x = -1; x <= TARGET_PORTAL_SIZE; x++) {
            for (int z = -1; z <= TARGET_PORTAL_SIZE; z++) {
                if (x == -1 || x == TARGET_PORTAL_SIZE || z == -1 || z == TARGET_PORTAL_SIZE) {
                    pos.set(origin).move(x, 0, z);
                    level.setBlock(pos, frame, 18);
                }
            }
        }

        shape.createPortalBlocks(level);
        AenderPortalData.get(level).register(level, shape);
        return shape;
    }

    private static BlockPos findExitOrigin(ServerLevel level, BlockPos approximateExit) {
        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();
        boolean destinationIsAender = level.dimension() == RetoldAenderDimensions.AENDER;

        for (BlockPos.MutableBlockPos column : BlockPos.spiralAround(
                approximateExit,
                CREATE_RADIUS,
                Direction.EAST,
                Direction.SOUTH
        )) {
            int surfaceY = level.getHeight(
                    Heightmap.Types.MOTION_BLOCKING,
                    column.getX(),
                    column.getZ()
            );

            if (!AenderPortalPlacement.canUseSurface(destinationIsAender, surfaceY, level.getMinY())) {
                continue;
            }

            int y = preferredPortalY(level, surfaceY);
            candidate.set(column.getX() - 1, y, column.getZ() - 1);

            if (canCreatePortal(level, candidate)) {
                return candidate.immutable();
            }
        }

        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                approximateExit.getX(),
                approximateExit.getZ()
        );
        int fallbackY = AenderPortalPlacement.fallbackY(
                destinationIsAender,
                surfaceY,
                approximateExit.getY(),
                level.getMinY() + 8,
                Math.min(level.getMaxY() - 6, level.getMinY() + level.getLogicalHeight() - 6)
        );

        WorldBorder border = level.getWorldBorder();
        int minOriginX = Mth.ceil(border.getMinX()) + 1;
        int maxOriginX = Mth.floor(border.getMaxX() - 1.0E-5D) - TARGET_PORTAL_SIZE;
        int minOriginZ = Mth.ceil(border.getMinZ()) + 1;
        int maxOriginZ = Mth.floor(border.getMaxZ() - 1.0E-5D) - TARGET_PORTAL_SIZE;
        int fallbackX = minOriginX <= maxOriginX
                ? Mth.clamp(approximateExit.getX() - 1, minOriginX, maxOriginX)
                : approximateExit.getX() - 1;
        int fallbackZ = minOriginZ <= maxOriginZ
                ? Mth.clamp(approximateExit.getZ() - 1, minOriginZ, maxOriginZ)
                : approximateExit.getZ() - 1;

        return new BlockPos(fallbackX, fallbackY, fallbackZ);
    }

    private static int preferredPortalY(ServerLevel level, int surfaceY) {
        int minY = level.getMinY() + 4;
        int maxY = Math.min(level.getMaxY() - 6, level.getMinY() + level.getLogicalHeight() - 6);
        return Mth.clamp(surfaceY, minY, maxY);
    }

    private static boolean canCreatePortal(ServerLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = -1; x <= TARGET_PORTAL_SIZE; x++) {
            for (int z = -1; z <= TARGET_PORTAL_SIZE; z++) {
                pos.set(origin).move(x, -1, z);
                BlockState supportState = level.getBlockState(pos);

                if (!supportState.isFaceSturdy(level, pos, Direction.UP) && !supportState.canBeReplaced()) {
                    return false;
                }

                for (int y = 0; y <= 3; y++) {
                    pos.set(origin).move(x, y, z);

                    if (!level.getWorldBorder().isWithinBounds(pos)) {
                        return false;
                    }

                    BlockState state = level.getBlockState(pos);

                    if (!state.canBeReplaced() && !state.is(RetoldBlocks.AENDER_PORTAL)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private static TeleportTransition createTransition(
            ServerLevel currentLevel,
            ServerLevel destinationLevel,
            Entity entity,
            BlockPos portalEntryPos,
            AenderPortalShape exitShape
    ) {
        AenderPortalShape sourceShape = AenderPortalShape.findComplete(currentLevel, portalEntryPos)
                .orElse(new AenderPortalShape(portalEntryPos, 1, 1));

        Vec3 offset = relativePositionInShape(sourceShape, entity.position());
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 target = new Vec3(
                exitShape.minCorner().getX()
                        + dimensions.width() / 2.0D
                        + (exitShape.width() - dimensions.width()) * offset.x(),
                exitShape.minCorner().getY() + 1.0D,
                exitShape.minCorner().getZ()
                        + dimensions.width() / 2.0D
                        + (exitShape.depth() - dimensions.width()) * offset.z()
        );

        Vec3 collisionFree = PortalShape.findCollisionFreePosition(
                target,
                destinationLevel,
                entity,
                dimensions
        );
        BlockPos ticketPos = exitShape.centerBlock();

        return new TeleportTransition(
                destinationLevel,
                collisionFree,
                Vec3.ZERO,
                entity.getYRot(),
                entity.getXRot(),
                TeleportTransition.PLAY_PORTAL_SOUND.then(moved -> moved.placePortalTicket(ticketPos))
        );
    }

    private static Vec3 relativePositionInShape(AenderPortalShape shape, Vec3 position) {
        double xRange = Math.max(1.0D, shape.width() - 1.0D);
        double zRange = Math.max(1.0D, shape.depth() - 1.0D);
        double x = Mth.clamp(
                (position.x() - (shape.minCorner().getX() + 0.5D)) / xRange,
                0.0D,
                1.0D
        );
        double z = Mth.clamp(
                (position.z() - (shape.minCorner().getZ() + 0.5D)) / zRange,
                0.0D,
                1.0D
        );
        return new Vec3(x, 0.0D, z);
    }

    private static double horizontalDistanceSqr(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }
}
