package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AirTempleWindEvents {
    private static final Identifier AIR_TEMPLE_ID =
            Identifier.fromNamespaceAndPath(Retold.MODID, "air_temple");
    private static final int PARTICLE_INTERVAL = 2;
    private static final int SOURCE_REFRESH_INTERVAL_TICKS = 20;
    private static final int PLAYER_SCAN_CHUNK_RADIUS = 8;
    private static final int GENERATED_SOURCE_PLAYER_RADIUS_BLOCKS = 192;
    private static final int ISLAND_TOP_OFFSET = 24;
    private static final int DYNAMIC_GUST_SLOTS = 16;
    private static final int BASE_GUST_WINDOW_TICKS = 220;
    private static final int GUST_WINDOW_STAGGER_TICKS = 37;
    private static final Map<Long, WindSource> GENERATED_SOURCES = new HashMap<>();
    private static final List<WindSource> CACHED_SOURCES = new ArrayList<>();
    private static long lastSourceRefreshTick = -SOURCE_REFRESH_INTERVAL_TICKS;

    private AirTempleWindEvents() {
    }

    public static void rememberGeneratedTemple(
            int centerX,
            int centerZ,
            int islandY
    ) {
        GENERATED_SOURCES.put(
                sourceKey(centerX, centerZ),
                new WindSource(centerX, centerZ, islandY)
        );
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
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

        if (CACHED_SOURCES.isEmpty()
                || gameTime - lastSourceRefreshTick >= SOURCE_REFRESH_INTERVAL_TICKS) {
            refreshCachedSources(overworld, airTemple);
            lastSourceRefreshTick = gameTime;
        }

        boolean emitParticles = gameTime % PARTICLE_INTERVAL == 0;

        for (WindSource source : CACHED_SOURCES) {
            applyWindForTemple(overworld, source, gameTime, emitParticles);
        }
    }

    private static void refreshCachedSources(
            ServerLevel level,
            Structure airTemple
    ) {
        Map<Long, WindSource> sources = new HashMap<>();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            ChunkPos playerChunk = player.chunkPosition();

            for (int dx = -PLAYER_SCAN_CHUNK_RADIUS; dx <= PLAYER_SCAN_CHUNK_RADIUS; dx++) {
                for (int dz = -PLAYER_SCAN_CHUNK_RADIUS; dz <= PLAYER_SCAN_CHUNK_RADIUS; dz++) {
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

                        WindSource source = sourceFromStart(start);
                        sources.put(sourceKey(source.centerX(), source.centerZ()), source);
                    }
                }
            }
        }

        for (WindSource source : GENERATED_SOURCES.values()) {
            if (isNearAnyPlayer(level, source)) {
                sources.put(sourceKey(source.centerX(), source.centerZ()), source);
            }
        }

        CACHED_SOURCES.clear();
        CACHED_SOURCES.addAll(sources.values());
    }

    private static WindSource sourceFromStart(StructureStart start) {
        BoundingBox bounds = start.getBoundingBox();
        int centerX = bounds.getCenter().getX();
        int centerZ = bounds.getCenter().getZ();
        int islandY = bounds.maxY() - ISLAND_TOP_OFFSET;

        return new WindSource(centerX, centerZ, islandY);
    }

    private static boolean isNearAnyPlayer(
            ServerLevel level,
            WindSource source
    ) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }

            double dx = player.getX() - source.centerX();
            double dz = player.getZ() - source.centerZ();

            if (dx * dx + dz * dz
                    <= GENERATED_SOURCE_PLAYER_RADIUS_BLOCKS * GENERATED_SOURCE_PLAYER_RADIUS_BLOCKS) {
                return true;
            }
        }

        return false;
    }

    private static void applyWindForTemple(
            ServerLevel level,
            WindSource source,
            long gameTime,
            boolean emitParticles
    ) {
        int centerX = source.centerX();
        int centerZ = source.centerZ();
        int islandY = source.islandY();

        for (AirTempleWindZone zone : createZones(centerX, centerZ, islandY, gameTime)) {
            List<Entity> entities = level.getEntities(
                    (Entity) null,
                    zone.bounds(),
                    AirTempleWindEvents::isWindAffected
            );

            for (Entity entity : entities) {
                zone.apply(level, entity);
            }

            if (emitParticles) {
                zone.emitParticles(level, level.getRandom());
            }
        }
    }

    private static List<AirTempleWindZone> createZones(
            int centerX,
            int centerZ,
            int islandY,
            long gameTime
    ) {
        List<AirTempleWindZone> zones = new ArrayList<>();
        long templeSeed = sourceKey(centerX, centerZ);

        for (int slot = 0; slot < DYNAMIC_GUST_SLOTS; slot++) {
            AirTempleWindZone zone = createDynamicZone(
                    centerX,
                    centerZ,
                    islandY,
                    gameTime,
                    templeSeed,
                    slot
            );

            if (zone != null) {
                zones.add(zone);
            }
        }

        if (zones.isEmpty()) {
            zones.add(createFallbackZone(centerX, centerZ, islandY, gameTime, templeSeed));
        }

        return zones;
    }

    private static AirTempleWindZone createDynamicZone(
            int centerX,
            int centerZ,
            int islandY,
            long gameTime,
            long templeSeed,
            int slot
    ) {
        int windowTicks = BASE_GUST_WINDOW_TICKS + (slot % 5) * 35;
        long window = (gameTime + (long) slot * GUST_WINDOW_STAGGER_TICKS) / windowTicks;
        long seed = mix64(templeSeed
                ^ window * 341873128712L
                ^ slot * 132897987541L);

        if (unit(seed + 1L) < 0.22D) {
            return null;
        }

        double centerOffsetX = range(seed + 2L, -112.0D, 112.0D);
        double centerOffsetZ = range(seed + 3L, -112.0D, 112.0D);
        double centerOffsetY = range(seed + 4L, -4.0D, 2.0D);
        double halfX = range(seed + 5L, 18.0D, 38.0D);
        double halfZ = range(seed + 6L, 18.0D, 38.0D);
        double height = range(seed + 7L, 10.0D, 16.0D);
        double x = centerX + centerOffsetX;
        double y = islandY + centerOffsetY;
        double z = centerZ + centerOffsetZ;
        double angle = range(seed + 8L, 0.0D, Math.PI * 2.0D)
                + Math.sin(gameTime / range(seed + 9L, 130.0D, 210.0D) + slot) * 0.25D;
        double strength = range(seed + 14L, 0.105D, 0.19D);
        Vec3 impulse = new Vec3(
                Math.cos(angle) * strength,
                0.0D,
                Math.sin(angle) * strength
        );
        double maxSpeed = range(seed + 16L, 0.9D, 1.45D);

        return new AirTempleWindZone(
                new AABB(x - halfX, y, z - halfZ, x + halfX, y + height, z + halfZ),
                impulse,
                maxSpeed,
                true,
                windowTicks,
                (int) range(seed + 17L, 0.0D, windowTicks),
                range(seed + 18L, 0.04D, 0.3D)
        );
    }

    private static AirTempleWindZone createFallbackZone(
            int centerX,
            int centerZ,
            int islandY,
            long gameTime,
            long templeSeed
    ) {
        long seed = mix64(templeSeed ^ gameTime / BASE_GUST_WINDOW_TICKS);
        double angle = range(seed + 1L, 0.0D, Math.PI * 2.0D);
        double x = centerX + range(seed + 2L, -72.0D, 72.0D);
        double z = centerZ + range(seed + 3L, -72.0D, 72.0D);

        return new AirTempleWindZone(
                new AABB(x - 24.0D, islandY - 3.0D, z - 24.0D, x + 24.0D, islandY + 13.0D, z + 24.0D),
                new Vec3(Math.cos(angle) * 0.12D, 0.0D, Math.sin(angle) * 0.12D),
                1.0D,
                true,
                BASE_GUST_WINDOW_TICKS,
                0,
                0.2D
        );
    }

    private static boolean isWindAffected(Entity entity) {
        return !entity.isRemoved()
                && entity.isAlive()
                && !entity.isSpectator()
                && !entity.noPhysics;
    }

    private static long sourceKey(int centerX, int centerZ) {
        return ChunkPos.pack(centerX >> 4, centerZ >> 4);
    }

    private static double range(long seed, double min, double max) {
        return min + unit(seed) * (max - min);
    }

    private static double unit(long seed) {
        return (mix64(seed) >>> 11) * 0x1.0p-53;
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private record WindSource(int centerX, int centerZ, int islandY) {
    }
}
