package cz.xefensor.retold.worldgen.delayed;

import cz.xefensor.retold.behavior.home.RetoldAnimalHomeData;

import cz.xefensor.retold.Retold;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class RetoldAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Retold.MODID);
    public static final Supplier<AttachmentType<RetoldChunkStructureData>> CHUNK_STRUCTURE_DATA =
            ATTACHMENT_TYPES.register(
                    "chunk_structure_data",
                    () -> AttachmentType
                            .builder(() -> RetoldChunkStructureData.EMPTY)
                            .serialize(RetoldChunkStructureData.CODEC.fieldOf("data"))
                            .build()
            );
    public static final Supplier<AttachmentType<RetoldAnimalHomeData>> ANIMAL_HOME_DATA =
            ATTACHMENT_TYPES.register(
                    "animal_home_data",
                    () -> AttachmentType
                            .builder(() -> RetoldAnimalHomeData.EMPTY)
                            .serialize(
                                    RetoldAnimalHomeData.CODEC.fieldOf("data"),
                                    RetoldAnimalHomeData::shouldSerialize
                            )
                            .build()
            );

    private RetoldAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
