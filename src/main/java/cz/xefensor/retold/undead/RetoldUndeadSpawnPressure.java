package cz.xefensor.retold.undead;

import cz.xefensor.retold.registry.RetoldTags;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;

public final class RetoldUndeadSpawnPressure {
    private static final int BONUS_WEIGHT_DIVISOR = 4;

    private RetoldUndeadSpawnPressure() {
    }

    public static void apply(LevelEvent.PotentialSpawns event) {
        if (event.getMobCategory() != MobCategory.MONSTER
                || !(event.getLevel() instanceof ServerLevel level)
                || RetoldWorldData.get(level).getStage()
                != RetoldWorldStage.STAGE_2) {
            return;
        }

        List<Weighted<MobSpawnSettings.SpawnerData>> baseSpawns =
                event.getSpawnerDataList();
        int baseSpawnCount = baseSpawns.size();

        for (int index = 0; index < baseSpawnCount; index++) {
            Weighted<MobSpawnSettings.SpawnerData> entry = baseSpawns.get(index);
            MobSpawnSettings.SpawnerData spawn = entry.value();

            if (!spawn.type()
                    .builtInRegistryHolder()
                    .is(RetoldTags.STAGE_2_UNDEAD_SPAWN_PRESSURE)) {
                continue;
            }

            event.addSpawnerData(new Weighted<>(spawn, bonusWeight(entry.weight())));
        }
    }

    static int bonusWeight(int baseWeight) {
        return Math.max(1, (baseWeight + 2) / BONUS_WEIGHT_DIVISOR);
    }
}
