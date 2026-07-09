package cz.xefensor.retold.behavior;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class RetoldMobRules {
    private static final int DESPERATE_HUNT_HUNGER = 86;
    private static final int LOW_CONFIDENCE_HUNT_GATE = 35;
    private static final int HIGH_STRESS_HUNT_GATE = 55;

    private RetoldMobRules() {
    }

    public static boolean isManagedMob(Entity entity) {
        if (entity == null) {
            return false;
        }

        String path = getEntityTypePath(entity.getType());

        return RetoldMobProfiles.isManaged(path);
    }

    public static boolean isManagedPredator(Entity entity) {
        if (entity == null) {
            return false;
        }

        return RetoldMobProfiles.get(entity).predator();
    }

    public static RetoldMobProfile profile(Entity entity) {
        return RetoldMobProfiles.get(entity);
    }

    public static RetoldMobProfileType profileType(Entity entity) {
        return RetoldMobProfiles.get(entity).type();
    }

    public static boolean isPackSocialHunter(Entity entity) {
        if (entity == null) {
            return false;
        }

        RetoldMobProfile profile = RetoldMobProfiles.get(entity);

        return profile.predator() && profile.packSocial();
    }

    public static boolean shouldBlockVanillaPredatorTarget(
            PathfinderMob mob,
            LivingEntity target
    ) {
        if (mob == null || target == null) {
            return false;
        }

        if (!isManagedPredator(mob)) {
            return false;
        }

        /*
         * Retold-owned combat is allowed.
         * Vanilla/random prey targeting is not.
         */
        if (
                RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.HUNT)
                        || RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.ATTACK)
                        || RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.TERRITORY)
        ) {
            return false;
        }

        return true;
    }

    public static int hungerInterval(PathfinderMob mob) {
        return RetoldMobProfiles.get(mob).hungerIntervalTicks();
    }

    public static int eatThreshold(PathfinderMob mob) {
        return RetoldMobProfiles.get(mob).eatThreshold();
    }

    public static int huntThreshold(PathfinderMob mob) {
        return RetoldMobProfiles.get(mob).huntThreshold();
    }

    public static boolean hasHuntDrive(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        if (mob == null || state == null) {
            return false;
        }

        int hunger = state.hunger();

        if (hunger >= DESPERATE_HUNT_HUNGER) {
            return true;
        }

        return hunger >= adjustedHuntThreshold(
                mob,
                state
        );
    }

    public static int adjustedHuntThreshold(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        int threshold = huntThreshold(mob);

        if (state == null) {
            return threshold;
        }

        int confidencePenalty = Math.max(
                0,
                LOW_CONFIDENCE_HUNT_GATE - state.confidence()
        ) / 2;

        int stressPenalty = Math.max(
                0,
                state.stress() - HIGH_STRESS_HUNT_GATE
        ) / 3;

        return Math.min(
                DESPERATE_HUNT_HUNGER,
                threshold + confidencePenalty + stressPenalty
        );
    }

    public static int foodRelief(
            PathfinderMob mob,
            String itemPath
    ) {
        String mobPath = getEntityTypePath(mob.getType());

        if (isPredator(mobPath)) {
            if (isMeatItem(itemPath) || isFishItem(itemPath)) {
                return 28;
            }

            return 18;
        }

        if (isGrazer(mobPath)) {
            if (itemPath.equals("hay_block") || itemPath.equals("wheat")) {
                return 28;
            }

            return 20;
        }

        if (isSmallPassive(mobPath)) {
            return 20;
        }

        if (isSlime(mobPath)) {
            return 18;
        }

        return 20;
    }

    public static int forageRelief(
            PathfinderMob mob,
            String blockPath
    ) {
        String mobPath = getEntityTypePath(mob.getType());

        if (isGrazer(mobPath)) {
            if (blockPath.equals("grass_block") || isCropBlock(blockPath)) {
                return 24;
            }

            return 18;
        }

        if (isSmallPassive(mobPath)) {
            if (isCropBlock(blockPath)) {
                return 22;
            }

            return 16;
        }

        if (mobPath.equals("bee")) {
            return 16;
        }

        if (mobPath.equals("hoglin")) {
            return 24;
        }

        return 16;
    }

    public static boolean canEatDroppedItem(
            PathfinderMob mob,
            ItemStack stack
    ) {
        if (mob == null || stack == null || stack.isEmpty()) {
            return false;
        }

        String mobPath = getEntityTypePath(mob.getType());
        String itemPath = getItemPath(stack);

        if (isPredator(mobPath)) {
            return isMeatItem(itemPath)
                    || isFishItem(itemPath)
                    || (mobPath.equals("fox") && isBerryItem(itemPath))
                    || ((mobPath.equals("cat") || mobPath.equals("ocelot")) && itemPath.equals("phantom_membrane"));
        }

        if (isGrazer(mobPath)) {
            return isGrazerFoodItem(itemPath);
        }

        if (isSmallPassive(mobPath)) {
            return isSmallPassiveFoodItem(itemPath);
        }

        if (mobPath.equals("bee")) {
            return isFlower(itemPath);
        }

        if (isNetherHungry(mobPath)) {
            return isMeatItem(itemPath)
                    || isNetherFungusItem(itemPath);
        }

        if (isUndeadHungry(mobPath)) {
            return itemPath.equals("rotten_flesh")
                    || isMeatItem(itemPath);
        }

        if (isSlime(mobPath)) {
            return isOrganicItem(itemPath);
        }

        if (mobPath.equals("guardian") || mobPath.equals("elder_guardian")) {
            return isFishItem(itemPath);
        }

        return false;
    }

    public static boolean canForageBlock(
            PathfinderMob mob,
            BlockState state
    ) {
        if (mob == null || state == null) {
            return false;
        }

        String mobPath = getEntityTypePath(mob.getType());
        String blockPath = getBlockPath(state);

        if (isGrazer(mobPath)) {
            return blockPath.equals("grass_block")
                    || blockPath.equals("short_grass")
                    || blockPath.equals("tall_grass")
                    || blockPath.equals("fern")
                    || blockPath.equals("large_fern")
                    || isCropBlock(blockPath)
                    || isFlower(blockPath);
        }

        if (isSmallPassive(mobPath)) {
            return blockPath.equals("short_grass")
                    || blockPath.equals("tall_grass")
                    || isCropBlock(blockPath)
                    || isFlower(blockPath);
        }

        if (mobPath.equals("bee")) {
            return isFlower(blockPath);
        }

        if (mobPath.equals("hoglin")) {
            return blockPath.equals("crimson_fungus");
        }

        return false;
    }

    public static boolean canHuntPrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        if (hunter == null || prey == null) {
            return false;
        }

        if (!prey.isAlive() || prey.isRemoved()) {
            return false;
        }

        if (hunter.level() != prey.level()) {
            return false;
        }

        String hunterPath = getEntityTypePath(hunter.getType());
        String preyPath = getEntityTypePath(prey.getType());

        if (hunterPath.equals("wolf")) {
            /*
             * Skeletons are wolf enemies later, not food.
             */
            return preyPath.equals("sheep")
                    || preyPath.equals("rabbit")
                    || preyPath.equals("chicken")
                    || isFishEntity(preyPath);
        }

        if (hunterPath.equals("fox")) {
            return preyPath.equals("chicken")
                    || preyPath.equals("rabbit")
                    || isFishEntity(preyPath);
        }

        if (hunterPath.equals("cat") || hunterPath.equals("ocelot")) {
            return preyPath.equals("rabbit")
                    || preyPath.equals("chicken")
                    || isFishEntity(preyPath)
                    || preyPath.equals("phantom");
        }

        if (isSpider(hunterPath)) {
            /*
             * No villager/player spider hunting in this food layer.
             * Combat behavior comes later.
             */
            return isPassiveFoodPrey(preyPath)
                    || isFishEntity(preyPath);
        }

        if (hunterPath.equals("dolphin")) {
            return isFishEntity(preyPath);
        }

        return false;
    }

    public static boolean isSmallFoodPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = getEntityTypePath(entity.getType());

        return path.equals("rabbit")
                || path.equals("chicken")
                || isFishEntity(path);
    }

    public static boolean isWolfEnemyButNotFood(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = getEntityTypePath(entity.getType());

        return path.equals("skeleton")
                || path.equals("stray")
                || path.equals("bogged")
                || path.equals("wither_skeleton");
    }

    private static boolean isPredator(String path) {
        return RetoldMobProfiles.isPredator(path);
    }

    private static boolean isSpider(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.HUNGRY_SWARM_PREDATOR);
    }

    private static boolean isGrazer(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.HUNGRY_GRAZER);
    }

    private static boolean isSmallPassive(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.SMALL_FORAGER);
    }

    private static boolean isNetherHungry(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.NETHER_HUNGRY);
    }

    private static boolean isUndeadHungry(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.UNDEAD_HUNGRY);
    }

    private static boolean isSlime(String path) {
        return RetoldMobProfiles.isType(path, RetoldMobProfileType.SLIME_HUNGRY);
    }

    private static boolean isPassiveFoodPrey(String path) {
        return path.equals("cow")
                || path.equals("sheep")
                || path.equals("pig")
                || path.equals("chicken")
                || path.equals("rabbit")
                || path.equals("goat")
                || path.equals("horse")
                || path.equals("donkey")
                || path.equals("mule")
                || path.equals("llama")
                || path.equals("trader_llama")
                || path.equals("camel");
    }

    private static boolean isFishEntity(String path) {
        return path.equals("cod")
                || path.equals("salmon")
                || path.equals("tropical_fish")
                || path.equals("pufferfish");
    }

    private static boolean isMeatItem(String itemPath) {
        return itemPath.equals("beef")
                || itemPath.equals("cooked_beef")
                || itemPath.equals("porkchop")
                || itemPath.equals("cooked_porkchop")
                || itemPath.equals("mutton")
                || itemPath.equals("cooked_mutton")
                || itemPath.equals("chicken")
                || itemPath.equals("cooked_chicken")
                || itemPath.equals("rabbit")
                || itemPath.equals("cooked_rabbit")
                || itemPath.equals("rotten_flesh");
    }

    private static boolean isFishItem(String itemPath) {
        return itemPath.equals("cod")
                || itemPath.equals("cooked_cod")
                || itemPath.equals("salmon")
                || itemPath.equals("cooked_salmon")
                || itemPath.equals("tropical_fish")
                || itemPath.equals("pufferfish");
    }

    private static boolean isBerryItem(String itemPath) {
        return itemPath.equals("sweet_berries")
                || itemPath.equals("glow_berries");
    }

    private static boolean isGrazerFoodItem(String itemPath) {
        return itemPath.equals("wheat")
                || itemPath.equals("hay_block")
                || itemPath.equals("apple")
                || itemPath.equals("carrot")
                || itemPath.equals("potato")
                || itemPath.equals("beetroot")
                || itemPath.equals("grass")
                || itemPath.equals("short_grass")
                || itemPath.equals("tall_grass")
                || itemPath.equals("fern")
                || itemPath.equals("large_fern");
    }

    private static boolean isSmallPassiveFoodItem(String itemPath) {
        return itemPath.equals("wheat_seeds")
                || itemPath.equals("beetroot_seeds")
                || itemPath.equals("melon_seeds")
                || itemPath.equals("pumpkin_seeds")
                || itemPath.equals("carrot")
                || itemPath.equals("potato")
                || itemPath.equals("beetroot")
                || itemPath.equals("dandelion");
    }

    private static boolean isNetherFungusItem(String itemPath) {
        return itemPath.equals("crimson_fungus")
                || itemPath.equals("warped_fungus")
                || itemPath.equals("brown_mushroom")
                || itemPath.equals("red_mushroom");
    }

    private static boolean isOrganicItem(String itemPath) {
        return isMeatItem(itemPath)
                || isFishItem(itemPath)
                || isGrazerFoodItem(itemPath)
                || isSmallPassiveFoodItem(itemPath)
                || isFlower(itemPath)
                || isNetherFungusItem(itemPath)
                || itemPath.equals("bone")
                || itemPath.equals("spider_eye")
                || itemPath.equals("string")
                || itemPath.equals("slime_ball")
                || itemPath.equals("magma_cream")
                || itemPath.equals("phantom_membrane");
    }

    private static boolean isCropBlock(String blockPath) {
        return blockPath.equals("wheat")
                || blockPath.equals("carrots")
                || blockPath.equals("potatoes")
                || blockPath.equals("beetroots")
                || blockPath.equals("melon_stem")
                || blockPath.equals("pumpkin_stem")
                || blockPath.equals("attached_melon_stem")
                || blockPath.equals("attached_pumpkin_stem");
    }

    private static boolean isFlower(String path) {
        return path.endsWith("_flower")
                || path.equals("poppy")
                || path.equals("dandelion")
                || path.equals("blue_orchid")
                || path.equals("allium")
                || path.equals("azure_bluet")
                || path.equals("red_tulip")
                || path.equals("orange_tulip")
                || path.equals("white_tulip")
                || path.equals("pink_tulip")
                || path.equals("oxeye_daisy")
                || path.equals("cornflower")
                || path.equals("lily_of_the_valley")
                || path.equals("sunflower")
                || path.equals("lilac")
                || path.equals("rose_bush")
                || path.equals("peony");
    }

    public static String getItemPath(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int separator = id.indexOf(':');

        if (separator < 0 || separator + 1 >= id.length()) {
            return id;
        }

        return id.substring(separator + 1);
    }

    public static String getBlockPath(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        int separator = id.indexOf(':');

        if (separator < 0 || separator + 1 >= id.length()) {
            return id;
        }

        return id.substring(separator + 1);
    }

    public static String getEntityTypePath(EntityType<?> entityType) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        int separator = id.indexOf(':');

        if (separator < 0 || separator + 1 >= id.length()) {
            return id;
        }

        return id.substring(separator + 1);
    }
}
