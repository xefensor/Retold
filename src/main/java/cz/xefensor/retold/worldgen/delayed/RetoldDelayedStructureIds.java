package cz.xefensor.retold.worldgen.delayed;

import java.util.List;

public final class RetoldDelayedStructureIds {
    private RetoldDelayedStructureIds() {
    }

    public static final String WOODLAND_MANSION = "minecraft:mansion";
    public static final String PILLAGER_OUTPOST = "minecraft:pillager_outpost";

    public static final List<String> ALL = List.of(
            WOODLAND_MANSION,
            PILLAGER_OUTPOST
    );
}