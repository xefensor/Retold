package cz.xefensor.retold.recipe;

import com.mojang.serialization.Codec;
import cz.xefensor.retold.Retold;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

public class RetoldKnownRecipeData extends SavedData {
    public static final SavedDataType<RetoldKnownRecipeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "crafted_recipes"),
            RetoldKnownRecipeData::new,
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf())
                    .xmap(
                            RetoldKnownRecipeData::new,
                            RetoldKnownRecipeData::toSerializedMap
                    )
    );

    private final Map<UUID, Set<String>> knownRecipesByPlayer = new HashMap<>();

    public RetoldKnownRecipeData() {
    }

    private RetoldKnownRecipeData(Map<String, List<String>> savedData) {
        for (Map.Entry<String, List<String>> entry : savedData.entrySet()) {
            UUID playerId = UUID.fromString(entry.getKey());
            knownRecipesByPlayer.put(playerId, new HashSet<>(entry.getValue()));
        }
    }

    public static ResourceKey<Recipe<?>> recipeKeyFromString(String id) {
        return ResourceKey.create(
                Registries.RECIPE,
                Identifier.parse(id)
        );
    }

    public static RetoldKnownRecipeData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    private Map<String, List<String>> toSerializedMap() {
        Map<String, List<String>> savedData = new HashMap<>();

        for (Map.Entry<UUID, Set<String>> entry : knownRecipesByPlayer.entrySet()) {
            savedData.put(
                    entry.getKey().toString(),
                    new ArrayList<>(entry.getValue())
            );
        }

        return savedData;
    }

    public boolean hasKnown(ServerPlayer player, ResourceKey<Recipe<?>> recipeId) {
        Set<String> craftedRecipes = knownRecipesByPlayer.get(player.getUUID());

        if (craftedRecipes == null) {
            return false;
        }

        return craftedRecipes.contains(recipeId.identifier().toString());
    }

    public boolean markKnown(ServerPlayer player, ResourceKey<Recipe<?>> recipeId) {
        Set<String> craftedRecipes = knownRecipesByPlayer.computeIfAbsent(
                player.getUUID(),
                ignored -> new HashSet<>()
        );

        boolean added = craftedRecipes.add(recipeId.identifier().toString());

        if (added) {
            setDirty();
        }

        return added;
    }
}