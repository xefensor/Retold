package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.world.entity.Entity;

public final class RetoldAiTickContext {
    private RetoldAiTickContext() {
    }

    /*
     * RetoldMobProfiles owns the reload-safe entity-type index. Keeping this
     * facade allocation-free preserves existing callers without per-mob state.
     */
    public static RetoldMobProfile profile(Entity entity) {
        return RetoldMobProfiles.get(entity);
    }

    public static RetoldMobProfileType profileType(Entity entity) {
        return profile(entity).type();
    }

    public static String entityPath(Entity entity) {
        if (entity == null) {
            return "";
        }

        return RetoldMobRules.getEntityTypePath(entity.getType());
    }
}
