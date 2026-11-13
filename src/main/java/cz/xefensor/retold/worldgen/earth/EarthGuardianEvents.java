package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

public final class EarthGuardianEvents {
    private static final int SOURCE_SCAN_INTERVAL_TICKS = 40;
    private static final int SOURCE_SCAN_CHUNK_RADIUS = 4;

    private EarthGuardianEvents() {
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null
                || overworld.getGameTime() % SOURCE_SCAN_INTERVAL_TICKS != 0L) {
            return;
        }

        Structure junglePyramid = overworld.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getValue(BuiltinStructures.JUNGLE_TEMPLE);

        if (junglePyramid == null) {
            return;
        }

        Map<Long, EarthLabyrinthSource> sources = new HashMap<>();

        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) {
                continue;
            }

            collectLoadedSources(overworld, junglePyramid, player.chunkPosition(), sources);
        }

        sources.values().forEach(source -> EarthGuardianSpawner.spawnIfNeeded(overworld, source));
    }

    private static void collectLoadedSources(
            ServerLevel level,
            Structure junglePyramid,
            ChunkPos playerChunk,
            Map<Long, EarthLabyrinthSource> sources
    ) {
        for (int dx = -SOURCE_SCAN_CHUNK_RADIUS; dx <= SOURCE_SCAN_CHUNK_RADIUS; dx++) {
            for (int dz = -SOURCE_SCAN_CHUNK_RADIUS; dz <= SOURCE_SCAN_CHUNK_RADIUS; dz++) {
                int chunkX = playerChunk.x() + dx;
                int chunkZ = playerChunk.z() + dz;

                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                for (StructureStart start : level.structureManager().startsForStructure(
                        new ChunkPos(chunkX, chunkZ),
                        structure -> structure == junglePyramid
                )) {
                    if (!start.isValid()) {
                        continue;
                    }

                    for (StructurePiece piece : start.getPieces()) {
                        if (piece instanceof EarthLabyrinthPiece labyrinth) {
                            EarthLabyrinthSource source = labyrinth.guardianSource();
                            sources.put(source.key(), source);
                        }
                    }
                }
            }
        }
    }
}
