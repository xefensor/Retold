package cz.xefensor.retold.behavior.food;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.behavior.species.RetoldStriderLavaSustenance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class RetoldNaturalFoodAcquisitionGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final int INITIAL_HUNGER = 99;
    private static final int TEST_TIMEOUT_TICKS = 100;

    private RetoldNaturalFoodAcquisitionGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        registerIsolated(
                event,
                "natural_food_piglin_receives_hoglin_kill_meal",
                RetoldNaturalFoodAcquisitionGameTests::piglinReceivesHoglinKillMeal
        );
        registerIsolated(
                event,
                "natural_food_undead_receive_living_kill_meals",
                RetoldNaturalFoodAcquisitionGameTests::undeadReceiveLivingKillMeals
        );
        registerIsolated(
                event,
                "natural_food_cubes_receive_living_kill_meals",
                RetoldNaturalFoodAcquisitionGameTests::cubesReceiveLivingKillMeals
        );
        registerIsolated(
                event,
                "natural_food_kill_meals_respect_exclusions",
                RetoldNaturalFoodAcquisitionGameTests::killMealsRespectExclusions
        );
        registerIsolated(
                event,
                "natural_food_strider_is_sustained_by_lava",
                RetoldNaturalFoodAcquisitionGameTests::striderIsSustainedByLava
        );
    }

    private static void piglinReceivesHoglinKillMeal(GameTestHelper helper) {
        List<Mob> mobs = new ArrayList<>();

        try {
            PathfinderMob piglin = spawn(helper, "piglin", new Vec3(2.5D, 2.0D, 2.5D), mobs);
            PathfinderMob hoglin = spawn(helper, "hoglin", new Vec3(3.5D, 2.0D, 2.5D), mobs);
            RetoldMobState state = makeHungry(helper, piglin);

            kill(helper.getLevel(), piglin, hoglin);
            helper.assertTrue(
                    state.hunger() < INITIAL_HUNGER,
                    "A hungry Piglin must receive a meal from a Hoglin it kills"
            );
            helper.succeed();
        } finally {
            cleanup(mobs);
        }
    }

    private static void undeadReceiveLivingKillMeals(GameTestHelper helper) {
        List<Mob> mobs = new ArrayList<>();
        List<String> undead = List.of(
                "zombie",
                "zombie_villager",
                "husk",
                "drowned",
                "zombified_piglin"
        );

        try {
            for (int index = 0; index < undead.size(); index++) {
                PathfinderMob killer = spawn(
                        helper,
                        undead.get(index),
                        new Vec3(2.5D + index * 2.0D, 2.0D, 2.5D),
                        mobs
                );
                PathfinderMob victim = spawn(
                        helper,
                        "cow",
                        new Vec3(3.5D + index * 2.0D, 2.0D, 2.5D),
                        mobs
                );
                RetoldMobState state = makeHungry(helper, killer);

                kill(helper.getLevel(), killer, victim);
                helper.assertTrue(
                        state.hunger() < INITIAL_HUNGER,
                        "A hungry minecraft:" + undead.get(index)
                                + " must receive a meal from a non-undead victim it kills"
                );
            }

            helper.succeed();
        } finally {
            cleanup(mobs);
        }
    }

    private static void cubesReceiveLivingKillMeals(GameTestHelper helper) {
        List<Mob> mobs = new ArrayList<>();

        try {
            for (int index = 0; index < 2; index++) {
                String cubePath = index == 0 ? "slime" : "magma_cube";
                PathfinderMob cube = spawn(
                        helper,
                        cubePath,
                        new Vec3(2.5D + index * 4.0D, 2.0D, 2.5D),
                        mobs
                );
                PathfinderMob victim = spawn(
                        helper,
                        "cow",
                        new Vec3(3.5D + index * 4.0D, 2.0D, 2.5D),
                        mobs
                );
                RetoldMobState state = makeHungry(helper, cube);

                kill(helper.getLevel(), cube, victim);
                helper.assertTrue(
                        state.hunger() < INITIAL_HUNGER,
                        "A hungry minecraft:" + cubePath
                                + " must receive a meal from a non-Cube victim it kills"
                );
            }

            helper.succeed();
        } finally {
            cleanup(mobs);
        }
    }

    private static void killMealsRespectExclusions(GameTestHelper helper) {
        List<Mob> mobs = new ArrayList<>();

        try {
            PathfinderMob zombie = spawn(helper, "zombie", new Vec3(2.5D, 2.0D, 2.5D), mobs);
            PathfinderMob husk = spawn(helper, "husk", new Vec3(3.5D, 2.0D, 2.5D), mobs);
            RetoldMobState zombieState = makeHungry(helper, zombie);
            kill(helper.getLevel(), zombie, husk);
            helper.assertTrue(
                    zombieState.hunger() == INITIAL_HUNGER,
                    "Undead-on-undead kills must not count as food"
            );

            PathfinderMob slime = spawn(helper, "slime", new Vec3(6.5D, 2.0D, 2.5D), mobs);
            PathfinderMob magmaCube = spawn(helper, "magma_cube", new Vec3(7.5D, 2.0D, 2.5D), mobs);
            RetoldMobState slimeState = makeHungry(helper, slime);
            kill(helper.getLevel(), slime, magmaCube);
            helper.assertTrue(
                    slimeState.hunger() == INITIAL_HUNGER,
                    "Slime and Magma Cube family kills must not count as food"
            );

            PathfinderMob secondZombie = spawn(helper, "zombie", new Vec3(10.5D, 2.0D, 2.5D), mobs);
            PathfinderMob creeper = spawn(helper, "creeper", new Vec3(11.5D, 2.0D, 2.5D), mobs);
            RetoldMobState secondZombieState = makeHungry(helper, secondZombie);
            kill(helper.getLevel(), secondZombie, creeper);
            helper.assertTrue(
                    secondZombieState.hunger() == INITIAL_HUNGER,
                    "A Creeper death must never count as a hunger meal"
            );

            helper.succeed();
        } finally {
            cleanup(mobs);
        }
    }

    private static void striderIsSustainedByLava(GameTestHelper helper) {
        List<Mob> mobs = new ArrayList<>();

        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 5; z++) {
                helper.setBlock(x, 0, z, Blocks.STONE);

                if (x == 1 || x == 5 || z == 1 || z == 5) {
                    helper.setBlock(x, 1, z, Blocks.STONE);
                } else {
                    helper.setBlock(x, 1, z, Blocks.LAVA);
                }

                helper.setBlock(x, 2, z, Blocks.AIR);
            }
        }

        PathfinderMob strider = spawn(
                helper,
                "strider",
                new Vec3(3.5D, 2.0D, 3.5D),
                mobs
        );
        RetoldMobState state = makeHungry(helper, strider);
        long gameTime = helper.getLevel().getGameTime();

        helper.assertTrue(
                RetoldAiControl.tryClaim(
                        strider,
                        RetoldAiControlMode.SEARCH,
                        RetoldAiControlOwner.FOOD,
                        RetoldAiPriorities.SEARCH,
                        "search_for_food",
                        gameTime,
                        TEST_TIMEOUT_TICKS * 2
                ),
                "The fixture must begin with ordinary food-search ownership"
        );
        helper.assertTrue(
                RetoldStriderLavaSustenance.tick(
                        helper.getLevel(),
                        strider,
                        gameTime
                ),
                "The fixture Strider must be recognized as lava-sustained"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    state.hunger() < INITIAL_HUNGER,
                    "A Strider standing on lava must receive passive hunger relief"
            );
            helper.assertTrue(
                    RetoldAiControl.getMode(strider) == RetoldAiControlMode.NONE,
                    "Lava sustenance must stop an ordinary food search that would leave lava"
            );

            for (int x = 2; x <= 4; x++) {
                for (int z = 2; z <= 4; z++) {
                    helper.assertTrue(
                            helper.getLevel().getBlockState(
                                    helper.absolutePos(new BlockPos(x, 1, z))
                            ).is(Blocks.LAVA),
                            "Strider sustenance must not consume lava at " + x + ",1," + z
                    );
                }
            }

            cleanup(mobs);
        });
    }

    private static RetoldMobState makeHungry(
            GameTestHelper helper,
            PathfinderMob mob
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                helper.getLevel().getGameTime()
        );
        state.setHunger(INITIAL_HUNGER);
        return state;
    }

    private static void kill(
            ServerLevel level,
            PathfinderMob killer,
            PathfinderMob victim
    ) {
        victim.setHealth(Math.min(1.0F, victim.getMaxHealth()));

        if (!victim.hurtServer(
                level,
                level.damageSources().mobAttack(killer),
                100.0F
        )) {
            throw new IllegalStateException(
                    "Could not apply test kill from " + killer.getType() + " to " + victim.getType()
            );
        }
    }

    private static PathfinderMob spawn(
            GameTestHelper helper,
            String entityPath,
            Vec3 relativePosition,
            List<Mob> mobs
    ) {
        ServerLevel level = helper.getLevel();
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.withDefaultNamespace(entityPath)
        );
        Entity entity = type.create(level, EntitySpawnReason.COMMAND);

        if (!(entity instanceof PathfinderMob mob)) {
            throw new IllegalStateException("Expected PathfinderMob minecraft:" + entityPath);
        }

        Vec3 position = helper.absoluteVec(relativePosition);
        mob.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        mob.setNoAi(true);
        mob.setSilent(true);
        mob.setPersistenceRequired();

        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }

        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }

        if (mob instanceof AbstractCubeMob cubeMob) {
            cubeMob.setSize(1, true);
        }

        if (!level.addFreshEntity(mob)) {
            throw new IllegalStateException("Could not add minecraft:" + entityPath);
        }

        mobs.add(mob);
        return mob;
    }

    private static void cleanup(List<Mob> mobs) {
        for (Mob mob : mobs) {
            RetoldMobStates.remove(mob);
            mob.discard();
        }
    }

    private static void registerIsolated(
            RegisterGameTestsEvent event,
            String name,
            Consumer<GameTestHelper> test
    ) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_" + name),
                new TestEnvironmentDefinition.AllOf()
        );
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment,
                EMPTY_STRUCTURE,
                TEST_TIMEOUT_TICKS,
                0,
                true
        );
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
