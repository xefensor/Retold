package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.event.RetoldFactionAssistEvents;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.territory.RetoldTerritoryConfigs;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.raid.Raid;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldCommanderSupportGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldCommanderSupportGameTests() {
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
                id("witches_only_assist_illagers_in_same_active_raid"),
                new InlineGameTest(
                        testData,
                        RetoldCommanderSupportGameTests::witchesOnlyAssistInSameRaid
                )
        );
    }

    private static void witchesOnlyAssistInSameRaid(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var outsideCaller = helper.spawn(EntityTypes.PILLAGER, 1, 2, 1);
        var raidCaller = helper.spawn(EntityTypes.PILLAGER, 1, 2, 3);
        var witch = helper.spawn(EntityTypes.WITCH, 3, 2, 1);
        var target = helper.spawn(EntityTypes.ZOMBIE, 5, 2, 1);
        Raid raid = new Raid(witch.blockPosition(), level.getDifficulty());

        try {
            helper.assertTrue(
                    RetoldFactionMembers.getFaction(witch) == null,
                    "A witch must not be a full Illager faction member"
            );
            helper.assertTrue(
                    RetoldFactionMembers.isLooseAllyOf(
                            witch,
                            RetoldFaction.ILLAGERS
                    ),
                    "A witch must retain its loose-ally identity"
            );
            helper.assertFalse(
                    RetoldFactionMembers.isCombatAlignedWith(
                            witch,
                            RetoldFaction.ILLAGERS
                    ),
                    "A witch outside a raid must not be combat-aligned with Illagers"
            );
            helper.assertTrue(
                    RetoldTerritoryConfigs.getForEntity(witch) == null,
                    "A witch must not receive Illager territory membership"
            );
            helper.assertFalse(
                    RetoldFactionRelations.shouldAttack(witch, outsideCaller),
                    "A witch must normally ignore Illagers"
            );
            helper.assertFalse(
                    RetoldFactionRelations.shouldAttack(outsideCaller, witch),
                    "Illagers must normally ignore witches"
            );

            RetoldFactionAssistEvents.callForFactionHelp(
                    level,
                    outsideCaller,
                    target,
                    RetoldFaction.ILLAGERS
            );
            helper.assertTrue(
                    witch.getTarget() == null,
                    "An ordinary Illager help call must not recruit a witch outside a raid"
            );

            witch.setCurrentRaid(raid);
            raidCaller.setCurrentRaid(raid);

            helper.assertTrue(
                    RetoldFactionMembers.areCooperatingAllies(
                            witch,
                            raidCaller,
                            RetoldFaction.ILLAGERS
                    ),
                    "A witch and Illager in the same active raid must cooperate"
            );
            helper.assertFalse(
                    RetoldFactionMembers.areCooperatingAllies(
                            witch,
                            outsideCaller,
                            RetoldFaction.ILLAGERS
                    ),
                    "A raiding witch must not recruit an Illager outside its raid"
            );

            RetoldFactionAssistEvents.callForFactionHelp(
                    level,
                    raidCaller,
                    target,
                    RetoldFaction.ILLAGERS
            );
            helper.assertTrue(
                    witch.getTarget() == target,
                    "A witch must answer a same-raid Illager help call"
            );

            witch.setCurrentRaid(null);
            RetoldCommanderSupportEvents.tickSupportBehavior(
                    level,
                    witch,
                    level.getGameTime()
            );
            helper.assertTrue(
                    witch.getTarget() == null,
                    "A witch must release its Retold-owned assist target after leaving the raid"
            );
            helper.succeed();
        } finally {
            witch.setCurrentRaid(null);
            raidCaller.setCurrentRaid(null);
            outsideCaller.discard();
            raidCaller.discard();
            witch.discard();
            target.discard();
        }
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
