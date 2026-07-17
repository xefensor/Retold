package cz.xefensor.retold.behavior.profiles;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class RetoldMobProfiles {
    private static final int DEFAULT_EAT_THRESHOLD = 18;
    private static final int DEFAULT_HUNT_THRESHOLD = 36;
    private static final int NO_HUNGER_THRESHOLD = 101;

    private static final RetoldMobProfile NONE = new RetoldMobProfile(
            RetoldMobProfileType.NONE,
            false,
            false,
            false,
            false,
            460,
            DEFAULT_EAT_THRESHOLD,
            DEFAULT_HUNT_THRESHOLD
    );

    private static final Map<String, RetoldMobProfile> PROFILES = new HashMap<>();
    private static final Map<EntityType<?>, RetoldMobProfile> PROFILES_BY_TYPE = new IdentityHashMap<>();

    static {
        registerGrazer(
                "cow",
                "mooshroom",
                "sheep",
                "goat",
                "horse",
                "donkey",
                "mule",
                "llama",
                "trader_llama",
                "camel"
        );
        registerSmallForager("pig", "chicken", "rabbit");
        registerPackPredator("wolf");
        registerSoloOpportunist("fox", "cat", "ocelot");
        registerAquaticPredator("dolphin");
        registerHungrySwarmPredator("spider", "cave_spider");
        registerHiveColony("bee");
        registerNetherHungry("piglin", "hoglin");
        registerUndeadHungry(
                "zombie",
                "zombie_villager",
                "husk",
                "drowned",
                "zombified_piglin"
        );
        registerUndeadTolerant("skeleton", "stray", "bogged");
        registerPhantomStalker("phantom");
        registerGhastArtillery("ghast");
        registerZoglinRampager("zoglin");
        registerSlimeHungry("slime", "magma_cube");
        registerSmallArthropodSwarm("silverfish", "endermite");
        registerProtectiveNeutral("polar_bear");
        registerArmadilloDefensive("armadillo");
        registerPandaBamboo("panda");
        registerSnifferForager("sniffer");
        registerTurtleBeach("turtle");
        registerAmphibianForager("frog");
        registerAquaticHelperPredator("axolotl");
        registerAquaticTerritoryGuard("guardian", "elder_guardian");
        registerTerritoryGuard("iron_golem", "snow_golem", "piglin_brute", "blaze", "shulker", "wither_skeleton");
        registerCommanderSupport("evoker", "witch");
        registerIllagerRaider("pillager", "vindicator", "ravager", "vex", "illusioner");
        registerSpecialVanilla("creeper", "enderman", "breeze", "creaking");
        registerApexOrBoss("warden", "wither", "ender_dragon");
    }

    private RetoldMobProfiles() {
    }

    public static RetoldMobProfile get(Entity entity) {
        if (entity == null) {
            return NONE;
        }

        return get(entity.getType());
    }

    public static RetoldMobProfile get(EntityType<?> entityType) {
        if (entityType == null) {
            return NONE;
        }

        return PROFILES_BY_TYPE.computeIfAbsent(
                entityType,
                type -> get(RetoldMobRules.getEntityTypePath(type))
        );
    }

    public static RetoldMobProfile get(String entityPath) {
        return PROFILES.getOrDefault(entityPath, NONE);
    }

    public static boolean isManaged(String entityPath) {
        return get(entityPath).managed();
    }

    public static boolean isPredator(String entityPath) {
        return get(entityPath).predator();
    }

    public static boolean isPackSocial(String entityPath) {
        return get(entityPath).packSocial();
    }

    public static boolean isType(String entityPath, RetoldMobProfileType type) {
        return get(entityPath).is(type);
    }

    private static void registerGrazer(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.HUNGRY_GRAZER,
                        true,
                        false,
                        true,
                        false,
                        440,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSmallForager(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SMALL_FORAGER,
                        true,
                        false,
                        true,
                        false,
                        390,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerPackPredator(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.PACK_PREDATOR,
                        true,
                        true,
                        true,
                        false,
                        460,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSoloOpportunist(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SOLO_OPPORTUNIST,
                        true,
                        true,
                        false,
                        false,
                        460,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerAquaticPredator(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.AQUATIC_PREDATOR,
                        true,
                        true,
                        true,
                        false,
                        460,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerHungrySwarmPredator(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.HUNGRY_SWARM_PREDATOR,
                        true,
                        true,
                        false,
                        false,
                        460,
                        DEFAULT_EAT_THRESHOLD,
                        30
                ),
                entityPaths
        );
    }

    private static void registerHiveColony(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.HIVE_COLONY,
                        true,
                        false,
                        true,
                        false,
                        460,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerNetherHungry(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.NETHER_HUNGRY,
                        true,
                        false,
                        true,
                        false,
                        380,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerUndeadHungry(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.UNDEAD_HUNGRY,
                        true,
                        false,
                        true,
                        false,
                        360,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerUndeadTolerant(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.UNDEAD_TOLERANT,
                        true,
                        false,
                        false,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerPhantomStalker(String... entityPaths) {
        registerSpecialUndead(
                RetoldMobProfileType.PHANTOM_STALKER,
                entityPaths
        );
    }

    private static void registerGhastArtillery(String... entityPaths) {
        registerSpecialUndead(
                RetoldMobProfileType.GHAST_ARTILLERY,
                entityPaths
        );
    }

    private static void registerZoglinRampager(String... entityPaths) {
        registerSpecialUndead(
                RetoldMobProfileType.ZOGLIN_RAMPAGER,
                entityPaths
        );
    }

    private static void registerSpecialUndead(
            RetoldMobProfileType type,
            String... entityPaths
    ) {
        register(
                new RetoldMobProfile(
                        type,
                        false,
                        false,
                        false,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSlimeHungry(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SLIME_HUNGRY,
                        true,
                        false,
                        false,
                        false,
                        360,
                        DEFAULT_EAT_THRESHOLD,
                        DEFAULT_HUNT_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSmallArthropodSwarm(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SMALL_ARTHROPOD_SWARM,
                        true,
                        false,
                        true,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerProtectiveNeutral(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.PROTECTIVE_NEUTRAL,
                        true,
                        false,
                        true,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerPandaBamboo(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.PANDA_BAMBOO,
                        true,
                        false,
                        true,
                        false,
                        520,
                        24,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSnifferForager(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SNIFFER_FORAGER,
                        true,
                        false,
                        false,
                        false,
                        520,
                        30,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerArmadilloDefensive(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.ARMADILLO_DEFENSIVE,
                        true,
                        false,
                        false,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerTurtleBeach(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.TURTLE_BEACH,
                        true,
                        false,
                        false,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerAmphibianForager(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.AMPHIBIAN_FORAGER,
                        true,
                        false,
                        true,
                        false,
                        420,
                        NO_HUNGER_THRESHOLD,
                        34
                ),
                entityPaths
        );
    }

    private static void registerAquaticHelperPredator(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.AQUATIC_HELPER_PREDATOR,
                        true,
                        false,
                        true,
                        false,
                        420,
                        NO_HUNGER_THRESHOLD,
                        36
                ),
                entityPaths
        );
    }

    private static void registerAquaticTerritoryGuard(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.AQUATIC_TERRITORY_GUARD,
                        true,
                        false,
                        true,
                        true,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerTerritoryGuard(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.TERRITORY_GUARD,
                        true,
                        false,
                        true,
                        true,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerCommanderSupport(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.COMMANDER_SUPPORT,
                        true,
                        false,
                        true,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerIllagerRaider(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.ILLAGER_RAIDER,
                        true,
                        false,
                        true,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerSpecialVanilla(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.SPECIAL_VANILLA,
                        false,
                        false,
                        false,
                        false,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void registerApexOrBoss(String... entityPaths) {
        register(
                new RetoldMobProfile(
                        RetoldMobProfileType.APEX_OR_BOSS,
                        false,
                        false,
                        false,
                        true,
                        0,
                        NO_HUNGER_THRESHOLD,
                        NO_HUNGER_THRESHOLD
                ),
                entityPaths
        );
    }

    private static void register(RetoldMobProfile profile, String... entityPaths) {
        for (String entityPath : entityPaths) {
            PROFILES.put(entityPath, profile);
        }
    }
}
