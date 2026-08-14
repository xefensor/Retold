package cz.xefensor.retold.network;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.recipe.RetoldKnownRecipeData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record RetoldRecipeKnowledgeSyncPayload(
        List<ResourceKey<Recipe<?>>> knownRecipes
) implements CustomPacketPayload {
    private static final int MAX_KNOWN_RECIPES = 65_536;

    public static final Type<RetoldRecipeKnowledgeSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(
                    Retold.MODID,
                    "recipe_knowledge_sync"
            ));

    private static final StreamCodec<ByteBuf, ResourceKey<Recipe<?>>> RECIPE_KEY_CODEC =
            Identifier.STREAM_CODEC.map(
                    RetoldKnownRecipeData::recipeKeyFromIdentifier,
                    ResourceKey::identifier
            );

    private static final StreamCodec<ByteBuf, List<ResourceKey<Recipe<?>>>>
            RECIPE_LIST_CODEC = ByteBufCodecs.collection(
                    ArrayList::new,
                    RECIPE_KEY_CODEC,
                    MAX_KNOWN_RECIPES
            );

    public static final StreamCodec<ByteBuf, RetoldRecipeKnowledgeSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    RECIPE_LIST_CODEC,
                    RetoldRecipeKnowledgeSyncPayload::knownRecipes,
                    RetoldRecipeKnowledgeSyncPayload::new
            );

    public RetoldRecipeKnowledgeSyncPayload(
            Collection<ResourceKey<Recipe<?>>> knownRecipes
    ) {
        this(knownRecipes.stream()
                .sorted((first, second) -> first.identifier()
                        .compareTo(second.identifier()))
                .toList());
    }

    public RetoldRecipeKnowledgeSyncPayload {
        knownRecipes = List.copyOf(knownRecipes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
