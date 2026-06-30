package cz.xefensor.retold.stage;

public enum RetoldWorldStage {
    STAGE_1(1),
    STAGE_2(2),
    STAGE_3(3);

    private final int id;

    RetoldWorldStage(int id) {
        this.id = id;
    }

    public static RetoldWorldStage getStageFromId(int id) {
        return switch (id) {
            case 2 -> STAGE_2;
            case 3 -> STAGE_3;
            default -> STAGE_1;
        };
    }

    public int getId() {
        return id;
    }
}