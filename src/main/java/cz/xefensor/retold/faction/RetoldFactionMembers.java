package cz.xefensor.retold.faction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class RetoldFactionMembers {
    private static final Map<Identifier, RetoldFaction> EXACT_MEMBERS = new HashMap<>();

    private static final Set<Identifier> ILLAGER_LOOSE_ALLIES = Set.of(
            id("witch")
    );

    static {
        register(RetoldFaction.NETHER_REMNANTS, "piglin", "piglin_brute", "blaze");
        register(RetoldFaction.ILLAGERS, "pillager", "vindicator", "evoker", "illusioner", "ravager", "vex");
        register(
                RetoldFaction.UNDEAD,
                "zombie",
                "zombie_villager",
                "husk",
                "drowned",
                "skeleton",
                "stray",
                "bogged",
                "wither_skeleton",
                "zombified_piglin",
                "phantom",
                "ghast",
                "zoglin"
        );
        register(RetoldFaction.SLIMES, "slime", "magma_cube");
        register(RetoldFaction.AQUATIC_HOSTILES, "guardian", "elder_guardian");
        register(RetoldFaction.CREEPERS, "creeper");
        register(RetoldFaction.ARTHROPODS, "spider", "cave_spider", "silverfish", "endermite");
        register(RetoldFaction.NETHER_BEASTS, "hoglin");
        register(RetoldFaction.BREEZES, "breeze");
        register(RetoldFaction.WARDENS, "warden");
        register(RetoldFaction.BOSSES, "wither", "ender_dragon");
        register(RetoldFaction.CREAKINGS, "creaking");
        register(RetoldFaction.VILLAGE_DEFENDERS, "iron_golem", "snow_golem");
        register(RetoldFaction.ENDERS, "enderman", "shulker");
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

        return EXACT_MEMBERS.get(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
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
        return isMemberOf(entity, faction)
                || isLooseAllyOf(entity, faction);
    }

    public static boolean isLooseAllyOf(Entity entity, RetoldFaction faction) {
        if (entity == null || faction == null) {
            return false;
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return faction == RetoldFaction.ILLAGERS
                && ILLAGER_LOOSE_ALLIES.contains(id);
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

    public static boolean isUndead(Entity entity) {
        return isMemberOf(entity, RetoldFaction.UNDEAD);
    }

    public static boolean isVillageDefender(Entity entity) {
        return isMemberOf(entity, RetoldFaction.VILLAGE_DEFENDERS);
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

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("minecraft", path);
    }

    private static void register(
            RetoldFaction faction,
            String... paths
    ) {
        for (String path : paths) {
            EXACT_MEMBERS.put(id(path), faction);
        }
    }
}
