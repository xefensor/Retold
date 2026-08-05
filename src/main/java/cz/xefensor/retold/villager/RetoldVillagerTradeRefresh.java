package cz.xefensor.retold.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.timeline.Timelines;

public final class RetoldVillagerTradeRefresh {
    private static final String LAST_REFRESH_DAY_KEY =
            "RetoldLastTradeRefreshDay";
    private RetoldVillagerTradeRefresh() {
    }

    public static void tick(
            ServerLevel level,
            Villager villager
    ) {
        if (level == null || villager == null) {
            return;
        }

        refreshForDay(
                villager,
                level.registryAccess()
                        .get(Timelines.OVERWORLD_DAY)
                        .map(timeline -> timeline.value().getPeriodCount(level.clockManager()))
                        .orElse(0)
        );
    }

    static boolean refreshForDay(
            Villager villager,
            long currentDay
    ) {
        if (villager == null || !villager.isAlive() || villager.isRemoved()) {
            return false;
        }

        var persistentData = villager.getPersistentData();

        if (!persistentData.contains(LAST_REFRESH_DAY_KEY)) {
            persistentData.putLong(LAST_REFRESH_DAY_KEY, currentDay);
            return false;
        }

        long lastRefreshDay = persistentData.getLong(LAST_REFRESH_DAY_KEY)
                .orElse(currentDay);

        if (currentDay < lastRefreshDay) {
            persistentData.putLong(LAST_REFRESH_DAY_KEY, currentDay);
            return false;
        }

        if (currentDay == lastRefreshDay) {
            return false;
        }

        persistentData.putLong(LAST_REFRESH_DAY_KEY, currentDay);

        if (!hasTradeInventory(villager)) {
            return false;
        }

        MerchantOffers offers = villager.getOffers();

        if (offers.isEmpty()) {
            return false;
        }

        for (MerchantOffer offer : offers) {
            offer.updateDemand();
            offer.resetUses();
        }

        syncOpenTradeMenu(villager, offers);
        return true;
    }

    private static boolean hasTradeInventory(Villager villager) {
        if (villager.isBaby()) {
            return false;
        }

        VillagerData data = villager.getVillagerData();

        return !data.profession().is(VillagerProfession.NONE)
                && !data.profession().is(VillagerProfession.NITWIT);
    }

    private static void syncOpenTradeMenu(
            Villager villager,
            MerchantOffers offers
    ) {
        Player tradingPlayer = villager.getTradingPlayer();

        if (tradingPlayer == null) {
            return;
        }

        VillagerData data = villager.getVillagerData();

        tradingPlayer.sendMerchantOffers(
                tradingPlayer.containerMenu.containerId,
                offers,
                data.level(),
                villager.getVillagerXp(),
                VillagerData.canLevelUp(data.level()),
                villager.canRestock()
        );
    }
}
