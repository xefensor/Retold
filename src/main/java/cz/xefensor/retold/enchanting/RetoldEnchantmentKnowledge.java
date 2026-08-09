package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.network.RetoldEnchantmentKnowledgeSyncPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.Set;

/** Server-authoritative entry point for reading, recording, and syncing learned spells. */
public final class RetoldEnchantmentKnowledge {
    private RetoldEnchantmentKnowledge() {
    }

    public static boolean isKnown(ServerPlayer player, Identifier enchantment) {
        return RetoldEnchantmentKnowledgeData.get(player.level()).hasKnown(player, enchantment);
    }

    public static boolean markKnown(ServerPlayer player, Identifier enchantment) {
        return markKnown(player, Set.of(enchantment));
    }

    public static boolean markKnown(ServerPlayer player, Collection<Identifier> enchantments) {
        RetoldEnchantmentKnowledgeData data = RetoldEnchantmentKnowledgeData.get(player.level());
        boolean added = data.markKnown(player, enchantments);

        if (added) {
            syncToPlayer(player);
        }

        return added;
    }

    public static Set<Identifier> knownEnchantments(ServerPlayer player) {
        return RetoldEnchantmentKnowledgeData.get(player.level()).knownEnchantments(player);
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player.connection == null || !player.connection.getConnection().isConnected()) {
            return;
        }

        PacketDistributor.sendToPlayer(
                player,
                new RetoldEnchantmentKnowledgeSyncPayload(knownEnchantments(player))
        );
    }
}
