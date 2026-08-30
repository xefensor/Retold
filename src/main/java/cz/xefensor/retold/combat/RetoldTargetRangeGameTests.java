package cz.xefensor.retold.combat;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.event.RetoldFactionCombatEvents;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.territory.RetoldTerritoryBrainGuards;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.function.Consumer;

public final class RetoldTargetRangeGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldTargetRangeGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_mob_target_ranges"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "mob_target_ranges_are_species_aware_and_bounded",
                RetoldTargetRangeGameTests::mobTargetRangesAreSpeciesAwareAndBounded
        );
    }

    private static void mobTargetRangesAreSpeciesAwareAndBounded(
            GameTestHelper helper
    ) {
        RetoldWorldData worldData = RetoldWorldData.get(helper.getLevel());
        RetoldWorldStage originalStage = worldData.getStage();
        var farZombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 2);
        var farCow = helper.spawn(EntityTypes.COW, 32, 2, 2);
        var nearZombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 12);
        var nearCow = helper.spawn(EntityTypes.COW, 13, 2, 12);
        var playerZombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 30);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        var skeleton = helper.spawn(EntityTypes.SKELETON, 2, 2, 40);
        var skeletonTarget = helper.spawn(EntityTypes.COW, 23, 2, 40);
        var piglin = helper.spawn(EntityTypes.PIGLIN, 2, 2, 50);
        var brainTarget = helper.spawn(EntityTypes.COW, 27, 2, 50);
        var retaliationZombie = helper.spawn(EntityTypes.ZOMBIE, 2, 2, 60);
        var retaliationTarget = helper.spawn(EntityTypes.COW, 31, 2, 60);
        var hordeRecruit = helper.spawn(EntityTypes.HUSK, 23, 2, 70);
        var nearHordeRecruit = helper.spawn(EntityTypes.DROWNED, 21, 2, 70);
        var hordeTarget = helper.spawn(EntityTypes.COW, 4, 2, 70);
        var ghast = helper.spawn(EntityTypes.GHAST, 2, 12, 80);

        farZombie.setNoAi(true);
        farCow.setNoAi(true);
        nearZombie.setNoAi(true);
        nearCow.setNoAi(true);
        playerZombie.setNoAi(true);
        skeleton.setNoAi(true);
        skeletonTarget.setNoAi(true);
        piglin.setNoAi(true);
        brainTarget.setNoAi(true);
        retaliationZombie.setNoAi(true);
        retaliationTarget.setNoAi(true);
        hordeRecruit.setNoAi(true);
        nearHordeRecruit.setNoAi(true);
        hordeTarget.setNoAi(true);
        ghast.setNoAi(true);

        try {
            worldData.setStage(RetoldWorldStage.STAGE_1);

            RetoldFactionCombatEvents.onEntityTickPost(
                    new EntityTickEvent.Post(farZombie)
            );
            helper.assertTrue(
                    farZombie.getTarget() == null,
                    "Stage 1 Zombies must not inherit the generic 40-block faction target range"
            );

            RetoldFactionCombatEvents.onEntityTickPost(
                    new EntityTickEvent.Post(nearZombie)
            );
            helper.assertTrue(
                    nearZombie.getTarget() == nearCow,
                    "A visible non-Undead target inside the Stage 1 Zombie range must remain valid"
            );

            player.setPos(Vec3.atCenterOf(
                    helper.absolutePos(new BlockPos(32, 2, 30))
            ));
            playerZombie.setTarget(player);
            helper.assertTrue(
                    playerZombie.getTarget() == null,
                    "The same acquisition ceiling must apply to distant player targets"
            );

            player.setPos(Vec3.atCenterOf(
                    helper.absolutePos(new BlockPos(13, 2, 30))
            ));
            playerZombie.setTarget(player);
            helper.assertTrue(
                    playerZombie.getTarget() == player,
                    "A nearby Survival player must remain a valid Zombie target"
            );

            worldData.setStage(RetoldWorldStage.STAGE_2);
            skeleton.setTarget(skeletonTarget);
            helper.assertTrue(
                    skeleton.getTarget() == skeletonTarget,
                    "Stage 2 Skeletons must retain their deliberate 22-block awareness"
            );

            skeleton.setTarget(null);
            skeletonTarget.setPos(
                    Vec3.atCenterOf(helper.absolutePos(new BlockPos(25, 2, 40)))
            );
            skeleton.setTarget(skeletonTarget);
            helper.assertTrue(
                    skeleton.getTarget() == null,
                    "Stage 2 Skeletons must reject a new target beyond 22 blocks"
            );

            RetoldTerritoryBrainGuards.pushCurrentMob(piglin);
            try {
                piglin.getBrain().setMemory(
                        MemoryModuleType.ATTACK_TARGET,
                        brainTarget
                );
            } finally {
                RetoldTerritoryBrainGuards.popCurrentMob(piglin);
            }
            helper.assertTrue(
                    RetoldAiTargets.getBrainAttackTargetSafely(piglin) == null,
                    "Brain-backed mobs must reject targets beyond their species-aware range"
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            retaliationZombie,
                            retaliationTarget,
                            RetoldTargetSource.RETALIATION
                    ),
                    "Direct retaliation may use the Zombie's wider retention boundary"
            );
            helper.assertTrue(
                    !RetoldTargetRangePolicy.releaseTargetIfBeyondRetention(
                            retaliationZombie
                    ),
                    "A retaliation target inside the retention boundary must remain owned"
            );

            retaliationTarget.setPos(
                    Vec3.atCenterOf(helper.absolutePos(new BlockPos(33, 2, 60)))
            );
            helper.assertTrue(
                    RetoldTargetRangePolicy.releaseTargetIfBeyondRetention(
                            retaliationZombie
                    ) && retaliationZombie.getTarget() == null,
                    "Even retaliation must stop after its target passes the bounded chase range"
            );

            helper.assertTrue(
                    !RetoldCombatTargets.applyAttackTarget(
                            hordeRecruit,
                            hordeTarget,
                            RetoldTargetSource.FACTION_ASSIST
                    ) && hordeRecruit.getTarget() == null,
                    "A relayed horde target must not jump to a recruit that cannot perceive the victim"
            );
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            nearHordeRecruit,
                            hordeTarget,
                            RetoldTargetSource.FACTION_ASSIST
                    ) && nearHordeRecruit.getTarget() == hordeTarget,
                    "A relayed target inside the recruit's own range must remain valid"
            );

            helper.assertValueEqual(
                    RetoldTargetRangePolicy.maximumAcquisitionRange(ghast),
                    64.0D,
                    "A justified long-range specialist must retain its explicit range"
            );

            helper.succeed();
        } finally {
            worldData.setStage(originalStage);
            farZombie.discard();
            farCow.discard();
            nearZombie.discard();
            nearCow.discard();
            player.discard();
            playerZombie.discard();
            skeleton.discard();
            skeletonTarget.discard();
            piglin.discard();
            brainTarget.discard();
            retaliationZombie.discard();
            retaliationTarget.discard();
            hordeRecruit.discard();
            nearHordeRecruit.discard();
            hordeTarget.discard();
            ghast.discard();
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> function
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true),
                        function
                )
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
