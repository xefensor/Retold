package cz.xefensor.retold.aender;

import cz.xefensor.retold.Retold;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class RetoldAenderAttachments {
    private RetoldAenderAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    Retold.MODID
            );

    public static final Supplier<AttachmentType<RetoldAenderChunkData>> CHUNK_DATA =
            ATTACHMENT_TYPES.register(
                    "aender_chunk_data",
                    () -> AttachmentType
                            .builder(() -> RetoldAenderChunkData.EMPTY)
                            .serialize(RetoldAenderChunkData.CODEC.fieldOf("data"))
                            .build()
            );

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}