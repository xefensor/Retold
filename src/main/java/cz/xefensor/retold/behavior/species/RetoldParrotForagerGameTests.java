package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.food.RetoldFoodBehaviorEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldParrotForagerGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldParrotForagerGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        registerTest(
                event,
                "parrot_crop_foraging_respects_mob_griefing",
                RetoldParrotForagerGameTests::cropForagingRespectsMobGriefing
        );
        registerTest(
                event,
                "parrot_warns_owner_about_real_danger",
                RetoldParrotForagerGameTests::warnsOwnerAboutRealDanger
        );
    }

    private static void cropForagingRespectsMobGriefing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Parrot parrot = helper.spawn(EntityTypes.PARROT, 2, 2, 2);
        BlockPos cropPos = new BlockPos(3, 2, 2);
        boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        long gameTime = level.getGameTime();
        RetoldMobState state = RetoldMobStates.getOrCreate(parrot, gameTime);

        try {
            helper.assertValueEqual(
                    RetoldMobRules.profileType(parrot),
                    RetoldMobProfileType.PARROT_FORAGER,
                    "Parrots must use their dedicated managed profile"
            );
            helper.assertTrue(
                    RetoldMobRules.canEatDroppedItem(
                            parrot,
                            new ItemStack(Items.WHEAT_SEEDS)
                    ),
                    "Parrots must recognize dropped seeds"
            );
            helper.assertFalse(
                    RetoldMobRules.canEatDroppedItem(
                            parrot,
                            new ItemStack(Items.DANDELION)
                    ),
                    "Parrots must not inherit the ordinary small-forager flower diet"
            );

            state.setHunger(80);
            helper.setBlock(cropPos, Blocks.WHEAT);
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    false,
                    level.getServer()
            );
            helper.assertFalse(
                    RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                            level,
                            parrot,
                            helper.absolutePos(cropPos),
                            gameTime
                    ),
                    "mobGriefing=false must block Parrot crop consumption"
            );
            helper.assertBlockPresent(Blocks.WHEAT, cropPos);
            helper.assertValueEqual(
                    state.hunger(),
                    80,
                    "Blocked crop consumption must not relieve hunger"
            );

            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    true,
                    level.getServer()
            );
            helper.assertTrue(
                    RetoldFoodBehaviorEvents.tryConsumeForageBlock(
                            level,
                            parrot,
                            helper.absolutePos(cropPos),
                            gameTime
                    ),
                    "A hungry Parrot must consume a reachable crop when mob griefing allows it"
            );
            helper.assertBlockNotPresent(Blocks.WHEAT, cropPos);
            helper.assertTrue(
                    state.hunger() < 80,
                    "Crop consumption must relieve Parrot hunger"
            );
            helper.succeed();
        } finally {
            level.getGameRules().set(
                    GameRules.MOB_GRIEFING,
                    originalMobGriefing,
                    level.getServer()
            );
            RetoldAiControl.clear(parrot);
            RetoldMobStates.remove(parrot);
            parrot.discard();
        }
    }

    private static void warnsOwnerAboutRealDanger(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer owner = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ServerPlayer creativeOwner =
                (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        ServerPlayer spectatorOwner =
                (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
        Parrot parrot = helper.spawn(EntityTypes.PARROT, 2, 2, 2);
        Zombie zombie = helper.spawn(EntityTypes.ZOMBIE, 4, 2, 2);
        long gameTime = level.getGameTime();

        try {
            Vec3 ownerPosition = helper.absoluteVec(new Vec3(2.5D, 2.0D, 2.5D));
            owner.snapTo(
                    ownerPosition.x(),
                    ownerPosition.y(),
                    ownerPosition.z(),
                    0.0F,
                    0.0F
            );
            owner.setOnGround(true);
            parrot.setTame(true, true);
            parrot.setOwner(owner);
            parrot.setOrderedToSit(false);

            RetoldParrotForagerEvents.tick(level, parrot, gameTime);
            helper.assertFalse(
                    RetoldParrotForagerEvents.isWarningOwner(parrot, gameTime),
                    "A nearby mob that is not threatening the owner must not trigger a warning"
            );

            creativeOwner.setLastHurtByMob(zombie);
            parrot.setOwner(creativeOwner);
            RetoldParrotForagerEvents.tick(level, parrot, gameTime + 1L);
            helper.assertFalse(
                    RetoldParrotForagerEvents.isWarningOwner(parrot, gameTime + 1L),
                    "Creative owners must be excluded from Parrot danger warnings"
            );

            spectatorOwner.setLastHurtByMob(zombie);
            parrot.setOwner(spectatorOwner);
            RetoldParrotForagerEvents.tick(level, parrot, gameTime + 2L);
            helper.assertFalse(
                    RetoldParrotForagerEvents.isWarningOwner(parrot, gameTime + 2L),
                    "Spectator owners must be excluded from Parrot danger warnings"
            );

            parrot.setOwner(owner);
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            zombie,
                            owner,
                            RetoldTargetSource.RETALIATION
                    ),
                    "The warning fixture must give the Zombie a valid owner target"
            );
            RetoldParrotForagerEvents.tick(level, parrot, gameTime + 3L);
            helper.assertTrue(
                    RetoldParrotForagerEvents.isWarningOwner(parrot, gameTime + 3L),
                    "A tamed Parrot must warn when a nearby mob actively targets its owner"
            );
            helper.assertTrue(
                    RetoldParrotForagerEvents.warningThreat(parrot) == zombie,
                    "The warning must remember the real owner threat"
            );
            helper.assertTrue(
                    parrot.getTarget() == null && !RetoldAiControl.isControlled(parrot),
                    "Warning must not turn the pacifist Parrot into a combat owner"
            );

            helper.assertTrue(
                    parrot.setEntityOnShoulder(owner),
                    "The warning behavior must preserve vanilla shoulder transfer"
            );
            helper.assertTrue(
                    owner.getShoulderParrotLeft().isPresent()
                            || owner.getShoulderParrotRight().isPresent(),
                    "The owner must retain the transferred Parrot on a shoulder"
            );
            RetoldParrotForagerEvents.tickShoulderParrotOwner(
                    level,
                    owner,
                    gameTime + 20L
            );
            helper.assertTrue(
                    RetoldParrotForagerEvents.hasRecentShoulderWarning(
                            owner,
                            gameTime + 20L
                    ),
                    "A shoulder Parrot must retain the owner-danger warning"
            );
            helper.succeed();
        } finally {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    zombie,
                    owner,
                    true
            );
            RetoldAiControl.clear(parrot);
            RetoldMobStates.remove(parrot);
            parrot.discard();
            zombie.discard();
            owner.discard();
            creativeOwner.discard();
            spectatorOwner.discard();
        }
    }

    private static void registerTest(
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
                100,
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
