package cz.xefensor.retold.stage;

import net.minecraft.server.level.ServerLevel;

public final class RetoldRaidProgression {
    private RetoldRaidProgression() {
    }

    public static boolean canStartRaid(ServerLevel level) {
        return canStartRaid(RetoldWorldData.get(level).getStage());
    }

    public static boolean canStartRaid(RetoldWorldStage stage) {
        return stage.getId() >= RetoldWorldStage.STAGE_3.getId();
    }
}
