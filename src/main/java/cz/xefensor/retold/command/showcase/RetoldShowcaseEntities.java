package cz.xefensor.retold.command.showcase;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

/** Server-owned, persistent exhibits for the developer content gallery. */
public final class RetoldShowcaseEntities {
    public static final String EXHIBIT_TAG = "retold.showcase";
    private static final int DISABLED_ARMOR_SLOTS = 0x7FFFFF;

    private RetoldShowcaseEntities() {
    }

    public static Entity spawn(
            ServerLevel level,
            EntityType<?> type,
            Vec3 position,
            String groupTag
    ) {
        // Deliberately skip finalizeSpawn: exhibits must not create boss escorts or encounters.
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);
        if (entity == null) {
            throw new IllegalStateException("Cannot create showcase entity " + BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }

        prepare(entity, position, groupTag);
        entity.setCustomName(Component.literal(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()));
        add(level, entity);
        return entity;
    }

    public static ArmorStand spawnSteelArmor(
            ServerLevel level,
            Vec3 position,
            boolean baby,
            String groupTag
    ) {
        ArmorStand stand = new ArmorStand(level, position.x, position.y, position.z);
        CompoundTag settings = new CompoundTag();
        settings.putBoolean("Small", baby);
        settings.putBoolean("ShowArms", true);
        settings.putInt("DisabledSlots", DISABLED_ARMOR_SLOTS);
        // The vanilla Small setter is private; its persisted flag also drives isBaby/armor rendering.
        stand.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), settings));
        prepare(stand, position, groupTag);
        stand.setCustomName(Component.literal(baby ? "Steel armor (baby)" : "Steel armor (adult)"));
        stand.setItemSlot(EquipmentSlot.HEAD, RetoldBlocks.STEEL_HELMET.get().getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.CHEST, RetoldBlocks.STEEL_CHESTPLATE.get().getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.LEGS, RetoldBlocks.STEEL_LEGGINGS.get().getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.FEET, RetoldBlocks.STEEL_BOOTS.get().getDefaultInstance());
        stand.setItemSlot(EquipmentSlot.MAINHAND, RetoldBlocks.STEEL_SWORD.get().getDefaultInstance());
        add(level, stand);
        return stand;
    }

    private static void prepare(Entity entity, Vec3 position, String groupTag) {
        entity.snapTo(position, 180.0F, 0.0F);
        entity.setNoGravity(true);
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setCustomNameVisible(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.addTag(EXHIBIT_TAG);
        entity.addTag(groupTag);

        if (entity instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
            living.setYBodyRot(180.0F);
            living.setYHeadRot(180.0F);
        }
        if (entity instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
        }
    }

    private static void add(ServerLevel level, Entity entity) {
        if (!level.addFreshEntity(entity)) {
            throw new IllegalStateException("Cannot add showcase entity " + entity.getType());
        }
    }
}
