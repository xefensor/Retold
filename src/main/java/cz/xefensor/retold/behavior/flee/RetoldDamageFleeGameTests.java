package cz.xefensor.retold.behavior.flee;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldDamageFleeGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldDamageFleeGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(
                id("passive_mobs_flee_every_successful_damage_source"),
                new InlineGameTest(
                        testData,
                        RetoldDamageFleeGameTests::passiveMobsFleeEverySuccessfulDamageSource
                )
        );
    }

    private static void passiveMobsFleeEverySuccessfulDamageSource(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        var sheep = helper.spawn(EntityTypes.SHEEP, 2, 2, 2);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 4);
        var pig = helper.spawn(EntityTypes.PIG, 2, 2, 6);
        var salmon = helper.spawn(EntityTypes.SALMON, 2, 2, 8);
        var wolf = helper.spawn(EntityTypes.WOLF, 5, 2, 6);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 2, 2);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPosition = helper.absoluteVec(new Vec3(5.5D, 2.0D, 4.5D));

        player.snapTo(
                playerPosition.x(),
                playerPosition.y(),
                playerPosition.z(),
                0.0F,
                0.0F
        );
        level.addFreshEntity(player);
        helper.setBlock(new BlockPos(2, 2, 8), Blocks.WATER);

        try {
            helper.assertTrue(
                    sheep.hurtServer(
                            level,
                            level.damageSources().mobAttack(zombie),
                            1.0F
                    ),
                    "The Zombie must deal real damage to the Sheep"
            );
            assertFleeing(helper, sheep, "A Sheep must flee a damaging Zombie");

            helper.assertTrue(
                    cow.hurtServer(
                            level,
                            level.damageSources().playerAttack(player),
                            1.0F
                    ),
                    "The Player must deal real damage to the Cow"
            );
            assertFleeing(helper, cow, "A Cow must flee a damaging Player");

            helper.assertTrue(
                    pig.hurtServer(
                            level,
                            level.damageSources().generic(),
                            1.0F
                    ),
                    "Source-less damage must reduce the Pig's health"
            );
            assertFleeing(helper, pig, "A Pig must panic after source-less damage");

            helper.assertTrue(
                    salmon.hurtServer(
                            level,
                            level.damageSources().generic(),
                            1.0F
                    ),
                    "Source-less damage must reduce the Salmon's health"
            );
            assertFleeing(helper, salmon, "An ordinary fish must panic after damage");

            helper.assertTrue(
                    wolf.hurtServer(
                            level,
                            level.damageSources().mobAttack(zombie),
                            1.0F
                    ),
                    "The Zombie must deal real damage to the Wolf"
            );
            helper.assertFalse(
                    RetoldAiControl.isControlledAs(wolf, RetoldAiControlMode.FLEE),
                    "A combat-capable predator must retain its retaliation behavior after an ordinary hit"
            );
        } finally {
            level.players().remove(player);
            player.discard();
            cleanup(sheep);
            cleanup(cow);
            cleanup(pig);
            cleanup(salmon);
            cleanup(wolf);
            cleanup(zombie);
        }

        helper.succeed();
    }

    private static void assertFleeing(
            GameTestHelper helper,
            PathfinderMob mob,
            String message
    ) {
        helper.assertTrue(
                RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.FLEE)
                        && mob.isSprinting(),
                message
        );
    }

    private static void cleanup(PathfinderMob mob) {
        RetoldAiControl.clear(mob);
        RetoldMobStates.remove(mob);
        mob.discard();
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
