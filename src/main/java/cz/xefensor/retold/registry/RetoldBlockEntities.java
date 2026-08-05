package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.block.AnimalFeederBlockEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Retold.MODID);

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<AnimalFeederBlockEntity>
            > ANIMAL_FEEDER = BLOCK_ENTITIES.register(
            "animal_feeder",
            () -> new BlockEntityType<>(
                    AnimalFeederBlockEntity::new,
                    RetoldBlocks.ANIMAL_FEEDER.get()
            )
    );

    private RetoldBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
