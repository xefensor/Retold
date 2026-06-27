package cz.xefensor.retold.worldgen;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldWorldgenRegistries {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Retold.MODID);

    public static final DeferredHolder<
            MapCodec<? extends DensityFunction>,
            MapCodec<RetoldCentralEndIslandMaskDensityFunction>
            > CENTRAL_END_ISLAND_MASK =
            DENSITY_FUNCTION_TYPES.register(
                    "central_end_island_mask",
                    () -> RetoldCentralEndIslandMaskDensityFunction.CODEC
            );

    private RetoldWorldgenRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
    }
}