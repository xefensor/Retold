package cz.xefensor.retold.behavior.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.EventHooks;

public final class RetoldMobGriefing {
    private RetoldMobGriefing() {
    }

    public static boolean canModifyBlocks(
            ServerLevel level,
            Entity actor
    ) {
        if (level == null || actor == null) {
            return false;
        }

        return EventHooks.canEntityGrief(
                level,
                responsibleActor(actor)
        );
    }

    public static boolean isEnabled(ServerLevel level) {
        return level != null
                && level.getGameRules().get(GameRules.MOB_GRIEFING);
    }

    private static Entity responsibleActor(Entity actor) {
        if (actor instanceof Projectile projectile && projectile.getOwner() != null) {
            return projectile.getOwner();
        }

        return actor;
    }
}
