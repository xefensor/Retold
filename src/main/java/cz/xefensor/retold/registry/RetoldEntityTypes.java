package cz.xefensor.retold.registry;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.aender.entity.AenderEye;
import cz.xefensor.retold.worldgen.air.GaleCore;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
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

    public static final DeferredHolder<EntityType<?>, EntityType<GaleCore>> GALE_CORE =
            ENTITY_TYPES.registerEntityType(
                    "gale_core",
                    GaleCore::new,
                    MobCategory.MONSTER,
                    builder -> builder
                            .sized(1.35F, 3.98F)
                            .clientTrackingRange(10)
                            .updateInterval(3)
            );

    public static final DeferredHolder<EntityType<?>, EntityType<Boat>> AENDER_BOAT =
            ENTITY_TYPES.registerEntityType(
                    "aender_boat",
                    (type, level) -> new Boat(type, level, () -> RetoldAenderWood.AENDER_BOAT_ITEM.get()),
                    MobCategory.MISC,
                    builder -> builder
                            .noLootTable()
                            .sized(1.375F, 0.5625F)
                            .eyeHeight(0.5625F)
                            .clientTrackingRange(10)
            );

    public static final DeferredHolder<EntityType<?>, EntityType<ChestBoat>> AENDER_CHEST_BOAT =
            ENTITY_TYPES.registerEntityType(
                    "aender_chest_boat",
                    (type, level) -> new ChestBoat(
                            type,
                            level,
                            () -> RetoldAenderWood.AENDER_CHEST_BOAT_ITEM.get()
                    ),
                    MobCategory.MISC,
                    builder -> builder
                            .noLootTable()
                            .sized(1.375F, 0.5625F)
                            .eyeHeight(0.5625F)
                            .clientTrackingRange(10)
            );

    private RetoldEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
