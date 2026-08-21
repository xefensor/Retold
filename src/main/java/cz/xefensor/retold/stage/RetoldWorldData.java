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
import java.util.Set;

public class RetoldWorldData extends SavedData {
    /*
     * Keep the hatch threshold on the four implemented acquisition paths until
     * Fire and Earth make the full six-offering ritual survival-obtainable.
     */
    private static final Set<RetoldRitualOffering> CURRENT_REQUIRED_EGG_OFFERINGS = Set.of(
            RetoldRitualOffering.WATER,
            RetoldRitualOffering.AIR,
            RetoldRitualOffering.LIFE,
            RetoldRitualOffering.DEATH
    );
    private static final int CURRENT_REQUIRED_EGG_OFFERING_MASK = CURRENT_REQUIRED_EGG_OFFERINGS
            .stream()
            .mapToInt(RetoldRitualOffering::mask)
            .reduce(0, (left, right) -> left | right);

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
                                    // Retain the old field name for saved-world compatibility.
                                    .forGetter(data -> data.offeredOfferingsMask),
                            Codec.BOOL
                                    .optionalFieldOf("water_element_offered", false)
                                    .forGetter(data -> false),
                            BlockPos.CODEC
                                    .optionalFieldOf("dragon_egg_pos")
                                    .forGetter(data -> Optional.ofNullable(data.dragonEggPos))
                    ).apply(instance, RetoldWorldData::new))
            );

    private RetoldWorldStage stage = RetoldWorldStage.STAGE_1;
    private int offeredOfferingsMask;
    private BlockPos dragonEggPos;

    public RetoldWorldData() {
    }

    private RetoldWorldData(
            int stageId,
            int offeredOfferingsMask,
            boolean oldWaterElementOffered,
            Optional<BlockPos> dragonEggPos
    ) {
        this.stage = RetoldWorldStage.getStageFromId(stageId);
        this.offeredOfferingsMask = offeredOfferingsMask;
        this.dragonEggPos = dragonEggPos.orElse(null);

        if (oldWaterElementOffered) {
            this.offeredOfferingsMask |= RetoldRitualOffering.WATER.mask();
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

    public boolean hasOffering(RetoldRitualOffering offering) {
        return (offeredOfferingsMask & offering.mask()) != 0;
    }

    public boolean offer(RetoldRitualOffering offering) {
        if (hasOffering(offering)) {
            return false;
        }

        offeredOfferingsMask |= offering.mask();
        setDirty();
        return true;
    }

    public int offeredOfferingCount() {
        return Integer.bitCount(offeredOfferingsMask);
    }

    public int offeredRequiredOfferingCount() {
        return Integer.bitCount(
                offeredOfferingsMask & CURRENT_REQUIRED_EGG_OFFERING_MASK
        );
    }

    public int requiredOfferingCount() {
        return CURRENT_REQUIRED_EGG_OFFERINGS.size();
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

    public boolean hasAllRequiredOfferings() {
        return (offeredOfferingsMask & CURRENT_REQUIRED_EGG_OFFERING_MASK)
                == CURRENT_REQUIRED_EGG_OFFERING_MASK;
    }

    public void clearOfferings() {
        if (offeredOfferingsMask != 0) {
            offeredOfferingsMask = 0;
            setDirty();
        }
    }
}
