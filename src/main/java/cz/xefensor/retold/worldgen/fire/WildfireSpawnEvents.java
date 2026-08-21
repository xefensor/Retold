package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Owns Stage 2 Wildfire encounters independently of biome monster weights and
 * the ordinary monster population cap.
 */
public final class WildfireSpawnEvents {
    static final int SPAWN_ATTEMPT_INTERVAL_TICKS = 600;
    private static final int SPAWN_POSITION_ATTEMPTS = 16;
    private static final int MIN_SPAWN_DISTANCE_BLOCKS = 48;
    private static final int MAX_PLAYER_DISTANCE_BLOCKS = 96;
    private static final int PLAYER_SAFETY_DISTANCE_BLOCKS = 24;
    private static final int MAX_DESPAWN_DISTANCE_BLOCKS = 128;
    private static final int PLAYER_VERTICAL_RANGE_BLOCKS = 48;
    private static final int DOWNWARD_FLOOR_SEARCH_BLOCKS = 32;

    private WildfireSpawnEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !isDedicatedSpawningEnabled(level)
                || level.getGameTime() % SPAWN_ATTEMPT_INTERVAL_TICKS != 0) {
            return;
        }

        List<ServerPlayer> eligiblePlayers = level.getPlayers(
                player -> player.isAlive() && !player.isSpectator()
        );

        if (eligiblePlayers.isEmpty()) {
            return;
        }

        ServerPlayer player = eligiblePlayers.get(
                level.getRandom().nextInt(eligiblePlayers.size())
        );
        trySpawnNearPlayer(level, player);
    }

    static boolean isDedicatedSpawningEnabled(ServerLevel level) {
        return level.dimension() == Level.NETHER
                && level.getDifficulty() != Difficulty.PEACEFUL
                && level.getGameRules().get(GameRules.SPAWN_MOBS)
                && RetoldWorldData.get(level).getStage().getId()
                >= RetoldWorldStage.STAGE_2.getId();
    }

    static boolean trySpawnNearPlayer(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();

        for (int attempt = 0; attempt < SPAWN_POSITION_ATTEMPTS; attempt++) {
            BlockPos candidate = findSpawnPosition(level, player, random);

            if (candidate != null && spawnAt(level, candidate) != null) {
                return true;
            }
        }

        return false;
    }

    static @Nullable Wildfire spawnAt(ServerLevel level, BlockPos pos) {
        var type = RetoldEntityTypes.WILDFIRE.get();

        if (!isDedicatedSpawningEnabled(level)
                || !SpawnPlacements.isSpawnPositionOk(type, level, pos)
                || !SpawnPlacements.checkSpawnRules(
                        type,
                        level,
                        EntitySpawnReason.NATURAL,
                        pos,
                        level.getRandom()
                )
                || !level.noCollision(type.getSpawnAABB(
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D
                ))) {
            return null;
        }

        Wildfire wildfire = type.create(level, EntitySpawnReason.NATURAL);

        if (wildfire == null) {
            return null;
        }

        wildfire.snapTo(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                level.getRandom().nextFloat() * 360.0F,
                0.0F
        );

        if (!EventHooks.checkSpawnPosition(
                wildfire,
                level,
                EntitySpawnReason.NATURAL
        )) {
            return null;
        }

        wildfire.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(pos),
                EntitySpawnReason.NATURAL,
                null
        );
        level.addFreshEntityWithPassengers(wildfire);
        return wildfire;
    }

    private static BlockPos findSpawnPosition(
            ServerLevel level,
            ServerPlayer player,
            RandomSource random
    ) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        int distance = Mth.randomBetweenInclusive(
                random,
                MIN_SPAWN_DISTANCE_BLOCKS,
                MAX_PLAYER_DISTANCE_BLOCKS
        );
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        int playerY = Mth.floor(player.getY());
        int minY = Math.max(
                level.getMinY() + 1,
                playerY - PLAYER_VERTICAL_RANGE_BLOCKS
        );
        int maxY = Math.min(
                level.getMaxY() - 3,
                playerY + PLAYER_VERTICAL_RANGE_BLOCKS
        );

        if (minY > maxY
                || !level.isPositionEntityTicking(
                        new BlockPos(x, Mth.clamp(playerY, minY, maxY), z)
                )) {
            return null;
        }

        int startY = Mth.randomBetweenInclusive(random, minY, maxY);
        int lowestY = Math.max(minY, startY - DOWNWARD_FLOOR_SEARCH_BLOCKS);

        for (int y = startY; y >= lowestY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            Vec3 center = Vec3.atBottomCenterOf(candidate);

            if (player.distanceToSqr(center)
                    > MAX_DESPAWN_DISTANCE_BLOCKS * MAX_DESPAWN_DISTANCE_BLOCKS
                    || level.getNearestPlayer(
                            center.x,
                            center.y,
                            center.z,
                            PLAYER_SAFETY_DISTANCE_BLOCKS,
                            false
                    ) != null) {
                continue;
            }

            if (SpawnPlacements.isSpawnPositionOk(
                    RetoldEntityTypes.WILDFIRE.get(),
                    level,
                    candidate
            )) {
                return candidate;
            }
        }

        return null;
    }
}
