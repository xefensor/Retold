package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class RetoldDelayedStructureRetrogen {
    private static final Queue<ChunkPos> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private static final int CHUNKS_PER_TICK = 2;

    private static final int PLAYER_SCAN_CHUNK_RADIUS = 12;
    private static final int PLAYER_SCAN_INTERVAL_TICKS = 40;

    private static int ticksUntilPlayerScan = 0;

    private RetoldDelayedStructureRetrogen() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (!data.hasAnyDeferredStructures()) {
            return;
        }

        enqueue(chunk.getPos());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            return;
        }

        ticksUntilPlayerScan--;

        if (ticksUntilPlayerScan <= 0) {
            ticksUntilPlayerScan = PLAYER_SCAN_INTERVAL_TICKS;
            enqueueDeferredChunksAroundPlayers(overworld);
        }

        for (int i = 0; i < CHUNKS_PER_TICK && !QUEUE.isEmpty(); i++) {
            ChunkPos pos = QUEUE.poll();
            QUEUED.remove(pos.getWorldPosition().asLong());

            processChunk(overworld, pos);
        }
    }

    private static void enqueue(ChunkPos pos) {
        long key = pos.getWorldPosition().asLong();

        if (QUEUED.add(key)) {
            QUEUE.add(pos);
        }
    }

    private static void processChunk(ServerLevel level, ChunkPos pos) {
        if (!level.hasChunk(pos.x(), pos.z())) {
            return;
        }

        ChunkAccess chunk = level.getChunk(pos.x(), pos.z());

        RetoldChunkStructureData data =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        if (!data.hasAnyDeferredStructures()) {
            return;
        }

        RetoldChunkStructureData newData = data;

        if (data.isEditedByPlayer()) {
            for (String structureId : data.deferredStructures()) {
                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);

                Retold.LOGGER.info(
                        "Skipped deferred structure {} at chunk [{}, {}] because chunk is edited",
                        structureId,
                        pos.x(),
                        pos.z()
                );
            }

            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
            return;
        }

        for (String structureId : data.deferredStructures()) {
            if (newData.hasChecked(structureId)) {
                newData = newData.withoutDeferred(structureId);
                continue;
            }

            RetrogenResult result = tryRetrogenStructure(level, pos, structureId);

            if (result == RetrogenResult.SUCCESS || result == RetrogenResult.PERMANENT_SKIP) {
                newData = newData.withChecked(structureId);
                newData = newData.withoutDeferred(structureId);
            }
        }

        if (newData != data) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);
        }
    }

    private static RetrogenResult tryRetrogenStructure(
            ServerLevel level,
            ChunkPos pos,
            String structureId
    ) {
        if (RetoldDelayedStructureIds.WOODLAND_MANSION.equals(structureId)) {
            Retold.LOGGER.info(
                    "Woodland mansion retrogen is still deferred at chunk [{}, {}]",
                    pos.x(),
                    pos.z()
            );

            return RetrogenResult.TRY_LATER;
        }

        if (RetoldDelayedStructureIds.PILLAGER_OUTPOST.equals(structureId)) {
            return tryPlacePillagerOutpost(level, pos);
        }

        Retold.LOGGER.warn(
                "Unknown deferred structure {} at chunk [{}, {}], skipping permanently",
                structureId,
                pos.x(),
                pos.z()
        );

        return RetrogenResult.PERMANENT_SKIP;
    }

    private static RetrogenResult tryPlacePillagerOutpost(
            ServerLevel level,
            ChunkPos pos
    ) {
        RetrogenResult safety = checkChunkAreaSafe(level, pos, 2);

        if (safety != RetrogenResult.SUCCESS) {
            return safety;
        }

        BlockPos placePos = getSurfacePositionForChunk(level, pos);

        boolean placed = runPlaceStructureCommand(
                level,
                RetoldDelayedStructureIds.PILLAGER_OUTPOST,
                placePos
        );

        if (!placed) {
            Retold.LOGGER.warn(
                    "Failed to place pillager outpost at chunk [{}, {}], position {}",
                    pos.x(),
                    pos.z(),
                    placePos
            );

            return RetrogenResult.TRY_LATER;
        }

        Retold.LOGGER.info(
                "Placed deferred pillager outpost at chunk [{}, {}], position {}",
                pos.x(),
                pos.z(),
                placePos
        );

        return RetrogenResult.SUCCESS;
    }

    private static RetrogenResult checkChunkAreaSafe(
            ServerLevel level,
            ChunkPos center,
            int radius
    ) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int chunkX = center.x() + dx;
                int chunkZ = center.z() + dz;

                if (!level.hasChunk(chunkX, chunkZ)) {
                    return RetrogenResult.TRY_LATER;
                }

                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

                RetoldChunkStructureData data =
                        chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

                if (data.isEditedByPlayer()) {
                    Retold.LOGGER.info(
                            "Skipping deferred structure near chunk [{}, {}] because chunk [{}, {}] is edited",
                            center.x(),
                            center.z(),
                            chunkX,
                            chunkZ
                    );

                    return RetrogenResult.PERMANENT_SKIP;
                }
            }
        }

        return RetrogenResult.SUCCESS;
    }

    private static BlockPos getSurfacePositionForChunk(
            ServerLevel level,
            ChunkPos pos
    ) {
        int x = pos.x() * 16 + 8;
        int z = pos.z() * 16 + 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);

        return new BlockPos(x, y, z);
    }

    private static boolean runPlaceStructureCommand(
            ServerLevel level,
            String structureId,
            BlockPos pos
    ) {
        MinecraftServer server = level.getServer();

        CommandSourceStack source = server
                .createCommandSourceStack()
                .withLevel(level)
                .withPosition(Vec3.atCenterOf(pos))
                .withSuppressedOutput();

        String command = "place structure "
                + structureId
                + " "
                + pos.getX()
                + " "
                + pos.getY()
                + " "
                + pos.getZ();

        try {
            ParseResults<CommandSourceStack> parsed =
                    server.getCommands().getDispatcher().parse(command, source);

            server.getCommands().performCommand(parsed, command);

            return true;
        } catch (Exception exception) {
            Retold.LOGGER.error(
                    "Exception while running delayed structure command: {}",
                    command,
                    exception
            );

            return false;
        }
    }

    public static void enqueueDeferredChunksAroundPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            ChunkPos playerChunk = player.chunkPosition();

            for (int dx = -PLAYER_SCAN_CHUNK_RADIUS; dx <= PLAYER_SCAN_CHUNK_RADIUS; dx++) {
                for (int dz = -PLAYER_SCAN_CHUNK_RADIUS; dz <= PLAYER_SCAN_CHUNK_RADIUS; dz++) {
                    int chunkX = playerChunk.x() + dx;
                    int chunkZ = playerChunk.z() + dz;

                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    ChunkAccess chunk = level.getChunk(chunkX, chunkZ);

                    RetoldChunkStructureData data =
                            chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

                    if (!data.hasAnyDeferredStructures()) {
                        continue;
                    }

                    enqueue(chunk.getPos());
                }
            }
        }
    }

    private enum RetrogenResult {
        SUCCESS,
        PERMANENT_SKIP,
        TRY_LATER
    }
}