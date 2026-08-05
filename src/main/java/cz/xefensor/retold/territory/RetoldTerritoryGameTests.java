package cz.xefensor.retold.territory;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class RetoldTerritoryGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldTerritoryGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        registerTest(
                event,
                environment,
                "territory_tags_and_faction_members_are_complete",
                RetoldTerritoryGameTests::territoryTagsAndMembersAreComplete
        );
        registerTest(
                event,
                environment,
                "territory_warning_gates_attack_until_escalation",
                RetoldTerritoryGameTests::territoryWarningGatesAttackUntilEscalation
        );
        registerTest(
                event,
                environment,
                "territory_retaliation_bypasses_warning",
                RetoldTerritoryGameTests::territoryRetaliationBypassesWarning
        );
    }

    private static void territoryTagsAndMembersAreComplete(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Registry<Structure> structures = level.registryAccess()
                .lookupOrThrow(Registries.STRUCTURE);
        RetoldTerritoryConfig netherRemnants = RetoldTerritoryConfigs.get(
                RetoldFaction.NETHER_REMNANTS
        );
        RetoldTerritoryConfig illagers = RetoldTerritoryConfigs.get(
                RetoldFaction.ILLAGERS
        );

        helper.assertTrue(
                netherRemnants != null,
                "Nether Remnants must have a territory configuration"
        );
        helper.assertTrue(
                illagers != null,
                "Illagers must have a territory configuration"
        );
        helper.assertTrue(
                netherRemnants.requiredDimension == Level.NETHER,
                "Nether Remnant territory must be restricted to the Nether"
        );
        helper.assertTrue(
                illagers.requiredDimension == null,
                "Illager territory must remain available in its structure dimensions"
        );

        assertStructureInTag(
                helper,
                structures,
                BuiltinStructures.BASTION_REMNANT,
                netherRemnants.territoryTag
        );
        assertStructureInTag(
                helper,
                structures,
                BuiltinStructures.FORTRESS,
                netherRemnants.territoryTag
        );
        assertStructureInTag(
                helper,
                structures,
                BuiltinStructures.PILLAGER_OUTPOST,
                illagers.territoryTag
        );
        assertStructureInTag(
                helper,
                structures,
                BuiltinStructures.WOODLAND_MANSION,
                illagers.territoryTag
        );

        assertEntityTypesUseTerritory(
                helper,
                level,
                RetoldFaction.NETHER_REMNANTS,
                EntityTypes.PIGLIN,
                EntityTypes.PIGLIN_BRUTE,
                EntityTypes.BLAZE
        );
        assertEntityTypesUseTerritory(
                helper,
                level,
                RetoldFaction.ILLAGERS,
                EntityTypes.PILLAGER,
                EntityTypes.VINDICATOR,
                EntityTypes.EVOKER,
                EntityTypes.ILLUSIONER,
                EntityTypes.RAVAGER,
                EntityTypes.VEX
        );

        Entity witch = EntityTypes.WITCH.create(level, EntitySpawnReason.COMMAND);

        try {
            helper.assertTrue(witch != null, "The Witch test entity must be created");
            helper.assertTrue(
                    RetoldTerritoryConfigs.getForEntity(witch) == null,
                    "Witches must remain loose allies without Illager territory membership"
            );
            helper.succeed();
        } finally {
            if (witch != null) {
                witch.discard();
            }
        }
    }

    private static void territoryWarningGatesAttackUntilEscalation(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        SightedPillager guard = spawnGuard(helper, new Vec3(1.5D, 2.0D, 1.5D));
        Player player = spawnPlayer(helper, GameType.SURVIVAL, new Vec3(4.5D, 2.0D, 1.5D));
        Player creative = spawnPlayer(helper, GameType.CREATIVE, new Vec3(4.5D, 2.0D, 3.5D));
        Player spectator = spawnPlayer(helper, GameType.SPECTATOR, new Vec3(4.5D, 2.0D, 5.5D));
        RetoldTerritoryConfig config = RetoldTerritoryConfigs.get(RetoldFaction.ILLAGERS);
        RetoldTerritoryContext territory = context(level, RetoldFaction.ILLAGERS);
        RetoldTerritoryMobState state = new RetoldTerritoryMobState();
        Map<PathfinderMob, RetoldTerritoryMobState> states = new HashMap<>();
        long gameTime = level.getGameTime();

        try {
            state.territoryContext = territory;
            states.put(guard, state);

            helper.assertFalse(
                    RetoldTerritoryTargetSelector.isPossibleIntruder(
                            level,
                            guard,
                            creative,
                            config,
                            gameTime
                    ),
                    "Creative players must never enter territory warning state"
            );
            helper.assertFalse(
                    RetoldTerritoryTargetSelector.isPossibleIntruder(
                            level,
                            guard,
                            spectator,
                            config,
                            gameTime
                    ),
                    "Spectators must never enter territory warning state"
            );

            RetoldTerritoryController.setWarningTarget(
                    state,
                    guard,
                    player,
                    gameTime
            );
            helper.assertTrue(
                    state.warningTarget == player,
                    "A survival intruder must become the warning target"
            );
            helper.assertValueEqual(
                    state.flowState,
                    RetoldTerritoryFlowState.OBSERVING,
                    "Initial trespass suspicion must begin with observation"
            );
            helper.assertTrue(
                    guard.getTarget() == null,
                    "Noticing a player must not immediately start combat"
            );

            RetoldFactionTargetGuards.setTargetIgnoringGuard(guard, player);
            helper.assertTrue(
                    RetoldTerritoryCombat.suppressExistingTargetDuringWarning(
                            level,
                            guard,
                            config,
                            states,
                            gameTime
                    ),
                    "Territory logic must suppress a non-retaliation target before escalation"
            );
            helper.assertTrue(
                    guard.getTarget() == null,
                    "Suppressed warning-stage combat must clear the mob target"
            );

            RetoldTerritoryReputation.addSuspicion(
                    territory,
                    player,
                    RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD,
                    gameTime
            );
            RetoldTerritoryStateMachine.reconcileWarningState(
                    guard,
                    state,
                    gameTime
            );
            helper.assertValueEqual(
                    state.flowState,
                    RetoldTerritoryFlowState.FINAL_WARNING,
                    "Attack-level suspicion must still pass through final warning"
            );

            long attackTime = gameTime
                    + RetoldTerritoryConstants.WARNING_MIN_FINAL_WARNING_TICKS_BEFORE_ATTACK;
            RetoldTerritoryController.tickWarningState(
                    new RetoldTerritoryStateContext(
                            level,
                            guard,
                            state,
                            config,
                            states,
                            attackTime,
                            true
                    )
            );
            helper.assertValueEqual(
                    state.flowState,
                    RetoldTerritoryFlowState.ATTACKING,
                    "A completed final warning must transition to attack"
            );
            helper.assertTrue(
                    guard.getTarget() == player,
                    "The escalated guard must target the warned intruder"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            guard,
                            player,
                            RetoldTargetSource.TERRITORY_ATTACK
                    ),
                    "Escalated combat must retain territory-attack ownership"
            );
            helper.succeed();
        } finally {
            RetoldTerritoryStateMachine.deactivate(guard, state, level.getGameTime());
            removePlayer(level, player);
            removePlayer(level, creative);
            removePlayer(level, spectator);
            guard.discard();
        }
    }

    private static void territoryRetaliationBypassesWarning(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SightedPillager guard = spawnGuard(helper, new Vec3(1.5D, 2.0D, 1.5D));
        Player attacker = spawnPlayer(helper, GameType.SURVIVAL, new Vec3(4.5D, 2.0D, 1.5D));
        RetoldTerritoryConfig config = RetoldTerritoryConfigs.get(RetoldFaction.ILLAGERS);
        RetoldTerritoryMobState state = new RetoldTerritoryMobState();
        Map<PathfinderMob, RetoldTerritoryMobState> states = new HashMap<>();
        long gameTime = level.getGameTime();

        try {
            state.territoryContext = context(level, RetoldFaction.ILLAGERS);
            states.put(guard, state);
            guard.setLastHurtByMob(attacker);

            helper.assertTrue(
                    RetoldTerritoryCombat.tryAdoptRetaliationTarget(
                            level,
                            guard,
                            state,
                            config,
                            states,
                            gameTime
                    ),
                    "Directly attacking a guard must trigger immediate retaliation"
            );
            helper.assertValueEqual(
                    state.flowState,
                    RetoldTerritoryFlowState.ATTACKING,
                    "Retaliation must enter attack without warning progression"
            );
            helper.assertTrue(
                    guard.getTarget() == attacker,
                    "The guard must retaliate against its direct attacker"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            guard,
                            attacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "Immediate retaliation must retain retaliation ownership"
            );
            helper.succeed();
        } finally {
            RetoldTerritoryStateMachine.deactivate(guard, state, level.getGameTime());
            removePlayer(level, attacker);
            guard.discard();
        }
    }

    private static void assertStructureInTag(
            GameTestHelper helper,
            Registry<Structure> structures,
            ResourceKey<Structure> structureKey,
            TagKey<Structure> tag
    ) {
        Holder.Reference<Structure> structure = structures.get(structureKey)
                .orElseThrow();

        helper.assertTrue(
                structure.is(tag),
                structureKey.identifier() + " must be included in " + tag.location()
        );
    }

    private static void assertEntityTypesUseTerritory(
            GameTestHelper helper,
            ServerLevel level,
            RetoldFaction expectedFaction,
            EntityType<?>... entityTypes
    ) {
        for (EntityType<?> entityType : entityTypes) {
            Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);

            try {
                helper.assertTrue(
                        entity != null,
                        "The territory-member test entity must be created"
                );
                RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(entity);
                helper.assertTrue(
                        config != null && config.faction == expectedFaction,
                        BuiltInRegistries.ENTITY_TYPE.getKey(entityType)
                                + " must use "
                                + expectedFaction
                                + " territory"
                );
            } finally {
                if (entity != null) {
                    entity.discard();
                }
            }
        }
    }

    private static SightedPillager spawnGuard(
            GameTestHelper helper,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        SightedPillager guard = new SightedPillager(level);
        Vec3 position = helper.absoluteVec(relativePosition);

        guard.snapTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(guard);
        return guard;
    }

    private static Player spawnPlayer(
            GameTestHelper helper,
            GameType gameType,
            Vec3 relativePosition
    ) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(gameType);
        Vec3 position = helper.absoluteVec(relativePosition);

        player.snapTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(player);
        return player;
    }

    private static void removePlayer(ServerLevel level, Player player) {
        level.players().remove(player);
        player.discard();
    }

    private static RetoldTerritoryContext context(
            ServerLevel level,
            RetoldFaction faction
    ) {
        return new RetoldTerritoryContext(
                faction,
                level.dimension().toString(),
                0,
                0
        );
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            Consumer<GameTestHelper> test
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(id(name), new InlineGameTest(testData, test));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private static final class SightedPillager extends Pillager {
        private SightedPillager(ServerLevel level) {
            super(EntityTypes.PILLAGER, level);
        }

        @Override
        public boolean hasLineOfSight(Entity target) {
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
