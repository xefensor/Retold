package cz.xefensor.retold.progression;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.Retold;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class RetoldLootModifiers {
    private static final DeferredRegister<
            MapCodec<? extends IGlobalLootModifier>
            > SERIALIZERS = DeferredRegister.create(
                    NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    Retold.MODID
            );

    public static final DeferredHolder<
            MapCodec<? extends IGlobalLootModifier>,
            MapCodec<RetoldLeafStickLootModifier>
            > MORE_LEAF_STICKS = SERIALIZERS.register(
                    "more_leaf_sticks",
                    () -> RetoldLeafStickLootModifier.CODEC
            );

    private RetoldLootModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        SERIALIZERS.register(modEventBus);
    }
}
