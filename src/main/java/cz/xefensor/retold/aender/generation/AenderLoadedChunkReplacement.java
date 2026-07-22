package cz.xefensor.retold.aender.generation;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Replaces terrain in already-loaded chunks and reconciles live-world state around that replacement.
 */
public final class AenderLoadedChunkReplacement {
    private AenderLoadedChunkReplacement() {
    }

    public static void regenerate(ServerLevel level, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        List<Entity> entities = entitiesInChunk(level, chunkPos);
        AenderChunkGenerator.regenerateLoadedChunk(chunk);
        reconcileEntitiesAfterRegeneration(level, chunkPos, entities);
        resend(level, chunk);
    }

    public static void blankForProgressiveRegeneration(ServerLevel level, ChunkAccess chunk) {
        discardEntitiesInChunk(level, chunk.getPos());
        AenderChunkSectionEditor.clear(chunk);
        AenderChunkSectionEditor.primeFreshHeightmaps(chunk);
        AenderVolatility.forgetGeneratedMark(chunk);
    }

    private static List<Entity> entitiesInChunk(ServerLevel level, ChunkPos chunkPos) {
        AABB chunkBounds = new AABB(
                chunkPos.getMinBlockX(),
                level.getMinY(),
                chunkPos.getMinBlockZ(),
                chunkPos.getMaxBlockX() + 1,
                level.getMaxY() + 1,
                chunkPos.getMaxBlockZ() + 1
        );

        return level.getEntities(
                (Entity) null,
                chunkBounds,
                entity -> !(entity instanceof Player)
                        && (entity.blockPosition().getX() >> 4) == chunkPos.x()
                        && (entity.blockPosition().getZ() >> 4) == chunkPos.z()
        );
    }

    private static void discardEntitiesInChunk(ServerLevel level, ChunkPos chunkPos) {
        for (Entity entity : entitiesInChunk(level, chunkPos)) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    private static void reconcileEntitiesAfterRegeneration(
            ServerLevel level,
            ChunkPos chunkPos,
            List<Entity> entities
    ) {
        for (Entity entity : entities) {
            if (entity.isRemoved() || entity instanceof Player) {
                continue;
            }

            if (isEntityValidAfterRegeneration(level, entity)) {
                continue;
            }

            if (shouldTryPreserveRegeneratedEntity(entity)
                    && moveEntityToRegeneratedSafeSpot(level, chunkPos, entity)) {
                continue;
            }

            entity.discard();
        }
    }

    private static boolean isEntityValidAfterRegeneration(ServerLevel level, Entity entity) {
        return entity.getY() >= AenderIslandSampler.MIN_Y
                && entity.getY() < AenderIslandSampler.MAX_Y
                && level.noCollision(entity, entity.getBoundingBox());
    }

    private static boolean shouldTryPreserveRegeneratedEntity(Entity entity) {
        if (entity.hasCustomName() || entity.isVehicle() || entity.isPassenger()) {
            return true;
        }

        return entity instanceof Mob mob && mob.isPersistenceRequired();
    }

    private static boolean moveEntityToRegeneratedSafeSpot(ServerLevel level, ChunkPos chunkPos, Entity entity) {
        BlockPos origin = entity.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int radius = 0; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;

                    if (!isInsideChunk(chunkPos, x, z)) {
                        continue;
                    }

                    int surfaceY = AenderIslandSampler.highestBlockYAt(x, z);

                    if (surfaceY < AenderIslandSampler.MIN_Y) {
                        continue;
                    }

                    int minY = Math.max(AenderIslandSampler.MIN_Y, surfaceY + 1);
                    int maxY = Math.min(AenderIslandSampler.MAX_Y - 1, surfaceY + 8);

                    for (int y = minY; y <= maxY; y++) {
                        pos.set(x, y, z);

                        if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) {
                            continue;
                        }

                        entity.snapTo(
                                x + 0.5D,
                                y,
                                z + 0.5D,
                                entity.getYRot(),
                                entity.getXRot()
                        );

                        if (isEntityValidAfterRegeneration(level, entity)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isInsideChunk(ChunkPos chunkPos, int x, int z) {
        return x >= chunkPos.getMinBlockX()
                && x <= chunkPos.getMaxBlockX()
                && z >= chunkPos.getMinBlockZ()
                && z <= chunkPos.getMaxBlockZ();
    }

    private static void resend(ServerLevel level, ChunkAccess chunk) {
        if (!(chunk instanceof LevelChunk levelChunk)) {
            return;
        }

        levelChunk.markUnsaved();

        ClientboundLevelChunkWithLightPacket packet =
                new ClientboundLevelChunkWithLightPacket(levelChunk, level.getLightEngine(), null, null);

        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false)) {
            player.connection.send(packet);
        }
    }
}
