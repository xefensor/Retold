package cz.xefensor.retold.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class RetoldWorldData extends SavedData {
    public static final SavedDataType<RetoldWorldData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "world_data"),
                    RetoldWorldData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT
                                    .fieldOf("stage")
                                    .forGetter(data -> data.stage.getId())
                    ).apply(instance, RetoldWorldData::new))
            );

    private RetoldWorldStage stage = RetoldWorldStage.STAGE_1;

    public RetoldWorldData() {
    }

    private RetoldWorldData(int stageId) {
        this.stage = RetoldWorldStage.getStageFromId(stageId);
    }

    public RetoldWorldStage getStage() {
        return stage;
    }

    public void setStage(RetoldWorldStage stage) {
        if (this.stage != stage) {
            this.stage = stage;
            setDirty();
        }
    }

    public static RetoldWorldData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}