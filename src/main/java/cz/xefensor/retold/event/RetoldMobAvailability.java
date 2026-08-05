package cz.xefensor.retold.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import org.jspecify.annotations.Nullable;

public final class RetoldMobAvailability {
    private RetoldMobAvailability() {
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getEntity() instanceof Endermite
                && !allowsEndermiteSpawn(event.getSpawnType())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    public static boolean allowsEndermiteSpawn(EntitySpawnReason spawnReason) {
        return switch (spawnReason) {
            case COMMAND, SPAWN_ITEM_USE, DISPENSER, LOAD, DIMENSION_TRAVEL -> true;
            default -> false;
        };
    }

    /**
     * Filters the vanilla Ender Pearl creation call, which does not pass through
     * NeoForge's normal mob spawn-position event.
     */
    public static @Nullable Entity createEndermiteIfAvailable(
            EntityType<?> entityType,
            Level level,
            EntitySpawnReason spawnReason
    ) {
        if (entityType == EntityTypes.ENDERMITE
                && !allowsEndermiteSpawn(spawnReason)) {
            return null;
        }

        return entityType.create(level, spawnReason);
    }
}
