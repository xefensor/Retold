package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldStarvationGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldStarvationGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        registerTest(
                event,
                "starvation_damages_every_hunger_tick_owner",
                RetoldStarvationGameTests::damagesEveryHungerTickOwner
        );
        registerTest(
                event,
                "starvation_kills_and_ignores_non_hunger_mobs",
                RetoldStarvationGameTests::killsAndIgnoresNonHungerMobs
        );
    }

    private static void damagesEveryHungerTickOwner(GameTestHelper helper) {
        assertEveryHungerProfileHasAnOwner(helper);
        placeFloor(helper);
        helper.setTime(18_000L);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 2);
        var bat = helper.spawn(EntityTypes.BAT, 4, 3, 2);
        var villager = helper.spawn(EntityTypes.VILLAGER, 6, 2, 2);
        long gameTime = helper.getLevel().getGameTime();
        RetoldMobState cowState = prepareForNextHungerTick(cow, gameTime, 99);
        RetoldMobState batState = prepareForNextHungerTick(bat, gameTime, 99);
        RetoldMobState villagerState = prepareForNextHungerTick(
                villager,
                gameTime,
                99
        );
        float cowHealth = cow.getHealth();
        float batHealth = bat.getHealth();
        float villagerHealth = villager.getHealth();

        helper.succeedWhen(() -> {
            helper.assertValueEqual(
                    cowState.hunger(),
                    RetoldStarvationBehavior.CRITICAL_HUNGER,
                    "A PathfinderMob must reach critical hunger on its metabolism tick"
            );
            helper.assertValueEqual(
                    batState.hunger(),
                    RetoldStarvationBehavior.CRITICAL_HUNGER,
                    "A Bat must reach critical hunger through its separate hunger owner"
            );
            helper.assertValueEqual(
                    villagerState.hunger(),
                    RetoldStarvationBehavior.CRITICAL_HUNGER,
                    "A Villager must reach critical hunger through its communal-food owner"
            );
            helper.assertValueEqual(
                    cow.getHealth(),
                    cowHealth - RetoldStarvationBehavior.DAMAGE_PER_HUNGER_INTERVAL,
                    "Critical hunger must damage ordinary hunger-aware PathfinderMobs"
            );
            helper.assertValueEqual(
                    bat.getHealth(),
                    batHealth - RetoldStarvationBehavior.DAMAGE_PER_HUNGER_INTERVAL,
                    "Critical hunger must damage hunger-aware non-Pathfinder Bats"
            );
            helper.assertValueEqual(
                    villager.getHealth(),
                    villagerHealth - RetoldStarvationBehavior.DAMAGE_PER_HUNGER_INTERVAL,
                    "Critical hunger must damage hunger-aware Villagers"
            );
        });
    }

    private static void assertEveryHungerProfileHasAnOwner(
            GameTestHelper helper
    ) {
        int hungerProfiles = 0;

        for (var entityType : BuiltInRegistries.ENTITY_TYPE) {
            RetoldMobProfile profile = RetoldMobProfiles.get(entityType);

            if (!profile.managed() || profile.hungerIntervalTicks() <= 0) {
                continue;
            }

            hungerProfiles++;
            boolean batOwner = entityType == EntityTypes.BAT
                    && profile.is(RetoldMobProfileType.BAT_COLONY);
            boolean villagerOwner = entityType == EntityTypes.VILLAGER
                    && profile.is(RetoldMobProfileType.VILLAGER_COMMUNAL);

            helper.assertTrue(
                    RetoldMobRules.canUseOrdinaryLifeSystems(entityType)
                            || batOwner
                            || villagerOwner,
                    "Every positive-hunger profile must reach an implemented hunger owner: "
                            + BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
            );
        }

        helper.assertTrue(
                hungerProfiles > 0,
                "The loaded profile set must contain hunger-aware mobs"
        );
    }

    private static void killsAndIgnoresNonHungerMobs(GameTestHelper helper) {
        placeFloor(helper);
        helper.setTime(18_000L);
        var chicken = helper.spawn(EntityTypes.CHICKEN, 2, 2, 2);
        var skeleton = helper.spawn(EntityTypes.SKELETON, 5, 2, 2);
        long gameTime = helper.getLevel().getGameTime();
        prepareForNextHungerTick(
                chicken,
                gameTime,
                RetoldStarvationBehavior.CRITICAL_HUNGER
        );
        RetoldMobState skeletonState = RetoldMobStates.getOrCreate(
                skeleton,
                gameTime
        );
        skeletonState.setHunger(RetoldStarvationBehavior.CRITICAL_HUNGER);
        skeletonState.markHungerTick(gameTime - 1_000L);
        chicken.setHealth(RetoldStarvationBehavior.DAMAGE_PER_HUNGER_INTERVAL);
        float skeletonHealth = skeleton.getHealth();

        helper.succeedWhen(() -> {
            helper.assertFalse(
                    chicken.isAlive(),
                    "Critical hunger must be able to kill a hunger-aware mob; health="
                            + chicken.getHealth()
                            + ", hunger="
                            + RetoldMobStates.get(chicken).hunger()
                            + ", lastHungerTickAt="
                            + RetoldMobStates.get(chicken).lastHungerTickAt()
                            + ", gameTime="
                            + helper.getLevel().getGameTime()
            );
            helper.assertValueEqual(
                    skeleton.getHealth(),
                    skeletonHealth,
                    "A profile with no hunger interval must not receive starvation damage"
            );
        });
    }

    private static RetoldMobState prepareForNextHungerTick(
            Mob mob,
            long gameTime,
            int hunger
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(mob, gameTime);
        int interval = RetoldMobRules.hungerInterval(mob);

        if (interval <= 0) {
            throw new IllegalArgumentException("Test mob must have an active hunger interval");
        }

        state.setHunger(hunger);
        state.markHungerTick(gameTime - interval);
        return state;
    }

    private static void placeFloor(GameTestHelper helper) {
        for (int x = 0; x <= 7; x++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
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
                new TestData<>(environment, EMPTY_STRUCTURE, 220, 0, true);

        event.registerTest(id(name), new InlineGameTest(testData, test));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
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
