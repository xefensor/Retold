package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.performance.RetoldAiLod;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public final class RetoldSlimeMergeGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldSlimeMergeGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        registerTest(
                event,
                "compatible_slimes_merge_with_cooldown",
                RetoldSlimeMergeGameTests::compatibleSlimesMergeWithCooldown
        );
        registerTest(
                event,
                "cube_mobs_only_damage_when_hungry",
                RetoldSlimeMergeGameTests::cubeMobsOnlyDamageWhenHungry
        );
        registerTest(
                event,
                "cube_mobs_swallow_items_grow_and_return_them",
                RetoldSlimeMergeGameTests::cubeMobsSwallowItemsGrowAndReturnThem
        );
        registerTest(
                event,
                "cube_mob_size_scales_hunger_and_starvation_splits",
                RetoldSlimeMergeGameTests::cubeMobSizeScalesHungerAndStarvationSplits
        );
        registerTest(
                event,
                "cube_mob_death_splits_inherit_half_hunger",
                RetoldSlimeMergeGameTests::cubeMobDeathSplitsInheritHalfHunger
        );
    }

    private static void cubeMobDeathSplitsInheritHalfHunger(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        String splitFamilyTag = "retold_test_half_hunger_split";
        var parent = helper.spawn(EntityTypes.SLIME, 4, 2, 4);

        parent.setSize(4, true);
        parent.addTag(splitFamilyTag);
        RetoldMobStates.getOrCreate(parent, gameTime).setHunger(73);
        var splitArea = parent.getBoundingBox().inflate(3.0D);

        parent.kill(level);
        parent.remove(Entity.RemovalReason.KILLED);

        helper.succeedWhen(() -> {
            List<AbstractCubeMob> children = level.getEntitiesOfClass(
                    AbstractCubeMob.class,
                    splitArea,
                    child -> child != parent
                            && child.getType() == EntityTypes.SLIME
                            && child.entityTags().contains(splitFamilyTag)
            );

            helper.assertTrue(
                    !children.isEmpty(),
                    "A killed size-four Slime must create vanilla split children"
            );

            for (AbstractCubeMob child : children) {
                helper.assertValueEqual(
                        RetoldMobStates.getOrCreate(child, gameTime).hunger(),
                        36,
                        "Every death-split child must inherit half of the parent's hunger"
                );
            }
        });
    }

    private static void cubeMobSizeScalesHungerAndStarvationSplits(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        var smallSlime = helper.spawn(EntityTypes.SLIME, 2, 2, 2);
        var starvingSlime = helper.spawn(EntityTypes.SLIME, 8, 2, 2);
        var starvingTinySlime = helper.spawn(EntityTypes.SLIME, 14, 2, 2);

        smallSlime.setSize(1, true);
        starvingSlime.setSize(4, true);
        starvingTinySlime.setSize(1, true);

        try {
            for (int size = 1; size <= 10; size++) {
                smallSlime.setSize(size, true);
                helper.assertValueEqual(
                        RetoldSlimeStarvationBehavior.hungerGain(smallSlime),
                        (size + 1) / 2,
                        "Every supported Slime size must use the survival-adjusted hunger rate"
                );
            }

            smallSlime.setSize(1, true);

            ItemStack storedApple = new ItemStack(Items.APPLE);
            helper.assertTrue(
                    RetoldSlimeItemStorage.swallow(starvingSlime, storedApple),
                    "The starving Slime must begin with swallowed storage"
            );

            RetoldMobState starvingState = RetoldMobStates.getOrCreate(
                    starvingSlime,
                    gameTime
            );
            starvingState.setHunger(RetoldSlimeStarvationBehavior.CRITICAL_HUNGER);
            var splitArea = starvingSlime.getBoundingBox().inflate(3.0D);

            helper.assertTrue(
                    RetoldSlimeStarvationBehavior.tryApplyCriticalHunger(
                            level,
                            starvingSlime,
                            starvingState,
                            gameTime
                    ),
                    "A critically hungry size-four Slime must split"
            );
            helper.assertTrue(
                    starvingSlime.isRemoved(),
                    "The critically hungry parent must be replaced by its children"
            );

            List<AbstractCubeMob> children = level.getEntitiesOfClass(
                    AbstractCubeMob.class,
                    splitArea,
                    child -> child.getType() == EntityTypes.SLIME
            );

            helper.assertValueEqual(
                    children.size(),
                    2,
                    "Critical hunger must produce exactly two children"
            );

            int storedItemCount = 0;

            for (AbstractCubeMob child : children) {
                helper.assertValueEqual(
                        child.getSize(),
                        2,
                        "Each child must have half the parent's size"
                );
                helper.assertValueEqual(
                        RetoldMobStates.getOrCreate(child, gameTime).hunger(),
                        RetoldSlimeStarvationBehavior.CRITICAL_HUNGER / 2,
                        "Each child must retain half of the critical hunger"
                );
                helper.assertTrue(
                        RetoldSlimeMergeBehavior.isOnCooldown(child, gameTime),
                        "Starvation children must not immediately merge back together"
                );
                storedItemCount += RetoldSlimeItemStorage.getStoredItemCount(child);
            }

            helper.assertValueEqual(
                    storedItemCount,
                    storedApple.getCount(),
                    "Splitting must preserve swallowed storage exactly once"
            );

            RetoldMobState tinyState = RetoldMobStates.getOrCreate(
                    starvingTinySlime,
                    gameTime
            );
            tinyState.setHunger(RetoldSlimeStarvationBehavior.CRITICAL_HUNGER);

            helper.assertTrue(
                    RetoldSlimeStarvationBehavior.tryApplyCriticalHunger(
                            level,
                            starvingTinySlime,
                            tinyState,
                            gameTime
                    ),
                    "A critically hungry size-one Slime must reach the terminal starvation rule"
            );
            helper.assertFalse(
                    starvingTinySlime.isAlive(),
                    "A size-one Slime must die when it can no longer split"
            );
            helper.succeed();
        } finally {
            smallSlime.discard();
            starvingSlime.discard();
            starvingTinySlime.discard();
            RetoldMobStates.remove(smallSlime);
            RetoldMobStates.remove(starvingSlime);
            RetoldMobStates.remove(starvingTinySlime);
        }
    }

    private static void cubeMobsSwallowItemsGrowAndReturnThem(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var slime = helper.spawn(EntityTypes.SLIME, 2, 2, 2);
        var magmaCube = helper.spawn(EntityTypes.MAGMA_CUBE, 6, 2, 2);
        var killedSlime = helper.spawn(EntityTypes.SLIME, 10, 2, 2);
        var maximumSlime = helper.spawn(EntityTypes.SLIME, 14, 2, 2);
        ItemStack damagedPickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        damagedPickaxe.setDamageValue(37);
        ItemStack stone = new ItemStack(Items.COBBLESTONE, 15);
        ItemStack dirt = new ItemStack(Items.DIRT, 32);
        ItemStack obsidian = new ItemStack(Items.OBSIDIAN, 64);
        ItemStack netherrack = new ItemStack(Items.NETHERRACK, 64);
        ItemStack sand = new ItemStack(Items.SAND, 64);
        ItemStack magmaStorage = new ItemStack(Items.BEACON, 16);

        slime.setSize(1, true);
        magmaCube.setSize(1, true);
        killedSlime.setSize(1, true);
        maximumSlime.setSize(10, true);

        try {
            helper.assertTrue(
                    RetoldMobRules.canEatDroppedItem(slime, damagedPickaxe),
                    "A Slime must recognize non-food tools as edible"
            );
            helper.assertTrue(
                    consume(slime, damagedPickaxe, level.getGameTime()),
                    "The Slime must swallow the complete tool stack"
            );
            helper.assertTrue(
                    consume(slime, stone, level.getGameTime()),
                    "The Slime must swallow the complete block stack"
            );
            helper.assertValueEqual(
                    slime.getSize(),
                    2,
                    "Sixteen swallowed items must grow a size-one Slime"
            );
            helper.assertTrue(
                    consume(slime, dirt, level.getGameTime()),
                    "The Slime must continue swallowing arbitrary item stacks"
            );
            helper.assertValueEqual(
                    slime.getSize(),
                    3,
                    "Growing from size two to three must cost thirty-two swallowed items"
            );
            helper.assertTrue(
                    consume(slime, obsidian, level.getGameTime()),
                    "The next growth step must accept its complete sixty-four-item cost"
            );
            helper.assertValueEqual(
                    slime.getSize(),
                    4,
                    "Growing from size three to four must cost sixty-four swallowed items"
            );
            helper.assertTrue(
                    consume(slime, netherrack, level.getGameTime()),
                    "The Slime must retain partial progress toward an expensive growth step"
            );
            helper.assertValueEqual(
                    slime.getSize(),
                    4,
                    "Half of the size-five cost must not grow the Slime early"
            );
            helper.assertTrue(
                    consume(slime, sand, level.getGameTime()),
                    "A second stack must complete the size-five growth cost"
            );
            helper.assertValueEqual(
                    slime.getSize(),
                    5,
                    "Food-driven growth must continue beyond the natural size-four limit"
            );
            helper.assertValueEqual(
                    RetoldSlimeItemStorage.getStoredItemCount(slime),
                    240,
                    "Every swallowed item must remain in persistent Slime storage"
            );
            helper.assertValueEqual(
                    RetoldSlimeItemStorage.growthCostForSize(9),
                    4096,
                    "The final growth step must require four thousand ninety-six items"
            );

            Collection<ItemEntity> drops = new ArrayList<>();
            LivingDropsEvent dropsEvent = new LivingDropsEvent(
                    slime,
                    level.damageSources().generic(),
                    drops,
                    true
            );
            RetoldSlimeItemStorage.onLivingDrops(dropsEvent);

            helper.assertValueEqual(
                    countMatching(drops, damagedPickaxe),
                    1,
                    "Death must return the swallowed tool with its damage component intact"
            );
            helper.assertValueEqual(
                    countMatching(drops, stone),
                    1,
                    "Death must return the complete swallowed stone stack"
            );
            helper.assertValueEqual(
                    countMatching(drops, dirt),
                    1,
                    "Death must return the complete swallowed dirt stack"
            );
            helper.assertValueEqual(
                    countMatching(drops, obsidian),
                    1,
                    "Death must return the swallowed size-four growth stack"
            );
            helper.assertValueEqual(
                    countMatching(drops, netherrack),
                    1,
                    "Death must return partial expensive-growth progress"
            );
            helper.assertValueEqual(
                    countMatching(drops, sand),
                    1,
                    "Death must return the stack that completed expensive growth"
            );
            helper.assertValueEqual(
                    RetoldSlimeItemStorage.getStoredItemCount(slime),
                    0,
                    "Released storage must be cleared so drops cannot duplicate"
            );

            helper.assertTrue(
                    consume(magmaCube, magmaStorage, level.getGameTime()),
                    "Magma Cubes must use the same arbitrary-item swallowing rule"
            );
            helper.assertValueEqual(
                    magmaCube.getSize(),
                    2,
                    "Sixteen swallowed items must also grow a Magma Cube"
            );

            helper.assertTrue(
                    consume(
                            maximumSlime,
                            new ItemStack(Items.GOLD_INGOT, 64),
                            level.getGameTime()
                    ),
                    "A size-ten Slime must still swallow and store dropped items"
            );
            helper.assertValueEqual(
                    maximumSlime.getSize(),
                    10,
                    "Food-driven growth must stop at size ten"
            );

            ItemStack killedSlimeStorage = new ItemStack(Items.IRON_AXE);
            killedSlimeStorage.setDamageValue(19);
            helper.assertTrue(
                    RetoldSlimeItemStorage.swallow(killedSlime, killedSlimeStorage),
                    "A Slime must persist an exact stack before a real death"
            );
            var deathDropArea = killedSlime.getBoundingBox().inflate(2.0D);
            killedSlime.hurtServer(
                    level,
                    level.damageSources().genericKill(),
                    Float.MAX_VALUE
            );
            helper.assertValueEqual(
                    countMatching(
                            level.getEntitiesOfClass(ItemEntity.class, deathDropArea),
                            killedSlimeStorage
                    ),
                    1,
                    "The registered death event must return swallowed items to the world"
            );
            helper.succeed();
        } finally {
            slime.discard();
            magmaCube.discard();
            killedSlime.discard();
            maximumSlime.discard();
            RetoldMobStates.remove(slime);
            RetoldMobStates.remove(magmaCube);
            RetoldMobStates.remove(killedSlime);
            RetoldMobStates.remove(maximumSlime);
        }
    }

    private static boolean consume(
            net.minecraft.world.entity.monster.cubemob.AbstractCubeMob slime,
            ItemStack stack,
            long gameTime
    ) {
        ItemEntity item = new ItemEntity(
                slime.level(),
                slime.getX(),
                slime.getY(),
                slime.getZ(),
                stack.copy()
        );

        return RetoldFoodBehaviorEvents.tryConsumeDroppedFood(
                slime,
                item,
                gameTime
        ) && item.isRemoved();
    }

    private static int countMatching(
            Collection<ItemEntity> drops,
            ItemStack expected
    ) {
        int matches = 0;

        for (ItemEntity drop : drops) {
            ItemStack actual = drop.getItem();

            if (actual.getCount() == expected.getCount()
                    && ItemStack.isSameItemSameComponents(actual, expected)) {
                matches++;
            }
        }

        return matches;
    }

    private static void cubeMobsOnlyDamageWhenHungry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var slime = helper.spawn(EntityTypes.SLIME, 2, 2, 2);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        var ironGolem = helper.spawn(EntityTypes.IRON_GOLEM, 2, 2, 2);
        var magmaCube = helper.spawn(EntityTypes.MAGMA_CUBE, 5, 2, 2);
        var sheep = helper.spawn(EntityTypes.SHEEP, 5, 2, 2);
        var tinySlime = helper.spawn(EntityTypes.SLIME, 8, 2, 2);
        var pig = helper.spawn(EntityTypes.PIG, 8, 2, 2);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var tinyPlayer = helper.makeMockPlayer(GameType.SURVIVAL);

        slime.setSize(2, true);
        magmaCube.setSize(2, true);
        tinySlime.setSize(1, true);
        player.setPos(slime.getX(), slime.getY(), slime.getZ());
        tinyPlayer.setPos(tinySlime.getX(), tinySlime.getY(), tinySlime.getZ());

        RetoldMobState slimeState = RetoldMobStates.getOrCreate(
                slime,
                level.getGameTime()
        );
        RetoldMobState magmaCubeState = RetoldMobStates.getOrCreate(
                magmaCube,
                level.getGameTime()
        );
        RetoldMobState tinySlimeState = RetoldMobStates.getOrCreate(
                tinySlime,
                level.getGameTime()
        );

        try {
            float fedPlayerHealth = player.getHealth();
            float fedGolemHealth = ironGolem.getHealth();
            float fedTinyPlayerHealth = tinyPlayer.getHealth();

            slime.playerTouch(player);
            slime.push(ironGolem);
            tinySlime.playerTouch(tinyPlayer);

            helper.assertValueEqual(
                    player.getHealth(),
                    fedPlayerHealth,
                    "A fed Slime must not damage a Player on contact"
            );
            helper.assertValueEqual(
                    ironGolem.getHealth(),
                    fedGolemHealth,
                    "A fed Slime must not damage an Iron Golem on contact"
            );
            helper.assertValueEqual(
                    tinyPlayer.getHealth(),
                    fedTinyPlayerHealth,
                    "A fed size-one Slime must remain harmless"
            );
            helper.assertFalse(
                    RetoldCombatTargets.applyAttackTarget(
                            slime,
                            cow,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "A fed Slime must reject a combat target"
            );

            slimeState.setHunger(RetoldMobRules.huntThreshold(slime));
            magmaCubeState.setHunger(RetoldMobRules.huntThreshold(magmaCube));
            tinySlimeState.setHunger(RetoldMobRules.huntThreshold(tinySlime));

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            slime,
                            cow,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The Slime must accept an ordinary mob as its current enemy"
            );
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            magmaCube,
                            sheep,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The Magma Cube must accept an ordinary mob as its current enemy"
            );
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            tinySlime,
                            pig,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "A hungry size-one Slime must accept an ordinary mob as its enemy"
            );

            float cowHealth = cow.getHealth();
            float sheepHealth = sheep.getHealth();
            float hungryPlayerHealth = player.getHealth();
            float hungryGolemHealth = ironGolem.getHealth();
            float hungryTinyPlayerHealth = tinyPlayer.getHealth();
            float pigHealth = pig.getHealth();

            slime.playerTouch(player);
            slime.push(ironGolem);
            slime.push(cow);
            magmaCube.push(sheep);
            tinySlime.playerTouch(tinyPlayer);
            tinySlime.push(pig);

            helper.assertTrue(
                    player.getHealth() < hungryPlayerHealth,
                    "A hungry Slime must retain vanilla Player contact damage"
            );
            helper.assertTrue(
                    ironGolem.getHealth() < hungryGolemHealth,
                    "A hungry Slime must retain vanilla Iron Golem contact damage"
            );
            helper.assertTrue(
                    cow.getHealth() < cowHealth,
                    "A Slime must damage its non-player target on contact"
            );
            helper.assertTrue(
                    sheep.getHealth() < sheepHealth,
                    "A Magma Cube must damage its non-player target on contact"
            );
            helper.assertTrue(
                    tinyPlayer.getHealth() < hungryTinyPlayerHealth,
                    "A hungry size-one Slime must damage Players on contact"
            );
            helper.assertTrue(
                    pig.getHealth() < pigHealth,
                    "A hungry size-one Slime must damage its ordinary target on contact"
            );

            slimeState.setHunger(0);

            helper.assertTrue(
                    RetoldSwarmScavengerEvents.clearSlimeCombatWhenFed(
                            slime,
                            level.getGameTime()
                    ),
                    "Feeding below the hunt threshold must clear a retained Slime target"
            );
            helper.assertTrue(
                    slime.getTarget() == null,
                    "A fed Slime must stop pursuing its previous target"
            );
            helper.succeed();
        } finally {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    slime,
                    cow,
                    true
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    magmaCube,
                    sheep,
                    true
            );
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    tinySlime,
                    pig,
                    true
            );
            slime.discard();
            cow.discard();
            ironGolem.discard();
            magmaCube.discard();
            sheep.discard();
            tinySlime.discard();
            pig.discard();
            player.discard();
            tinyPlayer.discard();
            RetoldMobStates.remove(slime);
            RetoldMobStates.remove(magmaCube);
            RetoldMobStates.remove(tinySlime);
        }
    }

    private static void compatibleSlimesMergeWithCooldown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        long gameTime = level.getGameTime();
        var survivor = helper.spawn(EntityTypes.SLIME, 2, 2, 2);
        var firstPartner = helper.spawn(EntityTypes.SLIME, 2, 2, 2);
        var magmaCube = helper.spawn(EntityTypes.MAGMA_CUBE, 2, 2, 2);
        var magmaPartner = helper.spawn(EntityTypes.MAGMA_CUBE, 2, 2, 2);
        var secondPartner = helper.spawn(EntityTypes.SLIME, 6, 2, 2);
        var maximumPartner = helper.spawn(EntityTypes.SLIME, 8, 2, 2);

        survivor.setSize(1, true);
        firstPartner.setSize(1, true);
        magmaCube.setSize(2, true);
        magmaPartner.setSize(2, true);
        secondPartner.setSize(2, true);
        maximumPartner.setSize(4, true);

        try {
            RetoldSlimeItemStorage.swallow(
                    survivor,
                    new ItemStack(Items.APPLE)
            );
            RetoldSlimeItemStorage.swallow(
                    firstPartner,
                    new ItemStack(Items.DIAMOND)
            );

            helper.assertTrue(
                    RetoldSlimeMergeBehavior.tryMerge(level, survivor, gameTime),
                    "Two touching size-one Slimes must merge"
            );
            helper.assertValueEqual(
                    survivor.getSize(),
                    2,
                    "The surviving Slime must grow to the next natural size"
            );
            helper.assertTrue(
                    firstPartner.isRemoved(),
                    "The absorbed Slime must be removed without a death split"
            );
            helper.assertValueEqual(
                    RetoldSlimeItemStorage.getStoredItemCount(survivor),
                    2,
                    "A merge must preserve swallowed items from both Slimes"
            );
            helper.assertTrue(
                    RetoldSlimeMergeBehavior.isOnCooldown(survivor, gameTime),
                    "A merged Slime must immediately enter its merge cooldown"
            );
            helper.assertFalse(
                    RetoldSlimeMergeBehavior.tryMerge(level, survivor, gameTime),
                    "The merge cooldown must prevent an immediate second merge"
            );

            long afterCooldown = gameTime + RetoldSlimeMergeBehavior.MERGE_COOLDOWN_TICKS;

            helper.assertFalse(
                    RetoldSlimeMergeBehavior.tryMerge(level, survivor, afterCooldown),
                    "Slimes and Magma Cubes must not merge across species"
            );
            helper.assertFalse(
                    magmaCube.isRemoved(),
                    "An incompatible Magma Cube must remain in the world"
            );

            helper.assertTrue(
                    RetoldSlimeMergeBehavior.tryMerge(level, magmaCube, afterCooldown),
                    "Two touching same-size Magma Cubes must merge"
            );
            helper.assertValueEqual(
                    magmaCube.getSize(),
                    4,
                    "Compatible Magma Cubes must use the same natural merge sizes"
            );
            helper.assertTrue(
                    magmaPartner.isRemoved(),
                    "The absorbed Magma Cube must be removed without a death split"
            );

            secondPartner.setPos(
                    survivor.getX(),
                    survivor.getY(),
                    survivor.getZ()
            );
            long afterCacheRefresh = afterCooldown
                    + RetoldAiLod.cacheTicks(survivor, 6)
                    + 1L;

            helper.assertTrue(
                    RetoldSlimeMergeBehavior.tryMerge(level, survivor, afterCacheRefresh),
                    "The Slime must merge again after its cooldown expires"
            );
            helper.assertValueEqual(
                    survivor.getSize(),
                    4,
                    "A second compatible merge must produce a full-size Slime"
            );
            helper.assertTrue(
                    secondPartner.isRemoved(),
                    "The second compatible Slime must be absorbed"
            );

            maximumPartner.setPos(
                    survivor.getX(),
                    survivor.getY(),
                    survivor.getZ()
            );
            long afterSecondCooldown = afterCacheRefresh
                    + RetoldSlimeMergeBehavior.MERGE_COOLDOWN_TICKS;

            helper.assertFalse(
                    RetoldSlimeMergeBehavior.tryMerge(
                            level,
                            survivor,
                            afterSecondCooldown
                    ),
                    "Full-size natural Slimes must not merge into oversized Slimes"
            );
            helper.assertFalse(
                    maximumPartner.isRemoved(),
                    "A rejected full-size merge partner must remain in the world"
            );
            helper.succeed();
        } finally {
            survivor.discard();
            firstPartner.discard();
            magmaCube.discard();
            magmaPartner.discard();
            secondPartner.discard();
            maximumPartner.discard();
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(
                Retold.MODID,
                path
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            String name,
            Consumer<GameTestHelper> test
    ) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_" + name),
                        new TestEnvironmentDefinition.AllOf()
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(
                id(name),
                new InlineGameTest(testData, test)
        );
    }

    private static final class InlineGameTest extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> test;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> testFunction
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.test = testFunction;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }
    }
}
