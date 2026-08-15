package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.registry.RetoldTags;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.undead.RetoldUndeadSpawnPressure;
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
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldUndeadTargetParityGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldUndeadTargetParityGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_undead_target_parity"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "undead_targeting_does_not_prioritize_players",
                RetoldUndeadTargetParityGameTests::undeadTargetingDoesNotPrioritizePlayers
        );
        registerTest(
                event,
                environment,
                "undead_allies_reject_and_clear_vanilla_targets",
                RetoldUndeadTargetParityGameTests::undeadAlliesRejectAndClearVanillaTargets
        );
        registerTest(
                event,
                environment,
                "undead_stage_two_expands_coordination",
                RetoldUndeadTargetParityGameTests::undeadStageTwoExpandsCoordination
        );
        registerTest(
                event,
                environment,
                "undead_stage_two_increases_natural_spawn_weights",
                RetoldUndeadTargetParityGameTests::undeadStageTwoIncreasesNaturalSpawnWeights
        );
    }

    private static void undeadStageTwoIncreasesNaturalSpawnWeights(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = worldData.getStage();
        MobSpawnSettings.SpawnerData zombieSpawn =
                new MobSpawnSettings.SpawnerData(EntityTypes.ZOMBIE, 1, 4);
        MobSpawnSettings.SpawnerData skeletonSpawn =
                new MobSpawnSettings.SpawnerData(EntityTypes.SKELETON, 1, 4);
        MobSpawnSettings.SpawnerData creeperSpawn =
                new MobSpawnSettings.SpawnerData(EntityTypes.CREEPER, 1, 4);
        WeightedList<MobSpawnSettings.SpawnerData> baseSpawns =
                WeightedList.<MobSpawnSettings.SpawnerData>builder()
                        .add(zombieSpawn, 95)
                        .add(skeletonSpawn, 100)
                        .add(creeperSpawn, 100)
                        .build();

        try {
            assertSpawnPressureTag(helper);

            worldData.setStage(RetoldWorldStage.STAGE_1);
            LevelEvent.PotentialSpawns stageOne = potentialSpawns(level, baseSpawns);
            RetoldUndeadSpawnPressure.apply(stageOne);
            helper.assertValueEqual(
                    stageOne.getSpawnerDataList().size(),
                    3,
                    "Stage 1 must preserve the biome's original natural-spawn weights"
            );

            worldData.setStage(RetoldWorldStage.STAGE_2);
            LevelEvent.PotentialSpawns stageTwo = potentialSpawns(level, baseSpawns);
            RetoldUndeadSpawnPressure.apply(stageTwo);
            helper.assertTrue(
                    hasWeightedEntry(stageTwo, zombieSpawn, 24)
                            && hasWeightedEntry(stageTwo, skeletonSpawn, 25),
                    "Stage 2 must add the rounded 25% weight bonus for tagged Undead entries"
            );
            helper.assertValueEqual(
                    stageTwo.getSpawnerDataList().size(),
                    5,
                    "Stage 2 must add only tagged entries and retain the original spawn data"
            );
            helper.assertValueEqual(
                    countEntries(stageTwo, creeperSpawn),
                    1L,
                    "Stage 2 must not change an unrelated monster's natural-spawn weight"
            );

            LevelEvent.PotentialSpawns nonMonster = new LevelEvent.PotentialSpawns(
                    level,
                    MobCategory.CREATURE,
                    BlockPos.ZERO,
                    baseSpawns
            );
            RetoldUndeadSpawnPressure.apply(nonMonster);
            helper.assertValueEqual(
                    nonMonster.getSpawnerDataList().size(),
                    3,
                    "Stage 2 pressure must not alter another natural-spawn category"
            );

            worldData.setStage(RetoldWorldStage.STAGE_3);
            LevelEvent.PotentialSpawns stageThree = potentialSpawns(level, baseSpawns);
            RetoldUndeadSpawnPressure.apply(stageThree);
            helper.assertValueEqual(
                    stageThree.getSpawnerDataList().size(),
                    3,
                    "Stage 3 must not add Undead spawn weight before its spawn cancellation"
            );
            helper.succeed();
        } finally {
            worldData.setStage(originalStage);
        }
    }

    private static void assertSpawnPressureTag(GameTestHelper helper) {
        List<EntityType<?>> pressuredTypes = List.of(
                EntityTypes.ZOMBIE,
                EntityTypes.ZOMBIE_VILLAGER,
                EntityTypes.HUSK,
                EntityTypes.DROWNED,
                EntityTypes.ZOMBIFIED_PIGLIN,
                EntityTypes.SKELETON,
                EntityTypes.STRAY,
                EntityTypes.BOGGED
        );

        helper.assertTrue(
                pressuredTypes.stream().allMatch(type ->
                        type.builtInRegistryHolder().is(
                                RetoldTags.STAGE_2_UNDEAD_SPAWN_PRESSURE
                        )),
                "The Stage 2 spawn-pressure tag must contain both coordination families"
        );
        helper.assertTrue(
                !EntityTypes.CREEPER.builtInRegistryHolder().is(
                        RetoldTags.STAGE_2_UNDEAD_SPAWN_PRESSURE
                ),
                "The Stage 2 spawn-pressure tag must not include unrelated monsters"
        );
    }

    private static LevelEvent.PotentialSpawns potentialSpawns(
            ServerLevel level,
            WeightedList<MobSpawnSettings.SpawnerData> baseSpawns
    ) {
        return new LevelEvent.PotentialSpawns(
                level,
                MobCategory.MONSTER,
                BlockPos.ZERO,
                baseSpawns
        );
    }

    private static boolean hasWeightedEntry(
            LevelEvent.PotentialSpawns event,
            MobSpawnSettings.SpawnerData spawn,
            int weight
    ) {
        return event.getSpawnerDataList().stream().anyMatch(entry ->
                entry.value().equals(spawn) && entry.weight() == weight
        );
    }

    private static long countEntries(
            LevelEvent.PotentialSpawns event,
            MobSpawnSettings.SpawnerData spawn
    ) {
        return event.getSpawnerDataList().stream()
                .filter(entry -> entry.value().equals(spawn))
                .count();
    }

    private static void undeadStageTwoExpandsCoordination(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = worldData.getStage();
        var source = helper.spawn(EntityTypes.ZOMBIE, 3, 3, 3);
        var closeFamilyRecruit = helper.spawn(EntityTypes.HUSK, 9, 3, 3);
        var distantFamilyRecruit = helper.spawn(EntityTypes.DROWNED, 18, 3, 3);
        PathfinderMob crossFamilyRecruit = spawnOccasionalRangedResponder(helper);
        var target = helper.spawn(EntityTypes.COW, 23, 3, 3);

        source.setNoAi(true);
        closeFamilyRecruit.setNoAi(true);
        distantFamilyRecruit.setNoAi(true);
        crossFamilyRecruit.setNoAi(true);
        target.setNoAi(true);

        try {
            worldData.setStage(RetoldWorldStage.STAGE_1);
            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            source,
                            target,
                            RetoldTargetSource.FACTION_COMBAT
                    ),
                    "The fixture source must acquire its ordinary enemy target"
            );

            RetoldUndeadHordeEvents.spreadTargetToNearbyHorde(
                    level,
                    source,
                    target,
                    level.getGameTime()
            );

            helper.assertTrue(
                    closeFamilyRecruit.getTarget() == target,
                    "Stage 1 must retain close same-family Undead cooperation"
            );
            helper.assertTrue(
                    distantFamilyRecruit.getTarget() == null,
                    "Stage 1 must not use the wider Stage 2 convergence radius"
            );
            helper.assertTrue(
                    crossFamilyRecruit.getTarget() == null,
                    "Stage 1 must not recruit a different Undead combat family"
            );

            worldData.setStage(RetoldWorldStage.STAGE_2);
            RetoldUndeadHordeEvents.spreadTargetToNearbyHorde(
                    level,
                    source,
                    target,
                    level.getGameTime() + 1L
            );

            helper.assertTrue(
                    distantFamilyRecruit.getTarget() == target
                            && RetoldFactionTargetMemory.getSource(
                            distantFamilyRecruit,
                            target
                    ) == RetoldTargetSource.FACTION_ASSIST,
                    "Stage 2 must widen same-family target sharing and convergence"
            );
            helper.assertTrue(
                    crossFamilyRecruit.getTarget() == target
                            && RetoldFactionTargetMemory.getSource(
                            crossFamilyRecruit,
                            target
                    ) == RetoldTargetSource.FACTION_ASSIST,
                    "Stage 2 must allow selected nearby ranged Undead to join the incident"
            );

            helper.succeed();
        } finally {
            worldData.setStage(originalStage);
            source.discard();
            closeFamilyRecruit.discard();
            distantFamilyRecruit.discard();
            crossFamilyRecruit.discard();
            target.discard();
        }
    }

    private static PathfinderMob spawnOccasionalRangedResponder(
            GameTestHelper helper
    ) {
        for (int attempt = 0; attempt < 64; attempt++) {
            PathfinderMob candidate = helper.spawn(EntityTypes.SKELETON, 7, 3, 4);

            if (RetoldUndeadStagePressure.isOccasionalResponder(candidate)) {
                return candidate;
            }

            candidate.discard();
        }

        throw new IllegalStateException("Unable to create a sampled Stage 2 Undead responder");
    }

    private static void undeadAlliesRejectAndClearVanillaTargets(
            GameTestHelper helper
    ) {
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 3, 3, 3);
        var skeleton = helper.spawn(EntityTypes.SKELETON, 5, 3, 3);
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 8, 3, 3);
        var zombieNautilus = helper.spawn(EntityTypes.ZOMBIE_NAUTILUS, 10, 3, 3);

        zombie.setNoAi(true);
        skeleton.setNoAi(true);
        zoglin.setNoAi(true);
        zombieNautilus.setNoAi(true);

        zombie.setTarget(skeleton);
        helper.assertTrue(
                zombie.getTarget() == null,
                "A vanilla target write must not make one Undead creature attack another"
        );
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        zombie,
                        skeleton,
                        RetoldTargetSource.RETALIATION
                )
                        && zombie.getTarget() == skeleton
                        && RetoldFactionTargetMemory.getSource(zombie, skeleton)
                        == RetoldTargetSource.RETALIATION,
                "An explicit Retold retaliation target must retain its intentional escape hatch"
        );
        RetoldCombatTargets.clearTargetReferencesAndAggression(
                zombie,
                skeleton,
                false
        );

        zombieNautilus.setTame(true, true);
        zoglin.setTarget(zombieNautilus);
        zoglin.getBrain().setMemory(
                MemoryModuleType.ATTACK_TARGET,
                zombieNautilus
        );
        helper.assertTrue(
                zoglin.getTarget() == zombieNautilus
                        && RetoldAiTargets.getBrainAttackTargetSafely(zoglin) == zombieNautilus,
                "A tamed Zombie Nautilus must remain a valid non-Undead target"
        );

        zombieNautilus.setTame(false, true);
        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(
                        zoglin.getTarget() == null
                                && RetoldAiTargets.getBrainAttackTargetSafely(zoglin) == null,
                        "Mob and Brain targets must clear when the target rejoins Undead diplomacy"
                );
                helper.succeed();
            } finally {
                zombie.discard();
                skeleton.discard();
                zoglin.discard();
                zombieNautilus.discard();
            }
        });
    }

    private static void undeadTargetingDoesNotPrioritizePlayers(GameTestHelper helper) {
        assertAdditionalUndeadMemberships(helper);
        assertZombieHordeParity(helper);
        assertSkeletonRangedParity(helper);
        assertGhastArtilleryParity(helper);
        // The fourth production scan belongs in a fresh shared-work-budget tick.
        helper.runAfterDelay(1, () -> {
            assertZoglinRampageParity(helper);
            helper.succeed();
        });
    }

    private static void assertAdditionalUndeadMemberships(GameTestHelper helper) {
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 5, 3, 5);
        var zombieNautilus = helper.spawn(EntityTypes.ZOMBIE_NAUTILUS, 7, 3, 5);
        var ordinaryPrey = helper.spawn(EntityTypes.COW, 9, 3, 5);

        try {
            helper.assertTrue(
                    RetoldFactionMembers.isUndead(zoglin)
                            && RetoldFactionMembers.isUndead(zombieNautilus),
                    "Zoglins and Zombie Nautiluses must belong to Retold's Undead faction"
            );
            helper.assertTrue(
                    RetoldFactionRelations.shouldAttack(zoglin, ordinaryPrey)
                            && RetoldFactionRelations.shouldAttack(zombieNautilus, ordinaryPrey)
                            && !RetoldFactionRelations.shouldAttack(zoglin, zombieNautilus),
                    "Additional Undead members must attack ordinary prey while tolerating each other"
            );
        } finally {
            zoglin.discard();
            zombieNautilus.discard();
            ordinaryPrey.discard();
        }
    }

    private static void assertZombieHordeParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 3, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 9, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.SKELETON, 6, 3, 5);
        ServerPlayer player = mockPlayer(helper, 10.5D, 3.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldUndeadHordeEvents.findHungryTarget(level, zombie) == nearerPrey,
                    "A hungry Zombie must choose nearer ordinary prey without player favoritism"
            );
        } finally {
            player.discard();
            zombie.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertSkeletonRangedParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var skeleton = helper.spawn(EntityTypes.SKELETON, 5, 3, 5);
        var betterRangedTarget = helper.spawn(EntityTypes.COW, 18, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 6, 3, 5);
        ServerPlayer player = mockPlayer(helper, 18.2D, 3.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldSkeletonRangedEvents.findVisibleEnemy(level, skeleton) == betterRangedTarget,
                    "A Skeleton must choose the better ranged target without player favoritism"
            );
        } finally {
            player.discard();
            skeleton.discard();
            betterRangedTarget.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertGhastArtilleryParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var ghast = helper.spawn(EntityTypes.GHAST, 5, 10, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 13, 10, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 6, 10, 5);
        ServerPlayer player = mockPlayer(helper, 17.5D, 10.0D, 5.5D);

        try {
            helper.assertTrue(
                    RetoldGhastArtilleryEvents.findBestArtilleryTarget(level, ghast) == nearerPrey,
                    "A Ghast must choose nearer visible prey without player favoritism"
            );
        } finally {
            player.discard();
            ghast.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static void assertZoglinRampageParity(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Keep this fixture in a separate shared-scan bucket from the Skeleton case.
        var zoglin = helper.spawn(EntityTypes.ZOGLIN, 37, 3, 5);
        var nearerPrey = helper.spawn(EntityTypes.COW, 41, 3, 5);
        var undeadNeighbor = helper.spawn(EntityTypes.ZOMBIE, 38, 3, 5);
        ServerPlayer player = mockPlayer(helper, 42.5D, 3.0D, 5.5D);

        try {
            var selected = RetoldZoglinRampagerEvents.findBestRampageTarget(level, zoglin);
            helper.assertTrue(
                    selected == nearerPrey,
                    "A Zoglin must choose nearer visible prey without player favoritism; selected "
                            + (selected == null ? "none" : selected.getType().toString())
            );
        } finally {
            player.discard();
            zoglin.discard();
            nearerPrey.discard();
            undeadNeighbor.discard();
        }
    }

    private static ServerPlayer mockPlayer(
            GameTestHelper helper,
            double x,
            double y,
            double z
    ) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(new Vec3(x, y, z));
        player.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
        return player;
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
