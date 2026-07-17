package cz.xefensor.retold.stage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RetoldWorldStageTest {
    @Test
    void mapsKnownIds() {
        assertEquals(RetoldWorldStage.STAGE_1, RetoldWorldStage.getStageFromId(1));
        assertEquals(RetoldWorldStage.STAGE_2, RetoldWorldStage.getStageFromId(2));
        assertEquals(RetoldWorldStage.STAGE_3, RetoldWorldStage.getStageFromId(3));
    }

    @Test
    void fallsBackToStageOneForUnknownIds() {
        assertEquals(RetoldWorldStage.STAGE_1, RetoldWorldStage.getStageFromId(-1));
        assertEquals(RetoldWorldStage.STAGE_1, RetoldWorldStage.getStageFromId(0));
        assertEquals(RetoldWorldStage.STAGE_1, RetoldWorldStage.getStageFromId(4));
    }
}
