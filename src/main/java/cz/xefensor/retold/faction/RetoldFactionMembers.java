package cz.xefensor.retold.faction;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.registry.RetoldTags;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RetoldFactionMembers {
    private static final Map<RetoldFaction, TagKey<EntityType<?>>> FACTION_TAGS =
            new EnumMap<>(RetoldFaction.class);
    private static final Map<EntityType<?>, StaticMembership> STATIC_MEMBERSHIPS =
            new ConcurrentHashMap<>();

    static {
        FACTION_TAGS.put(
                RetoldFaction.NETHER_REMNANTS,
                RetoldTags.FACTION_NETHER_REMNANTS
        );
        FACTION_TAGS.put(RetoldFaction.ILLAGERS, RetoldTags.FACTION_ILLAGERS);
        FACTION_TAGS.put(RetoldFaction.UNDEAD, RetoldTags.FACTION_UNDEAD);
        FACTION_TAGS.put(RetoldFaction.SLIMES, RetoldTags.FACTION_SLIMES);
        FACTION_TAGS.put(
                RetoldFaction.AQUATIC_HOSTILES,
                RetoldTags.FACTION_AQUATIC_HOSTILES
        );
        FACTION_TAGS.put(RetoldFaction.CREEPERS, RetoldTags.FACTION_CREEPERS);
        FACTION_TAGS.put(
                RetoldFaction.ARTHROPODS,
                RetoldTags.FACTION_ARTHROPODS
        );
        FACTION_TAGS.put(
                RetoldFaction.SILVERFISH,
                RetoldTags.FACTION_SILVERFISH
        );
        FACTION_TAGS.put(
                RetoldFaction.ENDERMITES,
                RetoldTags.FACTION_ENDERMITES
        );
        FACTION_TAGS.put(
                RetoldFaction.NETHER_BEASTS,
                RetoldTags.FACTION_NETHER_BEASTS
        );
        FACTION_TAGS.put(RetoldFaction.BREEZES, RetoldTags.FACTION_BREEZES);
        FACTION_TAGS.put(RetoldFaction.WARDENS, RetoldTags.FACTION_WARDENS);
        FACTION_TAGS.put(RetoldFaction.BOSSES, RetoldTags.FACTION_BOSSES);
        FACTION_TAGS.put(
                RetoldFaction.CREAKINGS,
                RetoldTags.FACTION_CREAKINGS
        );
        FACTION_TAGS.put(
                RetoldFaction.VILLAGE_DEFENDERS,
                RetoldTags.FACTION_VILLAGE_DEFENDERS
        );
        FACTION_TAGS.put(RetoldFaction.ENDERS, RetoldTags.FACTION_ENDERS);
    }

    private RetoldFactionMembers() {
    }

    public static RetoldFaction getFaction(Entity entity) {
        if (entity == null) {
            return null;
        }

        if (entity instanceof Player) {
            return RetoldFaction.PLAYER;
        }

        if (isDefendingTamedWolf(entity)) {
            return RetoldFaction.VILLAGE_DEFENDERS;
        }

        RetoldFaction faction = getFaction(entity.getType());

        if (faction == RetoldFaction.UNDEAD && isTamedUndeadMount(entity)) {
            return null;
        }

        return faction;
    }

    public static RetoldFaction getFaction(EntityType<?> entityType) {
        if (entityType == null) {
            return null;
        }

        return staticMembership(entityType).faction();
    }

    public static TagKey<EntityType<?>> getFactionTag(RetoldFaction faction) {
        if (faction == null) {
            return null;
        }

        return FACTION_TAGS.get(faction);
    }

    public static boolean hasConflictingFactionTags(EntityType<?> entityType) {
        return entityType != null
                && staticMembership(entityType).conflictingFactionTags();
    }

    public static boolean isMemberOf(Entity entity, RetoldFaction faction) {
        if (entity == null || faction == null) {
            return false;
        }

        return getFaction(entity) == faction;
    }

    public static boolean hasFaction(Entity entity) {
        return getFaction(entity) != null;
    }

    public static boolean isAlignedWith(Entity entity, RetoldFaction faction) {
        /*
         * Permanent identity relation. A loose ally remains non-hostile even when its
         * context-dependent combat cooperation is inactive.
         */
        return isMemberOf(entity, faction)
                || isLooseAllyOf(entity, faction);
    }

    public static boolean isLooseAllyOf(Entity entity, RetoldFaction faction) {
        if (entity == null || faction != RetoldFaction.ILLAGERS) {
            return false;
        }

        StaticMembership membership = staticMembership(entity.getType());

        return membership.faction() == null
                && !membership.conflictingFactionTags()
                && membership.illagerLooseAlly();
    }

    public static boolean isCombatAlignedWith(
            Entity entity,
            RetoldFaction faction
    ) {
        /*
         * Active relation used by help calls and combat coordination. Witches only
         * enter this relation while attached to an active raid.
         */
        return isMemberOf(entity, faction)
                || isActiveLooseAllyOf(entity, faction);
    }

    public static boolean areCooperatingAllies(
            Entity first,
            Entity second,
            RetoldFaction faction
    ) {
        if (first == null || second == null || faction == null) {
            return false;
        }

        if (!isCombatAlignedWith(first, faction)
                || !isCombatAlignedWith(second, faction)) {
            return false;
        }

        if (!isLooseAllyOf(first, faction)
                && !isLooseAllyOf(second, faction)) {
            return true;
        }

        /* Conditional allies must share the exact active raid, not merely be near one. */
        return shareActiveRaid(first, second);
    }

    public static RetoldFaction getActiveCombatFaction(Entity entity) {
        RetoldFaction faction = getFaction(entity);

        if (faction != null) {
            return faction;
        }

        if (isActiveLooseAllyOf(entity, RetoldFaction.ILLAGERS)) {
            return RetoldFaction.ILLAGERS;
        }

        return null;
    }

    public static RetoldFaction getFactionOrLooseAllyFaction(Entity entity) {
        RetoldFaction faction = getFaction(entity);

        if (faction != null) {
            return faction;
        }

        if (isLooseAllyOf(entity, RetoldFaction.ILLAGERS)) {
            return RetoldFaction.ILLAGERS;
        }

        return null;
    }

    public static boolean isTargetableMemberOf(LivingEntity entity, RetoldFaction faction) {
        return isMemberOf(entity, faction);
    }

    public static boolean isPlayer(Entity entity) {
        return isMemberOf(entity, RetoldFaction.PLAYER);
    }

    public static boolean isNetherRemnant(Entity entity) {
        return isMemberOf(entity, RetoldFaction.NETHER_REMNANTS);
    }

    public static boolean isIllager(Entity entity) {
        return isMemberOf(entity, RetoldFaction.ILLAGERS);
    }

    public static boolean isIllagerAligned(Entity entity) {
        return isAlignedWith(entity, RetoldFaction.ILLAGERS);
    }

    private static boolean isActiveLooseAllyOf(
            Entity entity,
            RetoldFaction faction
    ) {
        return isLooseAllyOf(entity, faction)
                && entity instanceof Raider raider
                && raider.hasActiveRaid();
    }

    private static boolean shareActiveRaid(Entity first, Entity second) {
        if (!(first instanceof Raider firstRaider)
                || !(second instanceof Raider secondRaider)) {
            return false;
        }

        Raid raid = firstRaider.getCurrentRaid();

        return raid != null
                && raid.isActive()
                && secondRaider.getCurrentRaid() == raid;
    }

    public static boolean isUndead(Entity entity) {
        return isMemberOf(entity, RetoldFaction.UNDEAD);
    }

    public static boolean isVillageDefender(Entity entity) {
        return isMemberOf(entity, RetoldFaction.VILLAGE_DEFENDERS);
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        STATIC_MEMBERSHIPS.clear();

        if (!(event instanceof TagsUpdatedEvent.ServerDataLoad)) {
            return;
        }

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            staticMembership(entityType);
        }
    }

    private static StaticMembership staticMembership(EntityType<?> entityType) {
        return STATIC_MEMBERSHIPS.computeIfAbsent(
                entityType,
                RetoldFactionMembers::classify
        );
    }

    private static StaticMembership classify(EntityType<?> entityType) {
        EnumSet<RetoldFaction> matches = EnumSet.noneOf(RetoldFaction.class);

        for (Map.Entry<RetoldFaction, TagKey<EntityType<?>>> entry
                : FACTION_TAGS.entrySet()) {
            if (entityType.builtInRegistryHolder().is(entry.getValue())) {
                matches.add(entry.getKey());
            }
        }

        boolean looseAlly = entityType.builtInRegistryHolder().is(
                RetoldTags.ILLAGER_LOOSE_ALLIES
        );

        if (matches.size() > 1) {
            Retold.LOGGER.error(
                    "Entity type {} belongs to conflicting Retold faction tags {}; "
                            + "treating it as unfactioned",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                    matches
            );
            return new StaticMembership(null, true, false);
        }

        RetoldFaction faction = matches.isEmpty() ? null : matches.iterator().next();

        if (faction != null && looseAlly) {
            Retold.LOGGER.warn(
                    "Entity type {} is both a full {} member and an Illager loose ally; "
                            + "full faction membership takes precedence",
                    BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                    faction
            );
            looseAlly = false;
        }

        return new StaticMembership(faction, false, looseAlly);
    }

    private static boolean isDefendingTamedWolf(Entity entity) {
        if (!(entity instanceof Wolf wolf)) {
            return false;
        }

        if (!wolf.isTame()) {
            return false;
        }

        LivingEntity target = wolf.getTarget();

        return target != null
                && target.isAlive()
                && target.level() == wolf.level();
    }

    private static boolean isTamedUndeadMount(Entity entity) {
        if (entity instanceof AbstractHorse horse) {
            return horse.isTamed();
        }

        return entity instanceof TamableAnimal tamableAnimal
                && tamableAnimal.isTame();
    }

    private record StaticMembership(
            RetoldFaction faction,
            boolean conflictingFactionTags,
            boolean illagerLooseAlly
    ) {
    }
}
