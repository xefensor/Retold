package cz.xefensor.retold.worldgen.air.wind;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.worldgen.air.AirTempleBreezeSpawner;
import cz.xefensor.retold.worldgen.air.GaleCore;
import cz.xefensor.retold.worldgen.air.GaleCoreSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AirTempleWindEvents {
    private static final Identifier AIR_TEMPLE_ID = Identifier.fromNamespaceAndPath(Retold.MODID, "air_temple");
    private static final int PARTICLE_INTERVAL_TICKS = 2;
    private static final int SOURCE_REFRESH_INTERVAL_TICKS = 40;
    private static final int BREEZE_SPAWN_INTERVAL_TICKS = 100;
    private static final int BOSS_SPAWN_INTERVAL_TICKS = 100;
    private static final int BOSS_SPAWN_RELOAD_GRACE_TICKS = 200;
    private static final int SOURCE_SCAN_CHUNK_RADIUS = 8;
    private static final Map<Long, AirTempleWindSource> GENERATED_SOURCES = new ConcurrentHashMap<>();
    private static final Map<Long, AirTempleWindSource> LOADED_SOURCES = new ConcurrentHashMap<>();
    private static final Set<Long> BREEZE_SPAWNED_SOURCES = ConcurrentHashMap.newKeySet();
    private static final Set<Long> BOSS_SPAWNED_SOURCES = ConcurrentHashMap.newKeySet();
    private static long lastSourceRefreshTick = -SOURCE_REFRESH_INTERVAL_TICKS;
    private static long bossSpawnAllowedAfterGameTime = Long.MIN_VALUE;

    private AirTempleWindEvents() {
    }

    public static void rememberGeneratedTemple(AirTempleWindSource source) {
        GENERATED_SOURCES.put(source.key(), source);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        GENERATED_SOURCES.clear();
        LOADED_SOURCES.clear();
        BREEZE_SPAWNED_SOURCES.clear();
        BOSS_SPAWNED_SOURCES.clear();
        lastSourceRefreshTick = -SOURCE_REFRESH_INTERVAL_TICKS;
        bossSpawnAllowedAfterGameTime = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        Structure airTemple = overworld
                .registryAccess()
                .lookupOrThrow(Registries.STRUCTURE)
                .getValue(AIR_TEMPLE_ID);

        if (airTemple == null) {
            return;
        }

        long gameTime = overworld.getGameTime();
        boolean emitParticles = gameTime % PARTICLE_INTERVAL_TICKS == 0;
        AirTempleWindData windData = AirTempleWindData.get(overworld);
        Map<Long, AirTempleWindSource> activeSources = new HashMap<>();

        if (bossSpawnAllowedAfterGameTime == Long.MIN_VALUE) {
            bossSpawnAllowedAfterGameTime = gameTime + BOSS_SPAWN_RELOAD_GRACE_TICKS;
        }

        if (gameTime - lastSourceRefreshTick >= SOURCE_REFRESH_INTERVAL_TICKS) {
            refreshLoadedSources(overworld, airTemple);
            lastSourceRefreshTick = gameTime;
        }

        if (!GENERATED_SOURCES.isEmpty()) {
            windData.rememberAll(GENERATED_SOURCES.values());
        }

        if (!LOADED_SOURCES.isEmpty()) {
            windData.rememberAll(LOADED_SOURCES.values());
        }

        activeSources.putAll(windData.sources());
        activeSources.putAll(GENERATED_SOURCES);
        activeSources.putAll(LOADED_SOURCES);

        for (AirTempleWindSource source : activeSources.values()) {
            if (gameTime % BREEZE_SPAWN_INTERVAL_TICKS == 0
                    && !BREEZE_SPAWNED_SOURCES.contains(source.key())
                    && AirTempleBreezeSpawner.spawnIfNeeded(overworld, source)) {
                BREEZE_SPAWNED_SOURCES.add(source.key());
            }

            if (gameTime % BOSS_SPAWN_INTERVAL_TICKS == 0
                    && gameTime >= bossSpawnAllowedAfterGameTime
                    && !BOSS_SPAWNED_SOURCES.contains(source.key())
                    && GaleCoreSpawner.spawnIfNeeded(overworld, source)) {
                BOSS_SPAWNED_SOURCES.add(source.key());
            }

            AirTempleWindZone zone = new AirTempleWindZone(source);
            List<Entity> affectedEntities = overworld.getEntities(
                    (Entity) null,
                    source.bounds(),
                    AirTempleWindEvents::isWindAffected
            );

            for (Entity entity : affectedEntities) {
                zone.apply(overworld, entity);
            }

            if (emitParticles) {
                for (ServerPlayer player : overworld.players()) {
                    if (source.bounds().intersects(player.getBoundingBox())) {
                        zone.emitParticles(overworld, player, overworld.getRandom());
                    }
                }
            }
        }
    }

    private static void refreshLoadedSources(ServerLevel level, Structure airTemple) {
        Map<Long, AirTempleWindSource> discovered = new HashMap<>();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            ChunkPos playerChunk = player.chunkPosition();

            for (int dx = -SOURCE_SCAN_CHUNK_RADIUS; dx <= SOURCE_SCAN_CHUNK_RADIUS; dx++) {
                for (int dz = -SOURCE_SCAN_CHUNK_RADIUS; dz <= SOURCE_SCAN_CHUNK_RADIUS; dz++) {
                    int chunkX = playerChunk.x() + dx;
                    int chunkZ = playerChunk.z() + dz;

                    if (!level.hasChunk(chunkX, chunkZ)) {
                        continue;
                    }

                    for (StructureStart start : level.structureManager().startsForStructure(
                            new ChunkPos(chunkX, chunkZ),
                            structure -> structure == airTemple
                    )) {
                        if (!start.isValid()) {
                            continue;
                        }

                        AirTempleWindSource source = AirTempleWindSource.fromStructureBounds(start.getBoundingBox());
                        discovered.put(source.key(), source);
                    }
                }
            }
        }

        LOADED_SOURCES.clear();
        LOADED_SOURCES.putAll(discovered);
    }

    private static boolean isWindAffected(Entity entity) {
        return !entity.isRemoved()
                && entity.isAlive()
                && !entity.isSpectator()
                && !entity.noPhysics
                && !(entity instanceof Breeze)
                && !(entity instanceof GaleCore)
                && (!(entity instanceof ServerPlayer player) || !player.isCreative());
    }
}
