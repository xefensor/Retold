package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldControlledCombatEvents;
import cz.xefensor.retold.behavior.hunting.RetoldControlledHuntingEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldSpiderEcologyGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldSpiderEcologyGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("isolated_spider_clock"),
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
                id("hungry_spiders_hunt_adult_animals_only_at_night"),
                new InlineGameTest(
                        testData,
                        RetoldSpiderEcologyGameTests::hungrySpidersHuntAdultAnimalsOnlyAtNight
                )
        );
    }

    private static void hungrySpidersHuntAdultAnimalsOnlyAtNight(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BrightSpider hunter = spawnBrightSpider(helper, new Vec3(1.5D, 2.0D, 1.5D));
        var recruit = helper.spawn(EntityTypes.CAVE_SPIDER, 2, 2, 1);
        var adultCow = helper.spawn(EntityTypes.COW, 4, 2, 1);
        BrightSpider playerTestSpider = spawnBrightSpider(helper, new Vec3(1.5D, 2.0D, 5.5D));
        Player player = spawnPlayer(helper, new Vec3(3.5D, 2.0D, 5.5D));
        long gameTime = level.getGameTime() + 3_000L;
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        long originalClockTime = level.getServer().clockManager().getTotalTicks(clock);

        RetoldMobStates.getOrCreate(hunter, gameTime).setHunger(100);
        RetoldMobStates.getOrCreate(recruit, gameTime).setHunger(100);

        try {
            helper.assertTrue(
                    !adultCow.isBaby()
                            && hunter.getLightLevelDependentMagicValue() >= 0.5F,
                    "The Spider ecology test must use an adult animal and a brightness-neutral hunter"
            );
            helper.assertTrue(
                    RetoldMobRules.canHuntPrey(hunter, adultCow, gameTime),
                    "A hungry Spider must consider a full-sized passive animal valid food"
            );
            setClockTime(level, clock, 6_000L);
            helper.assertFalse(
                    RetoldControlledHuntingEvents.tryStartHunt(
                            level,
                            hunter,
                            gameTime
                    ),
                    "A hungry Spider must not start an animal hunt during the day"
            );
            helper.assertTrue(
                    hunter.getTarget() == null,
                    "Daytime hunt rejection must not leave an animal target"
            );

            setClockTime(level, clock, 18_000L);
            helper.assertTrue(
                    RetoldControlledHuntingEvents.tryStartHunt(
                            level,
                            hunter,
                            gameTime + 1L
                    ),
                    "Night must allow a hungry Spider to start an adult-animal hunt"
            );
            helper.assertTrue(
                    hunter.getTarget() == adultCow
                            && RetoldAiControl.isControlledAs(
                            hunter,
                            RetoldAiControlMode.HUNT
                    )
                            && RetoldFactionTargetMemory.isOwnedByAny(
                            hunter,
                            adultCow,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The initiating Spider must own its adult-animal hunt target"
            );

            RetoldSwarmScavengerEvents.tickSpiderSwarm(
                    level,
                    recruit,
                    gameTime + 1L
            );
            helper.assertTrue(
                    recruit.getTarget() == adultCow
                            && RetoldAiControl.isControlledAsBy(
                            recruit,
                            RetoldAiControlMode.HUNT,
                            RetoldAiControlOwner.SWARM
                    )
                            && RetoldFactionTargetMemory.isOwnedByAny(
                            recruit,
                            adultCow,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "A hungry nearby Cave Spider must be able to join the Spider's animal hunt"
            );

            setClockTime(level, clock, 6_000L);
            helper.assertTrue(
                    RetoldControlledHuntingEvents.endTimeRestrictedHuntIfNeeded(
                            level,
                            hunter
                    )
                            && RetoldControlledHuntingEvents.endTimeRestrictedHuntIfNeeded(
                            level,
                            recruit
                    ),
                    "Daytime must end both the initiating and recruited Spider hunts"
            );
            helper.assertTrue(
                    hunter.getTarget() == null
                            && recruit.getTarget() == null,
                    "Ending daytime Spider hunts must release their animal targets"
            );
            RetoldControlledCombatEvents.tickControlledCombat(
                    level,
                    playerTestSpider,
                    gameTime + 2L
            );
            helper.assertTrue(
                    playerTestSpider.getTarget() == null,
                    "Bright conditions must still suppress proactive Spider aggression toward players"
            );
        } finally {
            setClockTime(level, clock, originalClockTime);
            cleanup(level, hunter, recruit, adultCow, playerTestSpider, player);
        }

        helper.succeed();
    }

    private static void setClockTime(
            ServerLevel level,
            Holder<WorldClock> clock,
            long clockTime
    ) {
        level.getServer().clockManager().setTotalTicks(clock, clockTime);
    }

    private static BrightSpider spawnBrightSpider(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        BrightSpider spider = new BrightSpider(level);
        Vec3 position = helper.absoluteVec(relativePosition);

        spider.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        level.addFreshEntity(spider);
        return spider;
    }

    private static Player spawnPlayer(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(relativePosition);

        player.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        level.addFreshEntity(player);
        return player;
    }

    private static void cleanup(
            ServerLevel level,
            net.minecraft.world.entity.Mob hunter,
            net.minecraft.world.entity.Mob recruit,
            net.minecraft.world.entity.Mob prey,
            net.minecraft.world.entity.Mob playerTestSpider,
            Player player
    ) {
        RetoldAiControl.clear(hunter);
        RetoldAiControl.clear(recruit);
        RetoldAiControl.clear(playerTestSpider);
        level.players().remove(player);
        player.discard();
        hunter.discard();
        recruit.discard();
        prey.discard();
        playerTestSpider.discard();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class BrightSpider extends Spider {
        private BrightSpider(ServerLevel level) {
            super(EntityTypes.SPIDER, level);
        }

        @Override
        public float getLightLevelDependentMagicValue() {
            return 1.0F;
        }

        @Override
        public boolean hasLineOfSight(net.minecraft.world.entity.Entity target) {
            return true;
        }
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
