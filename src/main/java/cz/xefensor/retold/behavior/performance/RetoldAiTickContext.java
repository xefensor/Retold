package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldAiTickContext {
    private static final Map<Entity, Context> CONTEXTS = new WeakHashMap<>();

    private RetoldAiTickContext() {
    }

    public static RetoldMobProfile profile(Entity entity) {
        return context(entity).profile;
    }

    public static RetoldMobProfileType profileType(Entity entity) {
        return context(entity).profile.type();
    }

    public static String entityPath(Entity entity) {
        return context(entity).entityPath;
    }

    private static Context context(Entity entity) {
        if (entity == null) {
            return Context.NONE;
        }

        long gameTime = gameTime(entity);
        Context context = CONTEXTS.get(entity);

        if (context != null && context.gameTime == gameTime) {
            return context;
        }

        String entityPath = RetoldMobRules.getEntityTypePath(entity.getType());
        Context refreshed = new Context(
                gameTime,
                entityPath,
                RetoldMobProfiles.get(entityPath)
        );

        CONTEXTS.put(
                entity,
                refreshed
        );
        return refreshed;
    }

    private static long gameTime(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            return level.getGameTime();
        }

        return Long.MIN_VALUE;
    }

    private record Context(
            long gameTime,
            String entityPath,
            RetoldMobProfile profile
    ) {
        private static final Context NONE = new Context(
                Long.MIN_VALUE,
                "",
                RetoldMobProfiles.get("")
        );
    }
}
