package cz.xefensor.retold.stage;

public final class RetoldStageRuntime {
    private static volatile RetoldWorldStage overworldStage = RetoldWorldStage.STAGE_1;

    private RetoldStageRuntime() {
    }

    public static RetoldWorldStage getOverworldStage() {
        return overworldStage;
    }

    public static void setOverworldStage(RetoldWorldStage stage) {
        overworldStage = stage;
    }

    public static boolean isAtLeast(RetoldWorldStage stage) {
        return overworldStage.getId() >= stage.getId();
    }
}