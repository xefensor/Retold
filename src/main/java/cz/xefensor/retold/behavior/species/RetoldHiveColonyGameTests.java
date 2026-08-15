package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.function.Consumer;

public final class RetoldHiveColonyGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldHiveColonyGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_hive_colony_defense"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "bees_collectively_defend_harmed_colony_members",
                RetoldHiveColonyGameTests::collectivelyDefendHarmedColonyMembers
        );
        registerTest(
                event,
                environment,
                "bees_defend_hives_but_not_smoked_harvests",
                RetoldHiveColonyGameTests::defendHivesButNotSmokedHarvests
        );
    }

    private static void collectivelyDefendHarmedColonyMembers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Bee victim = helper.spawn(EntityTypes.BEE, 4, 3, 5);
        Bee recruit = helper.spawn(EntityTypes.BEE, 7, 3, 5);
        Bee busyBee = helper.spawn(EntityTypes.BEE, 8, 3, 7);
        Zombie attacker = helper.spawn(EntityTypes.ZOMBIE, 5, 3, 5);
        Drowned busyTarget = helper.spawn(EntityTypes.DROWNED, 9, 3, 7);
        long gameTime = level.getGameTime();

        try {
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            busyBee,
                            busyTarget,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The busy-Bee fixture must begin with an unrelated urgent target"
            );

            float healthBeforeDamage = victim.getHealth();
            helper.assertTrue(
                    victim.hurtServer(
                            level,
                            level.damageSources().mobAttack(attacker),
                            1.0F
                    ),
                    "The Zombie must deal real damage to the colony member"
            );
            helper.assertTrue(
                    victim.getHealth() < healthBeforeDamage,
                    "Collective defense must begin only after successful health damage"
            );
            assertDefense(
                    helper,
                    victim,
                    attacker,
                    RetoldTargetSource.RETALIATION,
                    "The harmed Bee must retain direct-retaliation ownership"
            );
            assertDefense(
                    helper,
                    recruit,
                    attacker,
                    RetoldTargetSource.FACTION_ASSIST,
                    "An available nearby Bee must join through colony assistance"
            );
            helper.assertTrue(
                    busyBee.getTarget() == busyTarget,
                    "A Bee with another live target must not be redirected"
            );

            attacker.discard();
            RetoldHiveColonyEvents.tick(level, victim, gameTime + 1L);
            RetoldHiveColonyEvents.tick(level, recruit, gameTime + 1L);

            helper.assertTrue(
                    victim.getTarget() == null,
                    "The harmed Bee's target must clear with the threat; found "
                            + victim.getTarget()
            );
            helper.assertTrue(
                    recruit.getTarget() == null,
                    "The assisting Bee's target must clear with the threat"
            );
            helper.assertFalse(
                    RetoldAiControl.isControlledBy(
                            victim,
                            RetoldAiControlOwner.HIVE_COLONY
                    ),
                    "The harmed Bee's colony ownership must clear with the threat"
            );
            helper.assertFalse(
                    RetoldAiControl.isControlledBy(
                            recruit,
                            RetoldAiControlOwner.HIVE_COLONY
                    ),
                    "The assisting Bee's colony ownership must clear with the threat"
            );
            helper.succeed();
        } finally {
            cleanup(victim, recruit, busyBee, attacker, busyTarget);
        }
    }

    private static void defendHivesButNotSmokedHarvests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos hivePos = new BlockPos(5, 4, 5);
        BlockPos campfirePos = hivePos.below();
        BlockPos absoluteHivePos = helper.absolutePos(hivePos);
        Bee bee = helper.spawn(EntityTypes.BEE, 7, 4, 5);
        var survivalPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        var creativePlayer = helper.makeMockPlayer(GameType.CREATIVE);
        BlockState fullHive = Blocks.BEEHIVE.defaultBlockState().setValue(
                BeehiveBlock.HONEY_LEVEL,
                BeehiveBlock.MAX_HONEY_LEVELS
        );
        BlockState litCampfire = Blocks.CAMPFIRE.defaultBlockState().setValue(
                CampfireBlock.LIT,
                true
        );

        try {
            helper.setBlock(hivePos, fullHive);
            helper.setBlock(campfirePos, litCampfire);
            positionAtHive(helper, survivalPlayer, hivePos);
            positionAtHive(helper, creativePlayer, hivePos);

            RetoldHiveColonyEvents.onHiveHarvest(harvestEvent(
                    survivalPlayer,
                    absoluteHivePos
            ));
            helper.assertTrue(
                    bee.getTarget() == null,
                    "Smoke must suppress Retold colony defense during harvest"
            );

            helper.setBlock(campfirePos, Blocks.AIR);
            RetoldHiveColonyEvents.onHiveHarvest(harvestEvent(
                    creativePlayer,
                    absoluteHivePos
            ));
            helper.assertTrue(
                    bee.getTarget() == null,
                    "Creative players must remain excluded from colony aggression"
            );

            RetoldHiveColonyEvents.onHiveHarvest(harvestEvent(
                    survivalPlayer,
                    absoluteHivePos
            ));
            assertDefense(
                    helper,
                    bee,
                    survivalPlayer,
                    RetoldTargetSource.FACTION_ASSIST,
                    "An unsmoked full-hive harvest must recruit nearby Bees"
            );

            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    bee,
                    survivalPlayer,
                    true
            );
            RetoldAiControl.clear(bee);
            RetoldHiveColonyEvents.onHiveBreak(new BreakBlockEvent(
                    level,
                    absoluteHivePos,
                    fullHive,
                    survivalPlayer
            ));
            assertDefense(
                    helper,
                    bee,
                    survivalPlayer,
                    RetoldTargetSource.FACTION_ASSIST,
                    "Breaking a hive must recruit nearby Bees"
            );
            helper.succeed();
        } finally {
            RetoldAiControl.clear(bee);
            RetoldMobStates.remove(bee);
            bee.discard();
            survivalPlayer.discard();
            creativePlayer.discard();
        }
    }

    private static UseItemOnBlockEvent harvestEvent(
            Player player,
            BlockPos hivePos
    ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(hivePos),
                Direction.UP,
                hivePos,
                false
        );

        return new UseItemOnBlockEvent(
                new UseOnContext(player, InteractionHand.MAIN_HAND, hit),
                UseItemOnBlockEvent.UsePhase.BLOCK
        );
    }

    private static void positionAtHive(
            GameTestHelper helper,
            Player player,
            BlockPos hivePos
    ) {
        Vec3 position = helper.absoluteVec(Vec3.atCenterOf(hivePos));
        player.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
    }

    private static void assertDefense(
            GameTestHelper helper,
            PathfinderMob bee,
            LivingEntity attacker,
            RetoldTargetSource source,
            String message
    ) {
        helper.assertTrue(
                bee.getTarget() == attacker
                        && RetoldFactionTargetMemory.isOwnedByAny(
                        bee,
                        attacker,
                        source
                )
                        && RetoldAiControl.isControlledAsBy(
                        bee,
                        RetoldAiControlMode.ATTACK,
                        RetoldAiControlOwner.HIVE_COLONY
                ),
                message
        );
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            RetoldAiControl.clear(mob);
            RetoldMobStates.remove(mob);

            if (!mob.isRemoved()) {
                mob.discard();
            }
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 120, 0, true),
                        test
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
