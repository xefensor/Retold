package cz.xefensor.retold.behavior.species;

import com.mojang.serialization.DynamicOps;
import cz.xefensor.retold.Retold;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class RetoldSlimeItemStorage {
    static final int BASE_GROWTH_COST = 16;
    static final int MAX_FOOD_GROWN_SIZE = 10;

    private static final String STORED_ITEMS_KEY = "RetoldSlimeStoredItems";
    private static final String GROWTH_PROGRESS_KEY = "RetoldSlimeGrowthProgress";

    private RetoldSlimeItemStorage() {
    }

    public static boolean swallow(AbstractCubeMob cubeMob, ItemStack stack) {
        if (cubeMob == null || stack == null || stack.isEmpty()) {
            return false;
        }

        Optional<Tag> encoded = ItemStack.CODEC.encodeStart(
                serializationOps(cubeMob),
                stack.copy()
        ).resultOrPartial(message -> Retold.LOGGER.warn(
                "Could not store an item stack swallowed by {}: {}",
                cubeMob.getStringUUID(),
                message
        ));

        if (encoded.isEmpty()) {
            return false;
        }

        ListTag storedItems = cubeMob.getPersistentData()
                .getListOrEmpty(STORED_ITEMS_KEY)
                .copy();
        storedItems.add(encoded.get());
        cubeMob.getPersistentData().put(STORED_ITEMS_KEY, storedItems);

        addGrowthProgress(cubeMob, stack.getCount());
        applyPendingGrowth(cubeMob);
        return true;
    }

    public static List<ItemStack> getStoredItems(AbstractCubeMob cubeMob) {
        if (cubeMob == null) {
            return List.of();
        }

        List<ItemStack> storedItems = new ArrayList<>();
        DynamicOps<Tag> ops = serializationOps(cubeMob);

        for (Tag encoded : cubeMob.getPersistentData().getListOrEmpty(STORED_ITEMS_KEY)) {
            ItemStack.CODEC.parse(ops, encoded)
                    .resultOrPartial(message -> Retold.LOGGER.warn(
                            "Could not read an item stack swallowed by {}: {}",
                            cubeMob.getStringUUID(),
                            message
                    ))
                    .filter(stack -> !stack.isEmpty())
                    .ifPresent(storedItems::add);
        }

        return List.copyOf(storedItems);
    }

    public static int getStoredItemCount(AbstractCubeMob cubeMob) {
        int count = 0;

        for (ItemStack stack : getStoredItems(cubeMob)) {
            count = saturatedAdd(count, stack.getCount());
        }

        return count;
    }

    public static void transfer(AbstractCubeMob source, AbstractCubeMob destination) {
        if (source == null || destination == null || source == destination) {
            return;
        }

        ListTag sourceItems = source.getPersistentData().getListOrEmpty(STORED_ITEMS_KEY);

        if (!sourceItems.isEmpty()) {
            ListTag destinationItems = destination.getPersistentData()
                    .getListOrEmpty(STORED_ITEMS_KEY)
                    .copy();

            for (Tag encoded : sourceItems) {
                destinationItems.add(encoded.copy());
            }

            destination.getPersistentData().put(STORED_ITEMS_KEY, destinationItems);
        }

        int combinedProgress = saturatedAdd(
                growthProgress(destination),
                growthProgress(source)
        );
        destination.getPersistentData().putInt(
                GROWTH_PROGRESS_KEY,
                combinedProgress
        );
        clear(source);
    }

    public static void clearStorage(AbstractCubeMob cubeMob) {
        if (cubeMob != null) {
            clear(cubeMob);
        }
    }

    public static void applyPendingGrowth(AbstractCubeMob cubeMob) {
        if (cubeMob == null) {
            return;
        }

        int progress = growthProgress(cubeMob);

        while (cubeMob.getSize() < MAX_FOOD_GROWN_SIZE) {
            int growthCost = growthCostForSize(cubeMob.getSize());

            if (progress < growthCost) {
                break;
            }

            progress -= growthCost;
            cubeMob.setSize(cubeMob.getSize() + 1, true);
        }

        if (cubeMob.getSize() >= MAX_FOOD_GROWN_SIZE) {
            progress = Math.min(
                    progress,
                    growthCostForSize(MAX_FOOD_GROWN_SIZE - 1) - 1
            );
        }

        cubeMob.getPersistentData().putInt(GROWTH_PROGRESS_KEY, progress);
    }

    static int growthCostForSize(int currentSize) {
        int boundedSize = Math.clamp(
                currentSize,
                1,
                MAX_FOOD_GROWN_SIZE - 1
        );
        return BASE_GROWTH_COST << (boundedSize - 1);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof AbstractCubeMob cubeMob)
                || !(cubeMob.level() instanceof ServerLevel level)) {
            return;
        }

        releaseStoredItems(level, cubeMob, event.getDrops());
    }

    public static void releaseStoredItems(
            ServerLevel level,
            AbstractCubeMob cubeMob,
            Collection<ItemEntity> drops
    ) {
        if (level == null || cubeMob == null || drops == null) {
            return;
        }

        List<ItemStack> storedItems = getStoredItems(cubeMob);
        clear(cubeMob);

        for (ItemStack stack : storedItems) {
            ItemEntity drop = new ItemEntity(
                    level,
                    cubeMob.getX(),
                    cubeMob.getY(),
                    cubeMob.getZ(),
                    stack.copy()
            );
            drop.setDefaultPickUpDelay();
            drops.add(drop);
        }
    }

    private static void addGrowthProgress(AbstractCubeMob cubeMob, int amount) {
        cubeMob.getPersistentData().putInt(
                GROWTH_PROGRESS_KEY,
                saturatedAdd(growthProgress(cubeMob), Math.max(0, amount))
        );
    }

    private static int growthProgress(AbstractCubeMob cubeMob) {
        return Math.max(
                0,
                cubeMob.getPersistentData().getIntOr(GROWTH_PROGRESS_KEY, 0)
        );
    }

    private static int saturatedAdd(int first, int second) {
        long result = (long) first + second;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, result));
    }

    private static void clear(AbstractCubeMob cubeMob) {
        cubeMob.getPersistentData().remove(STORED_ITEMS_KEY);
        cubeMob.getPersistentData().remove(GROWTH_PROGRESS_KEY);
    }

    private static DynamicOps<Tag> serializationOps(AbstractCubeMob cubeMob) {
        return RegistryOps.create(NbtOps.INSTANCE, cubeMob.registryAccess());
    }
}
