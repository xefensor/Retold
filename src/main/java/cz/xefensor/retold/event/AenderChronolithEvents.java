package cz.xefensor.retold.event;

import cz.xefensor.retold.chronolith.ChronolithController;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class AenderChronolithEvents {
    private AenderChronolithEvents() {
    }

    public static void toggle(ServerLevel level, BlockPos pos, ServerPlayer player) {
        ChronolithController.toggle(level, pos, player);
    }

    public static void stopAt(ServerLevel level, BlockPos pos) {
        ChronolithController.stopAt(level, pos);
    }

    public static void syncToPlayer(ServerPlayer player) {
        ChronolithController.syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ChronolithController.tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ChronolithController.stopForPlayerLogout(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ChronolithController.stopAll(event.getServer());
    }
}
