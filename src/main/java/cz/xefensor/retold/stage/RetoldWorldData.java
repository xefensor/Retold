package cz.xefensor.retold.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.concurrent.ThreadLocalRandom;

public class RetoldWorldData extends SavedData {
    public static final SavedDataType<RetoldWorldData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "world_data"),
            RetoldWorldData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("stage").forGetter(data -> data.stage.getId()),
                    Codec.LONG.optionalFieldOf("end_sky_seed", 0L).forGetter(RetoldWorldData::getEndSkySeed)
            ).apply(instance, RetoldWorldData::new))
    );

    private RetoldWorldStage stage = RetoldWorldStage.STAGE_1;
    private long endSkySeed = ThreadLocalRandom.current().nextLong();

    public RetoldWorldData() {
    }

    private RetoldWorldData(int stageId, long endSkySeed) {
        this.stage = RetoldWorldStage.getStageFromId(stageId);

        if (endSkySeed == 0L) {
            this.endSkySeed = ThreadLocalRandom.current().nextLong();
            setDirty();
        } else {
            this.endSkySeed = endSkySeed;
        }
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

    public long getEndSkySeed() {
        return endSkySeed;
    }

    public void randomizeEndSkySeed() {
        this.endSkySeed = ThreadLocalRandom.current().nextLong();
        setDirty();
    }

    public static RetoldWorldData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}