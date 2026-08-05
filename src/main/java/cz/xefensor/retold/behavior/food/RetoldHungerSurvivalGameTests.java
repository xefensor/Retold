package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.hunting.RetoldControlledHuntingEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.villager.RetoldVillagerCommunalFood;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Natural feeding-path coverage for every loaded Retold mob with metabolism.
 *
 * <p>Each species begins at 99 hunger and must stay alive while lowering hunger
 * through its production behavior. Predators receive living prey, ordinary
 * foragers receive food found in one of their actual spawn habitats, aquatic
 * mobs receive a water habitat, and Villagers must use communal village
 * storage. The registry coverage test fails when a positive-hunger profile is
 * added without a case.</p>
 */
public final class RetoldHungerSurvivalGameTests {
    private static final Identifier TEST_STRUCTURE =
            Identifier.withDefaultNamespace("woodland_mansion/2x2_a1");

    private static final int INITIAL_HUNGER = 99;
    private static final int TEST_TIMEOUT_TICKS = 700;

    private static final List<SurvivalCase> CASES = List.of(
            source("armadillo", Habitat.BADLANDS, FoodSource.GRUB_BADLANDS),
            prey("axolotl", Habitat.AQUATIC, "tropical_fish"),
            source("bat", Habitat.CAVE, FoodSource.AMBIENT_CAVE_INSECTS),
            source("bee", Habitat.LAND, FoodSource.FLOWER),
            source("camel", Habitat.DESERT, FoodSource.DESERT_BROWSE),
            prey("cat", Habitat.WETLAND, "frog"),
            prey("cave_spider", Habitat.CAVE, "bat"),
            source("chicken", Habitat.LAND, FoodSource.SMALL_PLANT),
            source("cow", Habitat.LAND, FoodSource.GRASS),
            prey("dolphin", Habitat.AQUATIC, "cod"),
            source("donkey", Habitat.LAND, FoodSource.GRASS),
            prey("drowned", Habitat.AQUATIC, "salmon"),
            prey("fox", Habitat.LAND, "chicken"),
            prey("frog", Habitat.WETLAND, "slime"),
            source("goat", Habitat.MOUNTAIN, FoodSource.ALPINE_FORAGE),
            source("hoglin", Habitat.NETHER, FoodSource.CRIMSON_FUNGUS),
            source("horse", Habitat.LAND, FoodSource.GRASS),
            prey("husk", Habitat.DESERT, "rabbit"),
            source("llama", Habitat.LAND, FoodSource.GRASS),
            prey("magma_cube", Habitat.LAVA, "strider"),
            source("mooshroom", Habitat.MUSHROOM_FIELDS, FoodSource.MYCELIUM),
            source("mule", Habitat.LAND, FoodSource.GRASS),
            prey("nautilus", Habitat.AQUATIC, "cod"),
            prey("ocelot", Habitat.LAND, "chicken"),
            source("panda", Habitat.LAND, FoodSource.BAMBOO),
            source("pig", Habitat.LAND, FoodSource.SMALL_PLANT),
            source("piglin", Habitat.NETHER, FoodSource.NETHER_MUSHROOM),
            source("rabbit", Habitat.DESERT, FoodSource.DESERT_BROWSE),
            source("sheep", Habitat.LAND, FoodSource.GRASS),
            prey("slime", Habitat.CAVE, "zombie"),
            source("sniffer", Habitat.LAND, FoodSource.SNIFFER_GROUND),
            prey("spider", Habitat.CAVE, "bat"),
            source("strider", Habitat.LAVA, FoodSource.LAVA_ENVIRONMENT),
            source("trader_llama", Habitat.LAND, FoodSource.CARAVAN_FODDER),
            source("turtle", Habitat.WETLAND, FoodSource.SEAGRASS),
            source("villager", Habitat.LAND, FoodSource.VILLAGE_STORAGE),
            prey("wolf", Habitat.LAND, "sheep"),
            prey("zombie", Habitat.LAND, "cow"),
            prey("zombie_villager", Habitat.LAND, "cow"),
            prey("zombified_piglin", Habitat.NETHER, "strider")
    );

    private RetoldHungerSurvivalGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> coverageEnvironment =
                event.registerEnvironment(
                        id("isolated_hunger_survival_coverage"),
                        new TestEnvironmentDefinition.AllOf()
                );

        registerTest(
                event,
                coverageEnvironment,
                "hunger_survival_matrix_covers_every_profile",
                80,
                RetoldHungerSurvivalGameTests::assertEveryProfileIsCovered
        );

        for (SurvivalCase survivalCase : CASES) {
            Holder<TestEnvironmentDefinition<?>> environment =
                    event.registerEnvironment(
                            id("isolated_hunger_survival_" + survivalCase.mobPath),
                            new TestEnvironmentDefinition.AllOf()
                    );

            registerTest(
                    event,
                    environment,
                    "hunger_survival_" + survivalCase.mobPath,
                    TEST_TIMEOUT_TICKS,
                    helper -> runSurvivalCase(helper, survivalCase)
            );
        }
    }

    private static void assertEveryProfileIsCovered(GameTestHelper helper) {
        Set<String> expected = new HashSet<>();

        for (SurvivalCase survivalCase : CASES) {
            helper.assertTrue(
                    expected.add(survivalCase.mobPath),
                    "The hunger-survival matrix must not duplicate minecraft:"
                            + survivalCase.mobPath
            );
        }

        Set<String> actual = new HashSet<>();

        for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
            RetoldMobProfile profile = RetoldMobProfiles.get(entityType);

            if (profile.managed() && profile.hungerIntervalTicks() > 0) {
                actual.add(BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath());
            }
        }

        helper.assertTrue(
                actual.equals(expected),
                "Every managed positive-hunger profile needs one natural survival case; missing="
                        + difference(actual, expected)
                        + ", stale="
                        + difference(expected, actual)
        );
        helper.succeed();
    }

    private static Set<String> difference(
            Set<String> first,
            Set<String> second
    ) {
        Set<String> result = new HashSet<>(first);
        result.removeAll(second);
        return result;
    }

    private static void runSurvivalCase(
            GameTestHelper helper,
            SurvivalCase survivalCase
    ) {
        buildHabitat(helper, survivalCase.habitat);
        helper.setTime(isNocturnal(survivalCase.mobPath) ? 18_000L : 6_000L);

        List<Entity> fixtures = new ArrayList<>();
        Mob subject = spawnMob(
                helper,
                survivalCase.mobPath,
                subjectPosition(helper, survivalCase)
        );
        stabilize(subject);
        ServerPlayer observer = makeLoadedWorldObserver(helper);

        RetoldMobProfile profile = RetoldMobProfiles.get(subject);
        helper.assertTrue(
                profile.managed() && profile.hungerIntervalTicks() > 0,
                "The survival case must target a loaded positive-hunger profile: minecraft:"
                        + survivalCase.mobPath
        );

        long startedAt = helper.getLevel().getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(subject, startedAt);
        state.setHunger(INITIAL_HUNGER);
        state.markHungerTick(startedAt);

        placeFoodSource(helper, survivalCase, subject, fixtures);

        if (subject instanceof Villager villager) {
            /*
             * Vanilla may discard an artificial HOME memory before the first
             * staggered Retold food tick. Keep the test village anchor valid so
             * this case measures communal-food acquisition rather than POI
             * validation timing.
             */
            helper.onEachTick(() -> setVillageHome(helper, villager));
            RetoldVillagerCommunalFood.tick(
                    helper.getLevel(),
                    villager,
                    startedAt
            );
        }

        if (survivalCase.foodSource == FoodSource.LIVE_PREY
                && subject instanceof net.minecraft.world.entity.PathfinderMob hunter
                && RetoldMobRules.canUseNaturalPreyHuntingSystems(hunter)) {
            helper.assertTrue(
                    RetoldControlledHuntingEvents.tryStartHunt(
                            helper.getLevel(),
                            hunter,
                            startedAt
                    ),
                    "The natural survival case must be able to start a production hunt for minecraft:"
                            + survivalCase.mobPath
            );
        }

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    subject.isAlive() && !subject.isRemoved(),
                    "minecraft:" + survivalCase.mobPath
                            + " must remain alive while a valid natural food opportunity exists"
            );
            helper.assertTrue(
                    state.hunger() < INITIAL_HUNGER,
                    "minecraft:" + survivalCase.mobPath
                            + " must actually eat and lower hunger through "
                            + survivalCase.foodSource
                            + "; hunger="
                            + state.hunger()
                            + ", lastAteAt="
                            + state.lastAteAt()
                            + ", lastSuccessfulHuntAt="
                            + state.lastSuccessfulHuntAt()
                            + ", pos="
                            + subject.blockPosition()
                            + ", control="
                            + RetoldAiControl.getMode(subject)
                            + "/"
                            + RetoldAiControl.getOwner(subject)
                            + ", target="
                            + (subject.getTarget() == null
                            ? "none"
                            : subject.getTarget().getType())
                            + ", navigationDone="
                            + subject.getNavigation().isDone()
                            + ", navigationTarget="
                            + subject.getNavigation().getTargetPos()
                            + ", fixtures="
                            + fixtureSummary(fixtures)
            );
            assertRenewableSourcePreserved(helper, survivalCase);

            cleanup(subject, fixtures, observer);
        });
    }

    private static void assertRenewableSourcePreserved(
            GameTestHelper helper,
            SurvivalCase survivalCase
    ) {
        BlockPos relativePos;
        Block expected;

        switch (survivalCase.foodSource) {
            case DESERT_BROWSE -> {
                relativePos = new BlockPos(5, 2, 5);
                expected = Blocks.DEAD_BUSH;
            }
            case ALPINE_FORAGE -> {
                relativePos = new BlockPos(5, 1, 5);
                expected = Blocks.SNOW_BLOCK;
            }
            case MYCELIUM -> {
                relativePos = new BlockPos(5, 1, 5);
                expected = Blocks.MYCELIUM;
            }
            case GRUB_BADLANDS -> {
                relativePos = new BlockPos(5, 1, 5);
                expected = Blocks.RED_SAND;
            }
            case LAVA_ENVIRONMENT -> {
                relativePos = new BlockPos(3, 1, 5);
                expected = Blocks.LAVA;
            }
            default -> {
                return;
            }
        }

        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(relativePos)).is(expected),
                survivalCase.foodSource + " must sustain the mob without consuming its habitat"
        );
    }

    private static void buildHabitat(
            GameTestHelper helper,
            Habitat habitat
    ) {
        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 14; z++) {
                for (int y = 1; y <= 10; y++) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }

        for (int x = 0; x <= 14; x++) {
            for (int z = 0; z <= 10; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
                helper.setBlock(x, 7, z, Blocks.STONE);
            }
        }

        for (int y = 2; y <= 6; y++) {
            for (int x = 0; x <= 14; x++) {
                helper.setBlock(x, y, 0, Blocks.STONE);
                helper.setBlock(x, y, 10, Blocks.STONE);
            }

            for (int z = 1; z < 10; z++) {
                helper.setBlock(0, y, z, Blocks.STONE);
                helper.setBlock(14, y, z, Blocks.STONE);
            }
        }

        if (habitat == Habitat.AQUATIC) {
            fillWater(helper, 1, 13, 1, 9, 2, 5);
        } else if (habitat == Habitat.WETLAND) {
            for (int x = 1; x <= 8; x++) {
                for (int z = 1; z <= 9; z++) {
                    helper.setBlock(x, 1, z, Blocks.SAND);
                }
            }

            fillWater(helper, 1, 8, 1, 9, 2, 3);
        } else if (habitat == Habitat.LAVA) {
            for (int x = 2; x <= 5; x++) {
                for (int z = 4; z <= 7; z++) {
                    helper.setBlock(x, 1, z, Blocks.LAVA);
                }
            }
        } else if (habitat == Habitat.DESERT) {
            fillGroundPatch(helper, Blocks.SAND);
        } else if (habitat == Habitat.BADLANDS) {
            fillGroundPatch(helper, Blocks.RED_SAND);
        } else if (habitat == Habitat.MOUNTAIN) {
            fillGroundPatch(helper, Blocks.SNOW_BLOCK);
        } else if (habitat == Habitat.MUSHROOM_FIELDS) {
            fillGroundPatch(helper, Blocks.MYCELIUM);
        } else if (habitat == Habitat.NETHER) {
            fillGroundPatch(helper, Blocks.NETHERRACK);
        }
    }

    private static void fillWater(
            GameTestHelper helper,
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            int minY,
            int maxY
    ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    helper.setBlock(x, y, z, Blocks.WATER);
                }
            }
        }
    }

    private static void placeFoodSource(
            GameTestHelper helper,
            SurvivalCase survivalCase,
            Mob subject,
            List<Entity> fixtures
    ) {
        switch (survivalCase.foodSource) {
            case DROPPED_ITEM -> fixtures.add(spawnItem(
                    helper,
                    survivalCase.sourcePath,
                    foodPosition(helper, survivalCase)
            ));
            case LIVE_PREY -> fixtures.add(spawnPrey(
                    helper,
                    subject,
                    survivalCase.sourcePath,
                    foodPosition(helper, survivalCase)
            ));
            case GRASS -> fillGroundPatch(helper, Blocks.GRASS_BLOCK);
            case SMALL_PLANT, FLOWER -> fillFlowerPatch(helper);
            case CRIMSON_FUNGUS -> {
                helper.setBlock(5, 1, 5, Blocks.CRIMSON_NYLIUM);
                helper.setBlock(5, 2, 5, Blocks.CRIMSON_FUNGUS);
            }
            case WARPED_FUNGUS -> {
                helper.setBlock(5, 1, 5, Blocks.WARPED_NYLIUM);
                helper.setBlock(5, 2, 5, Blocks.WARPED_FUNGUS);
            }
            case LAVA_ENVIRONMENT -> {
                // The habitat itself is the non-consumable food source.
            }
            case GRUB_SOIL -> fillGroundPatch(helper, Blocks.DIRT);
            case GRUB_BADLANDS -> fillGroundPatch(helper, Blocks.RED_SAND);
            case DESERT_BROWSE -> fillDesertBrowsePatch(helper);
            case ALPINE_FORAGE -> fillGroundPatch(helper, Blocks.SNOW_BLOCK);
            case MYCELIUM -> fillGroundPatch(helper, Blocks.MYCELIUM);
            case NETHER_MUSHROOM -> {
                helper.setBlock(5, 1, 5, Blocks.NETHERRACK);
                helper.setBlock(5, 2, 5, Blocks.BROWN_MUSHROOM);
            }
            case AMBIENT_CAVE_INSECTS -> {
                // The production Bat colony behavior catches ambient cave insects.
            }
            case CARAVAN_FODDER -> placeCaravanFodder(helper, subject, fixtures);
            case BAMBOO -> {
                helper.setBlock(5, 1, 5, Blocks.DIRT);
                helper.setBlock(5, 2, 5, Blocks.BAMBOO);
            }
            case SNIFFER_GROUND -> fillGroundPatch(helper, Blocks.DIRT);
            case SEAGRASS -> helper.setBlock(5, 2, 5, Blocks.SEAGRASS);
            case VILLAGE_STORAGE -> placeVillageStorage(helper, subject);
        }
    }

    private static void fillGroundPatch(
            GameTestHelper helper,
            Block block
    ) {
        for (int x = 2; x <= 5; x++) {
            for (int z = 4; z <= 7; z++) {
                helper.setBlock(x, 1, z, block);
            }
        }
    }

    private static void fillFlowerPatch(GameTestHelper helper) {
        for (int x = 2; x <= 5; x++) {
            for (int z = 4; z <= 7; z++) {
                helper.setBlock(x, 1, z, Blocks.DIRT);
                helper.setBlock(x, 2, z, Blocks.DANDELION);
            }
        }
    }

    private static void fillDesertBrowsePatch(GameTestHelper helper) {
        for (int x = 2; x <= 12; x++) {
            for (int z = 2; z <= 8; z++) {
                helper.setBlock(x, 1, z, Blocks.SAND);
                helper.setBlock(x, 2, z, Blocks.DEAD_BUSH);
            }
        }
    }

    private static ItemEntity spawnItem(
            GameTestHelper helper,
            String itemPath,
            Vec3 position
    ) {
        Item item = BuiltInRegistries.ITEM.getValue(
                Identifier.withDefaultNamespace(itemPath)
        );
        ItemEntity itemEntity = new ItemEntity(
                helper.getLevel(),
                position.x(),
                position.y(),
                position.z(),
                new ItemStack(item)
        );
        itemEntity.setNeverPickUp();

        if (!helper.getLevel().addFreshEntity(itemEntity)) {
            throw new IllegalStateException("Could not add food item minecraft:" + itemPath);
        }

        return itemEntity;
    }

    private static LivingEntity spawnPrey(
            GameTestHelper helper,
            Mob hunter,
            String preyPath,
            Vec3 position
    ) {
        Mob prey = spawnMob(helper, preyPath, position);

        if (prey instanceof AbstractCubeMob cubeMob) {
            cubeMob.setSize(
                    RetoldMobRules.getEntityTypePath(hunter.getType()).equals("frog")
                            ? 2
                            : 1,
                    true
            );
        }

        if (RetoldMobRules.getEntityTypePath(hunter.getType()).equals("frog")) {
            prey.setNoAi(true);
        }

        if (hunter instanceof net.minecraft.world.entity.PathfinderMob pathfinderMob
                && (RetoldMobRules.canUseNaturalPreyHuntingSystems(pathfinderMob)
                || RetoldMobRules.isUndeadHungry(pathfinderMob)
                || RetoldMobRules.isSlimeHungry(pathfinderMob))) {
            prey.setNoAi(true);
            prey.setHealth(Math.min(1.0F, prey.getMaxHealth()));
        }

        return prey;
    }

    private static Mob spawnMob(
            GameTestHelper helper,
            String mobPath,
            Vec3 position
    ) {
        ServerLevel level = helper.getLevel();
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.withDefaultNamespace(mobPath)
        );
        Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);

        if (!(entity instanceof Mob mob)) {
            throw new IllegalStateException("Expected Mob entity minecraft:" + mobPath);
        }

        mob.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        mob.setPersistenceRequired();
        mob.setSilent(true);

        if (!level.addFreshEntity(mob)) {
            throw new IllegalStateException("Could not add mob minecraft:" + mobPath);
        }

        return mob;
    }

    private static void stabilize(Mob mob) {
        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }

        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }

        if (mob instanceof AbstractCubeMob cubeMob) {
            cubeMob.setSize(1, true);
        }
    }

    private static void placeVillageStorage(
            GameTestHelper helper,
            Mob subject
    ) {
        if (!(subject instanceof Villager villager)) {
            throw new IllegalStateException("Village storage requires a Villager subject");
        }

        BlockPos home = new BlockPos(3, 2, 5);
        BlockPos chestPos = new BlockPos(4, 2, 5);
        helper.setBlock(chestPos, Blocks.CHEST);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(
                helper.absolutePos(chestPos)
        );

        if (!(blockEntity instanceof Container container)) {
            throw new IllegalStateException("Expected communal chest at " + chestPos);
        }

        container.setItem(0, new ItemStack(
                BuiltInRegistries.ITEM.getValue(
                        Identifier.withDefaultNamespace("bread")
                ),
                4
        ));
        container.setChanged();
        villager.getBrain().setMemory(
                MemoryModuleType.HOME,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(home)
                )
        );
    }

    private static void setVillageHome(
            GameTestHelper helper,
            Villager villager
    ) {
        villager.getBrain().setMemory(
                MemoryModuleType.HOME,
                GlobalPos.of(
                        helper.getLevel().dimension(),
                        helper.absolutePos(new BlockPos(3, 2, 5))
                )
        );
    }

    private static void placeCaravanFodder(
            GameTestHelper helper,
            Mob subject,
            List<Entity> fixtures
    ) {
        if (!RetoldMobRules.isEntityPath(subject, "trader_llama")) {
            throw new IllegalStateException("Caravan fodder requires a Trader Llama subject");
        }

        Mob traderMob = spawnMob(
                helper,
                "wandering_trader",
                helper.absoluteVec(new Vec3(4.5D, 2.0D, 5.5D))
        );

        if (!(traderMob instanceof WanderingTrader trader)) {
            throw new IllegalStateException("Expected a Wandering Trader caravan holder");
        }

        subject.setLeashedTo(trader, true);
        fixtures.add(trader);
    }

    private static Vec3 subjectPosition(
            GameTestHelper helper,
            SurvivalCase survivalCase
    ) {
        if (survivalCase.mobPath.equals("frog")) {
            return helper.absoluteVec(new Vec3(7.5D, 3.0D, 5.5D));
        }

        double y = survivalCase.habitat == Habitat.AQUATIC
                || survivalCase.habitat == Habitat.WETLAND
                ? 3.0D
                : survivalCase.habitat == Habitat.CAVE ? 4.0D : 2.0D;
        return helper.absoluteVec(new Vec3(3.5D, y, 5.5D));
    }

    private static Vec3 foodPosition(
            GameTestHelper helper,
            SurvivalCase survivalCase
    ) {
        if (survivalCase.mobPath.equals("frog")) {
            return helper.absoluteVec(new Vec3(9.5D, 2.0D, 5.5D));
        }

        double y = survivalCase.habitat == Habitat.AQUATIC
                ? 3.0D
                : survivalCase.habitat == Habitat.WETLAND ? 3.0D : 2.0D;
        return helper.absoluteVec(new Vec3(5.5D, y, 5.5D));
    }

    private static boolean isNocturnal(String mobPath) {
        return mobPath.equals("bat")
                || mobPath.equals("cave_spider")
                || mobPath.equals("spider");
    }

    private static void cleanup(
            Mob subject,
            List<Entity> fixtures,
            ServerPlayer observer
    ) {
        RetoldMobStates.remove(subject);
        subject.discard();

        for (Entity fixture : fixtures) {
            if (fixture instanceof Mob mob) {
                RetoldMobStates.remove(mob);
            }

            fixture.discard();
        }

        subject.level().players().remove(observer);
        observer.discard();
    }

    private static ServerPlayer makeLoadedWorldObserver(GameTestHelper helper) {
        ServerPlayer observer = (ServerPlayer) helper.makeMockServerPlayer(
                GameType.CREATIVE
        );
        BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));
        observer.snapTo(
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        return observer;
    }

    private static String fixtureSummary(List<Entity> fixtures) {
        return fixtures.stream()
                .map(entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                        + "@"
                        + entity.blockPosition()
                        + ":alive="
                        + (!(entity instanceof LivingEntity living) || living.isAlive())
                        + ":removed="
                        + entity.isRemoved())
                .toList()
                .toString();
    }

    private static SurvivalCase item(
            String mobPath,
            Habitat habitat,
            String itemPath
    ) {
        return new SurvivalCase(
                mobPath,
                habitat,
                FoodSource.DROPPED_ITEM,
                itemPath
        );
    }

    private static SurvivalCase prey(
            String mobPath,
            Habitat habitat,
            String preyPath
    ) {
        return new SurvivalCase(
                mobPath,
                habitat,
                FoodSource.LIVE_PREY,
                preyPath
        );
    }

    private static SurvivalCase source(
            String mobPath,
            Habitat habitat,
            FoodSource foodSource
    ) {
        return new SurvivalCase(mobPath, habitat, foodSource, "");
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            int timeoutTicks,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        TEST_STRUCTURE,
                        timeoutTicks,
                        0,
                        true
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private record SurvivalCase(
            String mobPath,
            Habitat habitat,
            FoodSource foodSource,
            String sourcePath
    ) {
    }

    private enum Habitat {
        LAND,
        AQUATIC,
        WETLAND,
        LAVA,
        CAVE,
        DESERT,
        BADLANDS,
        MOUNTAIN,
        MUSHROOM_FIELDS,
        NETHER
    }

    private enum FoodSource {
        DROPPED_ITEM,
        LIVE_PREY,
        GRASS,
        SMALL_PLANT,
        FLOWER,
        CRIMSON_FUNGUS,
        WARPED_FUNGUS,
        LAVA_ENVIRONMENT,
        GRUB_SOIL,
        GRUB_BADLANDS,
        DESERT_BROWSE,
        ALPINE_FORAGE,
        MYCELIUM,
        NETHER_MUSHROOM,
        AMBIENT_CAVE_INSECTS,
        CARAVAN_FODDER,
        BAMBOO,
        SNIFFER_GROUND,
        SEAGRASS,
        VILLAGE_STORAGE
    }

    private static final class InlineGameTest extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> test;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> test
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }
    }
}
