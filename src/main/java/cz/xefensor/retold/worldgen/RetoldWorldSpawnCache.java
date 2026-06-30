package cz.xefensor.retold.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class RetoldWorldSpawnCache {
    private static volatile BlockPos overworldSpawn = BlockPos.ZERO;
    private static volatile boolean initialized = false;

    private RetoldWorldSpawnCache() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        initialized = false;
        overworldSpawn = BlockPos.ZERO;

        update(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        update(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 == 0) {
            update(event.getServer());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static BlockPos getOverworldSpawn() {
        return overworldSpawn;
    }

    private static void update(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);

        if (overworld == null) {
            return;
        }

        var spawnData = overworld.getLevelData().getRespawnData();

        if (spawnData.dimension() != Level.OVERWORLD) {
            return;
        }

        overworldSpawn = spawnData.pos();
        initialized = true;
    }
}