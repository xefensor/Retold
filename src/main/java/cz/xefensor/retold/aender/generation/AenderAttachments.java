package cz.xefensor.retold.aender.generation;

import cz.xefensor.retold.Retold;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class AenderAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Retold.MODID);

    public static final Supplier<AttachmentType<AenderChunkRealityData>> CHUNK_REALITY =
            ATTACHMENT_TYPES.register(
                    "aender_chunk_reality",
                    () -> AttachmentType
                            .builder(() -> AenderChunkRealityData.STALE)
                            .serialize(AenderChunkRealityData.CODEC.fieldOf("data"))
                            .build()
            );

    private AenderAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
