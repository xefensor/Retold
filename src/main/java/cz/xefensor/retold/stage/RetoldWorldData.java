package cz.xefensor.retold.stage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;

public class RetoldWorldData extends SavedData {
    public static final SavedDataType<RetoldWorldData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "world_data"),
                    RetoldWorldData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.INT
                                    .fieldOf("stage")
                                    .forGetter(data -> data.stage.getId()),
                            Codec.INT
                                    .optionalFieldOf("offered_elements", 0)
                                    .forGetter(data -> data.offeredElementsMask),
                            Codec.BOOL
                                    .optionalFieldOf("water_element_offered", false)
                                    .forGetter(data -> false),
                            BlockPos.CODEC
                                    .optionalFieldOf("dragon_egg_pos")
                                    .forGetter(data -> Optional.ofNullable(data.dragonEggPos))
                    ).apply(instance, RetoldWorldData::new))
            );

    private RetoldWorldStage stage = RetoldWorldStage.STAGE_1;
    private int offeredElementsMask;
    private BlockPos dragonEggPos;

    public RetoldWorldData() {
    }

    private RetoldWorldData(
            int stageId,
            int offeredElementsMask,
            boolean oldWaterElementOffered,
            Optional<BlockPos> dragonEggPos
    ) {
        this.stage = RetoldWorldStage.getStageFromId(stageId);
        this.offeredElementsMask = offeredElementsMask;
        this.dragonEggPos = dragonEggPos.orElse(null);

        if (oldWaterElementOffered) {
            this.offeredElementsMask |= RetoldElementType.WATER.mask();
        }
    }

    public static RetoldWorldData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
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

    public boolean hasElementOffered(RetoldElementType element) {
        return (offeredElementsMask & element.mask()) != 0;
    }

    public boolean offerElement(RetoldElementType element) {
        if (hasElementOffered(element)) {
            return false;
        }

        offeredElementsMask |= element.mask();
        setDirty();
        return true;
    }

    public int offeredElementCount() {
        return Integer.bitCount(offeredElementsMask);
    }

    public BlockPos getDragonEggPos() {
        return dragonEggPos;
    }

    public void setDragonEggPos(BlockPos dragonEggPos) {
        if (!dragonEggPos.equals(this.dragonEggPos)) {
            this.dragonEggPos = dragonEggPos.immutable();
            setDirty();
        }
    }

    public void clearDragonEggPos() {
        if (dragonEggPos != null) {
            dragonEggPos = null;
            setDirty();
        }
    }

    public boolean hasAllElements() {
        for (RetoldElementType element : RetoldElementType.values()) {
            if (!hasElementOffered(element)) {
                return false;
            }
        }

        return true;
    }

    public void clearOfferedElements() {
        if (offeredElementsMask != 0) {
            offeredElementsMask = 0;
            setDirty();
        }
    }
}
