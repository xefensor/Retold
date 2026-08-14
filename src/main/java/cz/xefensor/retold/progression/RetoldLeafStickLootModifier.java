package cz.xefensor.retold.progression;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.registry.RetoldTags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Adds a reliable early-game Stick source to all blocks in the vanilla leaves
 * tag while preserving the normal shears and Silk Touch harvest paths.
 */
public final class RetoldLeafStickLootModifier extends LootModifier {
    static final float BASE_CHANCE = 0.20F;
    static final float FORTUNE_CHANCE_PER_LEVEL = 0.05F;
    static final float LIVING_BUSH_CHANCE = 0.10F;

    public static final MapCodec<RetoldLeafStickLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance -> codecStart(instance).apply(
                    instance,
                    RetoldLeafStickLootModifier::new
            ));

    public RetoldLeafStickLootModifier(
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
        BlockState state = context.getOptionalParameter(
                LootContextParams.BLOCK_STATE
        );
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (state == null || tool == null || !canSupplySticks(state)) {
            return generatedLoot;
        }

        var enchantments = context.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT);
        int silkTouchLevel = tool.getEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.SILK_TOUCH)
        );
        if (tool.is(RetoldTags.LEAF_PRESERVING_TOOLS)
                || silkTouchLevel > 0) {
            return generatedLoot;
        }

        if (state.is(Blocks.DEAD_BUSH)) {
            generatedLoot.removeIf(stack -> stack.is(Items.STICK));
            generatedLoot.add(new ItemStack(
                    Items.STICK,
                    2 + context.getRandom().nextInt(3)
            ));
            return generatedLoot;
        }

        int fortuneLevel = tool.getEnchantmentLevel(
                enchantments.getOrThrow(Enchantments.FORTUNE)
        );
        if (state.is(BlockTags.LEAVES)
                && context.getRandom().nextFloat()
                < chanceForFortune(fortuneLevel)) {
            generatedLoot.add(new ItemStack(
                    Items.STICK,
                    1 + context.getRandom().nextInt(2)
            ));
        } else if (state.is(RetoldTags.STICK_DROPPING_LIVING_BUSHES)
                && context.getRandom().nextFloat() < LIVING_BUSH_CHANCE) {
            generatedLoot.add(new ItemStack(Items.STICK));
        }

        return generatedLoot;
    }

    static float chanceForFortune(int fortuneLevel) {
        return Math.min(
                1.0F,
                BASE_CHANCE
                        + Math.max(0, fortuneLevel)
                        * FORTUNE_CHANCE_PER_LEVEL
        );
    }

    private static boolean canSupplySticks(BlockState state) {
        return state.is(BlockTags.LEAVES)
                || state.is(Blocks.DEAD_BUSH)
                || state.is(RetoldTags.STICK_DROPPING_LIVING_BUSHES);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return RetoldLootModifiers.MORE_LEAF_STICKS.get();
    }
}
