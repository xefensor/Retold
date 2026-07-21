package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class AirTempleDiscoveryEvents {
    static final String MAP_DECORATION_KEY = "retold:air_temple";
    private static final String MAP_NAME_KEY = "filled_map.retold.air_temple";
    private static final int MIN_CARTOGRAPHER_LEVEL = 3;
    private static final int MAP_SEARCH_RADIUS = 100;
    private static final int FAILED_SEARCH_RETRY_TICKS = 6000;
    private static final int EMERALD_COST = 12;
    private static final int MAX_USES = 4;
    private static final int VILLAGER_XP = 10;
    private static final float PRICE_MULTIPLIER = 0.2F;
    private static final byte MAP_SCALE = 2;
    private static final TagKey<Structure> AIR_TEMPLE_MAP_DESTINATIONS =
            TagKey.create(
                    Registries.STRUCTURE,
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "on_air_temple_maps"
                    )
            );
    private static final Map<Villager, Long> FAILED_SEARCH_RETRY_AT =
            new WeakHashMap<>();

    private AirTempleDiscoveryEvents() {
    }

    @SubscribeEvent
    public static void onEntityInteract(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getTarget() instanceof Villager villager)) {
            return;
        }

        // Offers may have been generated and saved before the world reached
        // Stage 2. Add this trade lazily so existing cartographers keep their
        // current offers and still gain the discovery path in upgraded worlds.
        ensureExplorerMapOffer(level, villager);
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        FAILED_SEARCH_RETRY_AT.clear();
    }

    private static boolean ensureExplorerMapOffer(
            ServerLevel level,
            Villager villager
    ) {
        if (!isEligible(level, villager) || hasExplorerMapOffer(villager)) {
            return false;
        }

        long gameTime = level.getGameTime();
        if (gameTime < FAILED_SEARCH_RETRY_AT.getOrDefault(villager, 0L)) {
            return false;
        }

        BlockPos templePos = level.findNearestMapStructure(
                AIR_TEMPLE_MAP_DESTINATIONS,
                villager.blockPosition(),
                MAP_SEARCH_RADIUS,
                false
        );
        if (templePos == null) {
            FAILED_SEARCH_RETRY_AT.put(
                    villager,
                    gameTime + FAILED_SEARCH_RETRY_TICKS
            );
            return false;
        }

        FAILED_SEARCH_RETRY_AT.remove(villager);
        return addExplorerMapOffer(level, villager, templePos);
    }

    static boolean ensureExplorerMapOffer(
            ServerLevel level,
            Villager villager,
            BlockPos templePos
    ) {
        if (!isEligible(level, villager) || hasExplorerMapOffer(villager)) {
            return false;
        }

        return addExplorerMapOffer(level, villager, templePos);
    }

    private static boolean isEligible(
            ServerLevel level,
            Villager villager
    ) {
        return !villager.isBaby()
                && villager.getVillagerData()
                        .profession()
                        .is(VillagerProfession.CARTOGRAPHER)
                && villager.getVillagerData().level()
                        >= MIN_CARTOGRAPHER_LEVEL
                && RetoldWorldData.get(level).getStage().getId()
                        >= RetoldWorldStage.STAGE_2.getId();
    }

    private static boolean hasExplorerMapOffer(Villager villager) {
        for (MerchantOffer offer : villager.getOffers()) {
            MapDecorations decorations = offer.getResult().getOrDefault(
                    DataComponents.MAP_DECORATIONS,
                    MapDecorations.EMPTY
            );
            if (decorations.decorations().containsKey(MAP_DECORATION_KEY)) {
                return true;
            }
        }

        return false;
    }

    private static boolean addExplorerMapOffer(
            ServerLevel level,
            Villager villager,
            BlockPos templePos
    ) {
        ItemStack map = MapItem.create(
                level,
                templePos.getX(),
                templePos.getZ(),
                MAP_SCALE,
                true,
                true
        );
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(
                map,
                templePos,
                MAP_DECORATION_KEY,
                MapDecorationTypes.TARGET_X
        );
        map.set(
                DataComponents.ITEM_NAME,
                Component.translatable(MAP_NAME_KEY)
        );

        MerchantOffers offers = villager.getOffers();
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, EMERALD_COST),
                Optional.of(new ItemCost(Items.COMPASS)),
                map,
                MAX_USES,
                VILLAGER_XP,
                PRICE_MULTIPLIER
        ));
        return true;
    }
}
