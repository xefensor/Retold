package cz.xefensor.retold.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;

public final class RetoldToolProgressionEvents {
    private static final float COPPER_STONE_SPEED_MULTIPLIER = 0.25F;
    private static final float PRE_STEEL_DEEPSLATE_SPEED_MULTIPLIER = 0.25F;
    private static final Identifier CAMPFIRE_RECIPE =
            Identifier.withDefaultNamespace("campfire");
    private static final Set<Identifier> DISABLED_TOOL_RECIPES = Set.of(
            Identifier.withDefaultNamespace("wooden_axe"),
            Identifier.withDefaultNamespace("wooden_hoe"),
            Identifier.withDefaultNamespace("wooden_pickaxe"),
            Identifier.withDefaultNamespace("wooden_shovel"),
            Identifier.withDefaultNamespace("wooden_spear"),
            Identifier.withDefaultNamespace("wooden_sword"),
            Identifier.withDefaultNamespace("stone_axe"),
            Identifier.withDefaultNamespace("stone_hoe"),
            Identifier.withDefaultNamespace("stone_pickaxe"),
            Identifier.withDefaultNamespace("stone_shovel"),
            Identifier.withDefaultNamespace("stone_spear"),
            Identifier.withDefaultNamespace("stone_sword")
    );

    private RetoldToolProgressionEvents() {
    }

    @SubscribeEvent
    public static void onModifyRecipeJsons(ModifyRecipeJsonsEvent event) {
        event.getRecipeJsons().keySet().removeAll(DISABLED_TOOL_RECIPES);

        JsonObject campfireRecipe = event.getRecipeJsons()
                .get(CAMPFIRE_RECIPE)
                .getAsJsonObject();
        JsonObject ingredients = new JsonObject();
        ingredients.addProperty("L", "#minecraft:logs");
        ingredients.addProperty("S", "minecraft:stick");
        campfireRecipe.add("key", ingredients);

        JsonArray pattern = new JsonArray();
        pattern.add(" S ");
        pattern.add("S S");
        pattern.add("LLL");
        campfireRecipe.add("pattern", pattern);
    }

    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        if (!event.getTargetBlock().is(BlockTags.LOGS)) {
            return;
        }

        ItemStack tool = event.getEntity().getMainHandItem();
        event.setCanHarvest(tool.isCorrectToolForDrops(event.getTargetBlock()));
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled()) {
            return;
        }

        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.is(Items.COPPER_PICKAXE)
                && event.getState().is(Blocks.STONE)) {
            event.setNewSpeed(
                    event.getNewSpeed() * COPPER_STONE_SPEED_MULTIPLIER
            );
        } else if ((tool.is(Items.COPPER_PICKAXE)
                || tool.is(Items.IRON_PICKAXE))
                && event.getState().is(RetoldTags.STEEL_TIER_BLOCKS)) {
            event.setNewSpeed(
                    event.getNewSpeed()
                            * PRE_STEEL_DEEPSLATE_SPEED_MULTIPLIER
            );
        }
    }
}
