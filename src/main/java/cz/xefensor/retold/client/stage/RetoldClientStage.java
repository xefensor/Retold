package cz.xefensor.retold.client.stage;

import cz.xefensor.retold.stage.RetoldWorldStage;

public final class RetoldClientStage {
    private static RetoldWorldStage stage = RetoldWorldStage.STAGE_1;

    private RetoldClientStage() {
    }

    public static RetoldWorldStage getStage() {
        return stage;
    }

    public static void setStage(RetoldWorldStage newStage) {
        stage = newStage;
    }

    public static boolean shouldUseGreenEndermanEyes() {
        return stage == RetoldWorldStage.STAGE_2
                || stage == RetoldWorldStage.STAGE_3;
    }
}