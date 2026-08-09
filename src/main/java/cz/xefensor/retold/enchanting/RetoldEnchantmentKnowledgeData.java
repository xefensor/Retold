package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public final class RetoldEnchantmentKnowledgeData extends SavedData {
    public static final SavedDataType<RetoldEnchantmentKnowledgeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "enchantment_knowledge"),
            RetoldEnchantmentKnowledgeData::new,
            RetoldEnchantmentKnowledgeCodecs.STATE.xmap(
                    RetoldEnchantmentKnowledgeData::new,
                    RetoldEnchantmentKnowledgeData::serialize
            )
    );

    private final RetoldEnchantmentKnowledgeStore store;

    public RetoldEnchantmentKnowledgeData() {
        store = new RetoldEnchantmentKnowledgeStore();
    }

    private RetoldEnchantmentKnowledgeData(
            RetoldEnchantmentKnowledgeStore.SerializedState serializedState
    ) {
        store = RetoldEnchantmentKnowledgeStore.fromSerializedState(serializedState);
    }

    public static RetoldEnchantmentKnowledgeData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean hasKnown(ServerPlayer player, Identifier enchantment) {
        return store.hasKnown(player.getUUID(), enchantment.toString());
    }

    public boolean markKnown(ServerPlayer player, Identifier enchantment) {
        return markKnown(player, Set.of(enchantment));
    }

    public boolean markKnown(ServerPlayer player, Collection<Identifier> enchantments) {
        Set<String> enchantmentIds = enchantments.stream()
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
        boolean added = store.markKnown(player.getUUID(), enchantmentIds);

        if (added) {
            setDirty();
        }

        return added;
    }

    public Set<Identifier> knownEnchantments(ServerPlayer player) {
        return store.knownEnchantments(player.getUUID()).stream()
                .map(Identifier::parse)
                .collect(Collectors.toUnmodifiableSet());
    }

    private RetoldEnchantmentKnowledgeStore.SerializedState serialize() {
        return store.serialize();
    }
}
