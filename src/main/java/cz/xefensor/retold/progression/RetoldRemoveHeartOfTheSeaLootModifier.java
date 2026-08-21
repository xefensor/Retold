package cz.xefensor.retold.progression;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Keeps the Heart of the Sea tied to Retold's Elder Guardian path. The loot
 * table restriction lives in the modifier data so this operation stays
 * narrowly scoped to buried treasure.
 */
public final class RetoldRemoveHeartOfTheSeaLootModifier extends LootModifier {
    public static final MapCodec<RetoldRemoveHeartOfTheSeaLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(
                    instance,
                    RetoldRemoveHeartOfTheSeaLootModifier::new
            ));

    public RetoldRemoveHeartOfTheSeaLootModifier(
            LootItemCondition[] conditions,
            int priority
    ) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        generatedLoot.removeIf(stack -> stack.is(Items.HEART_OF_THE_SEA));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return RetoldLootModifiers.REMOVE_HEART_OF_THE_SEA.get();
    }
}
