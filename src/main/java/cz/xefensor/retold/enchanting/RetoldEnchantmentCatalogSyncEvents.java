package cz.xefensor.retold.enchanting;

import cz.xefensor.retold.network.RetoldEnchantmentCatalogSyncPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RetoldEnchantmentCatalogSyncEvents {
    private RetoldEnchantmentCatalogSyncEvents() {
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        RetoldEnchantmentCatalogSyncPayload payload =
                new RetoldEnchantmentCatalogSyncPayload(
                        RetoldEnchantmentCatalog.definitions()
                );

        event.getRelevantPlayers().forEach(player ->
                PacketDistributor.sendToPlayer(player, payload)
        );
    }
}
