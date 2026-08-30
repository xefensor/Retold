package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldSkeletonTacticsGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldSkeletonTacticsGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath(
                        Retold.MODID,
                        "ranged_undead_hold_fire_and_reposition_around_allies"
                ),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true),
                        RetoldSkeletonTacticsGameTests::rangedUndeadAvoidAlliedFiringLane
                )
        );
    }

    private static void rangedUndeadAvoidAlliedFiringLane(GameTestHelper helper) {
        var level = helper.getLevel();
        Skeleton skeleton = helper.spawnWithNoFreeWill(EntityTypes.SKELETON, 2, 2, 2);
        Zombie alliedBlocker = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 8, 2, 2);
        var target = helper.spawnWithNoFreeWill(EntityTypes.COW, 14, 2, 2);

        try {
            skeleton.setItemInHand(
                    InteractionHand.MAIN_HAND,
                    new ItemStack(Items.BOW)
            );
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            skeleton,
                            target,
                            RetoldTargetSource.FACTION_ASSIST
                    ),
                    "The ranged-tactics fixture must begin with an owned enemy target"
            );
            helper.assertTrue(
                    RetoldSkeletonRangedEvents.findBlockingUndeadAlly(
                            level,
                            skeleton,
                            target,
                            level.getGameTime()
                    ) == alliedBlocker,
                    "An allied Undead body directly between shooter and target must block fire"
            );

            int arrowsBeforeBlockedShot = nearbyArrowCount(helper, skeleton);
            skeleton.performRangedAttack(target, 1.0F);
            helper.assertTrue(
                    nearbyArrowCount(helper, skeleton) == arrowsBeforeBlockedShot,
                    "A Skeleton must hold its shot while an Undead ally blocks the firing lane"
            );

            RetoldSkeletonRangedEvents.maintainRange(
                    skeleton,
                    target,
                    level.getGameTime()
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            skeleton,
                            RetoldAiControlMode.ATTACK,
                            RetoldAiControlOwner.UNDEAD_RANGED
                    ),
                    "A blocked Skeleton must retain owned ranged repositioning control"
            );
            Vec3 reposition = RetoldSkeletonRangedEvents.clearFiringLaneDestination(
                    skeleton,
                    target
            );
            helper.assertTrue(
                    Math.abs(reposition.z - skeleton.getZ()) >= 4.0D
                            && reposition.x < skeleton.getX(),
                    "A blocked Skeleton must choose a lateral and slightly retreating angle"
            );

            alliedBlocker.setPos(
                    alliedBlocker.getX(),
                    alliedBlocker.getY(),
                    alliedBlocker.getZ() + 5.0D
            );
            helper.assertTrue(
                    RetoldSkeletonRangedEvents.findBlockingUndeadAlly(
                            level,
                            skeleton,
                            target,
                            level.getGameTime()
                    ) == null,
                    "Moving the ally clear must reopen the firing lane"
            );

            int arrowsBeforeClearShot = nearbyArrowCount(helper, skeleton);
            skeleton.performRangedAttack(target, 1.0F);
            helper.assertTrue(
                    nearbyArrowCount(helper, skeleton) == arrowsBeforeClearShot + 1,
                    "A clear firing lane must preserve the normal Skeleton shot"
            );
            helper.succeed();
        } finally {
            RetoldAiControl.clear(skeleton);
            level.getEntitiesOfClass(
                    AbstractArrow.class,
                    skeleton.getBoundingBox().inflate(40.0D)
            ).forEach(AbstractArrow::discard);
            skeleton.discard();
            alliedBlocker.discard();
            target.discard();
        }
    }

    private static int nearbyArrowCount(GameTestHelper helper, Skeleton skeleton) {
        return helper.getLevel().getEntitiesOfClass(
                AbstractArrow.class,
                skeleton.getBoundingBox().inflate(40.0D)
        ).size();
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
