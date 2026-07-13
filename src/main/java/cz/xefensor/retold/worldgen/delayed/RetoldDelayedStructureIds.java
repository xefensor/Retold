package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.stage.RetoldWorldStage;

import java.util.List;

public final class RetoldDelayedStructureIds {
    public static final String AIR_TEMPLE = "retold:air_temple";
    public static final String WOODLAND_MANSION = "minecraft:mansion";
    public static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";
    public static final List<String> ALL = List.of(
            AIR_TEMPLE,
            WOODLAND_MANSION,
            PILLAGER_OUTPOST
    );

    private RetoldDelayedStructureIds() {
    }

    public static RetoldWorldStage requiredStage(String structureId) {
        if (PILLAGER_OUTPOST.equals(structureId)) {
            return RetoldWorldStage.STAGE_3;
        }

        return RetoldWorldStage.STAGE_2;
    }
}
