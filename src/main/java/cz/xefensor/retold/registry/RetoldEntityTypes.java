package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.entity.AenderEye;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldEntityTypes {
    private static final DeferredRegister.Entities ENTITY_TYPES =
            DeferredRegister.createEntities(Retold.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AenderEye>> AENDER_EYE =
            ENTITY_TYPES.registerEntityType(
                    "aender_eye",
                    AenderEye::new,
                    MobCategory.AMBIENT,
                    builder -> builder
                            .sized(0.7F, 0.7F)
                            .clientTrackingRange(8)
                            .updateInterval(3)
            );

    private RetoldEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
