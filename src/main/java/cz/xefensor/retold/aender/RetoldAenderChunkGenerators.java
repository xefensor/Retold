package cz.xefensor.retold.aender;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class RetoldAenderChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Retold.MODID);

    public static final Supplier<MapCodec<? extends ChunkGenerator>> AENDER =
            CHUNK_GENERATORS.register(
                    "aender",
                    () -> RetoldAenderChunkGenerator.CODEC
            );

    private RetoldAenderChunkGenerators() {
    }

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}