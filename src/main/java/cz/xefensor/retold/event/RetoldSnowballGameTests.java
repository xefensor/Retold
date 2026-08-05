package cz.xefensor.retold.event;

import cz.xefensor.retold.Retold;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldSnowballGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final float DAMAGE_EPSILON = 0.001F;

    private RetoldSnowballGameTests() {
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
                id("snowballs_deal_one_damage"),
                new InlineGameTest(
                        testData,
                        RetoldSnowballGameTests::snowballsDealOneDamage
                )
        );
    }

    private static void snowballsDealOneDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var snowGolem = helper.spawn(EntityTypes.SNOW_GOLEM, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 1);
        var blaze = helper.spawn(EntityTypes.BLAZE, 3, 2, 1);
        var creeper = helper.spawn(EntityTypes.CREEPER, 4, 2, 1);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Snowball golemSnowball = new Snowball(
                level,
                snowGolem,
                new ItemStack(Items.SNOWBALL)
        );
        Snowball playerSnowball = new Snowball(
                level,
                player,
                new ItemStack(Items.SNOWBALL)
        );
        Snowball ownerlessSnowball = new Snowball(
                level,
                2.0,
                2.0,
                2.0,
                new ItemStack(Items.SNOWBALL)
        );

        try {
            float cowHealth = cow.getHealth();
            cow.hurtServer(
                    level,
                    level.damageSources().thrown(golemSnowball, snowGolem),
                    0.0F
            );
            assertDamage(
                    helper,
                    cowHealth - cow.getHealth(),
                    1.0F,
                    "A Snow Golem snowball must deal one damage to an ordinary target"
            );

            float blazeHealth = blaze.getHealth();
            blaze.hurtServer(
                    level,
                    level.damageSources().thrown(golemSnowball, snowGolem),
                    3.0F
            );
            assertDamage(
                    helper,
                    blazeHealth - blaze.getHealth(),
                    3.0F,
                    "The vanilla three-damage Blaze interaction must remain unchanged"
            );

            float playerShotHealth = cow.getHealth();
            cow.invulnerableTime = 0;
            cow.hurtServer(
                    level,
                    level.damageSources().thrown(playerSnowball, player),
                    0.0F
            );
            assertDamage(
                    helper,
                    playerShotHealth - cow.getHealth(),
                    1.0F,
                    "Player-thrown snowballs must deal one damage"
            );

            float ownerlessShotHealth = cow.getHealth();
            cow.invulnerableTime = 0;
            cow.hurtServer(
                    level,
                    level.damageSources().thrown(ownerlessSnowball, null),
                    0.0F
            );
            assertDamage(
                    helper,
                    ownerlessShotHealth - cow.getHealth(),
                    1.0F,
                    "Ownerless or dispenser-style snowballs must deal one damage"
            );

            float creeperHealth = creeper.getHealth();
            creeper.hurtServer(
                    level,
                    level.damageSources().thrown(golemSnowball, snowGolem),
                    0.0F
            );
            assertDamage(
                    helper,
                    creeperHealth - creeper.getHealth(),
                    1.0F,
                    "Snow Golem snowballs must damage Creepers when they hit"
            );

            float playerShotCreeperHealth = creeper.getHealth();
            creeper.invulnerableTime = 0;
            creeper.hurtServer(
                    level,
                    level.damageSources().thrown(playerSnowball, player),
                    0.0F
            );
            assertDamage(
                    helper,
                    playerShotCreeperHealth - creeper.getHealth(),
                    1.0F,
                    "Player snowballs must also damage Creepers"
            );
            helper.succeed();
        } finally {
            golemSnowball.discard();
            playerSnowball.discard();
            ownerlessSnowball.discard();
            snowGolem.discard();
            cow.discard();
            blaze.discard();
            creeper.discard();
            player.discard();
        }
    }

    private static void assertDamage(
            GameTestHelper helper,
            float actual,
            float expected,
            String message
    ) {
        helper.assertTrue(
                Math.abs(actual - expected) <= DAMAGE_EPSILON,
                message
        );
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
