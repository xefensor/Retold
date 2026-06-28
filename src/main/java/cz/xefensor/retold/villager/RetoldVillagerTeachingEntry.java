package cz.xefensor.retold.villager;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record RetoldVillagerTeachingEntry(
        Identifier profession,
        int defaultEmeraldCost,
        List<TeachableRecipe> recipes
) {
    public static final Codec<RetoldVillagerTeachingEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Identifier.CODEC.fieldOf("profession").forGetter(RetoldVillagerTeachingEntry::profession),
                    Codec.INT.optionalFieldOf("default_emerald_cost", 1).forGetter(RetoldVillagerTeachingEntry::defaultEmeraldCost),
                    TeachableRecipe.CODEC.listOf().fieldOf("recipes").forGetter(RetoldVillagerTeachingEntry::recipes)
            ).apply(instance, RetoldVillagerTeachingEntry::new)
    );

    public int emeraldCostFor(Identifier recipeId) {
        for (TeachableRecipe recipe : recipes) {
            if (!recipe.id().equals(recipeId)) {
                continue;
            }

            if (recipe.emeraldCost() >= 0) {
                return recipe.emeraldCost();
            }

            return defaultEmeraldCost;
        }

        return -1;
    }

    public record TeachableRecipe(
            Identifier id,
            int emeraldCost
    ) {
        public static final Codec<TeachableRecipe> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Identifier.CODEC.fieldOf("id").forGetter(TeachableRecipe::id),
                        Codec.INT.optionalFieldOf("emerald_cost", -1).forGetter(TeachableRecipe::emeraldCost)
                ).apply(instance, TeachableRecipe::new)
        );
    }
}