package cz.xefensor.retold.territory;

import java.util.EnumSet;
import java.util.Set;

public enum RetoldTerritoryFlowState {
    INACTIVE,
    OBSERVING,
    WARNING,
    FINAL_WARNING,
    ATTACKING,
    COOLDOWN;

    public static RetoldTerritoryFlowState fromWarningLevel(RetoldWarningLevel warningLevel) {
        return switch (warningLevel) {
            case NONE, NOTICED -> OBSERVING;
            case WARNING -> WARNING;
            case FINAL_WARNING, ATTACK -> FINAL_WARNING;
        };
    }

    public boolean isWarningState() {
        return this == OBSERVING || this == WARNING || this == FINAL_WARNING;
    }

    public boolean continuesOutsideTerritory() {
        return this == ATTACKING;
    }

    public boolean canTransitionTo(RetoldTerritoryFlowState nextState) {
        if (this == nextState) {
            return true;
        }

        return allowedTransitions().contains(nextState);
    }

    private Set<RetoldTerritoryFlowState> allowedTransitions() {
        return switch (this) {
            case INACTIVE -> EnumSet.of(OBSERVING, WARNING, FINAL_WARNING, ATTACKING);
            case OBSERVING -> EnumSet.of(INACTIVE, WARNING, FINAL_WARNING, ATTACKING);
            case WARNING -> EnumSet.of(INACTIVE, OBSERVING, FINAL_WARNING, ATTACKING);
            case FINAL_WARNING -> EnumSet.of(INACTIVE, OBSERVING, WARNING, ATTACKING);
            case ATTACKING -> EnumSet.of(INACTIVE, COOLDOWN);
            case COOLDOWN -> EnumSet.of(INACTIVE, ATTACKING);
        };
    }
}
