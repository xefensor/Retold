package cz.xefensor.retold.behavior.performance;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.flee.RetoldControlledFleeEvents;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfile;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfileType;
import cz.xefensor.retold.behavior.profiles.RetoldMobProfiles;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.villager.RetoldVillagerCommunalSupply;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Isolated, phase-by-phase TPS coverage for every mob that has a Retold profile.
 *
 * <p>Each species runs through idle, hunting, social, danger, and ecology phases
 * after a short warmup. The danger fixture writes last-hurt memory for every
 * subject so existing retaliation and danger systems are exercised. Mobs using
 * the shared passive-flee behavior additionally take one point of real mob damage,
 * which includes the production damage event, immediate movement claim, and
 * remembered flee follow-through in the measured workload. Wild ordinary predators
 * are lowered through their 25% health threshold so their ten-second wounded-flight
 * continuation is measured. Bee and undead-mount damage-triggered defense paths
 * receive the same treatment.</p>
 *
 * <p>Every test owns a separate GameTest environment. This is intentional: two
 * 50-mob samples running in the same server tick would contaminate one another's
 * wall-clock measurement and make the reported species cost meaningless.</p>
 *
 * <p>This matrix is a performance regression gate, not a substitute for the
 * focused behavior GameTests or natural-terrain in-game verification.</p>
 */
public final class RetoldPerMobTpsGameTests {
    private static final Identifier TEST_STRUCTURE =
            Identifier.withDefaultNamespace("woodland_mansion/2x2_a1");

    private static final int SUBJECT_COUNT = 50;
    private static final int SUPPORT_COUNT = 8;
    private static final int WARMUP_TICKS = 20;
    private static final int PHASE_TICKS = 80;
    private static final int PHASE_COUNT = BenchmarkPhase.values().length;
    private static final int TEST_TICKS = WARMUP_TICKS + PHASE_TICKS * PHASE_COUNT;
    private static final int TEST_TIMEOUT_TICKS = TEST_TICKS + 40;
    private static final double MAX_SERVER_TICK_MILLIS = 50.0D;

    private static final int ARENA_MIN = 0;
    private static final int ARENA_MAX = 14;
    private static final int ARENA_CLEAR_TOP = 14;

    private static final List<String> PROFILED_MOBS = List.of(
            "armadillo",
            "axolotl",
            "bat",
            "bee",
            "blaze",
            "bogged",
            "breeze",
            "camel",
            "camel_husk",
            "cat",
            "cave_spider",
            "chicken",
            "cod",
            "cow",
            "creaking",
            "creeper",
            "dolphin",
            "donkey",
            "drowned",
            "elder_guardian",
            "ender_dragon",
            "enderman",
            "endermite",
            "evoker",
            "fox",
            "frog",
            "ghast",
            "glow_squid",
            "goat",
            "guardian",
            "hoglin",
            "horse",
            "husk",
            "illusioner",
            "iron_golem",
            "llama",
            "magma_cube",
            "mooshroom",
            "mule",
            "nautilus",
            "ocelot",
            "panda",
            "parrot",
            "phantom",
            "pig",
            "piglin",
            "piglin_brute",
            "pillager",
            "polar_bear",
            "pufferfish",
            "rabbit",
            "ravager",
            "salmon",
            "sheep",
            "shulker",
            "silverfish",
            "skeleton",
            "skeleton_horse",
            "slime",
            "sniffer",
            "snow_golem",
            "spider",
            "stray",
            "strider",
            "squid",
            "trader_llama",
            "tropical_fish",
            "turtle",
            "vex",
            "villager",
            "vindicator",
            "warden",
            "witch",
            "wither",
            "wither_skeleton",
            "wolf",
            "zoglin",
            "zombie",
            "zombie_horse",
            "zombie_nautilus",
            "zombie_villager",
            "zombified_piglin"
    );

    private RetoldPerMobTpsGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        for (String mobPath : PROFILED_MOBS) {
            Identifier environmentId = id("isolated_mob_tps_" + mobPath);
            Holder<TestEnvironmentDefinition<?>> environment =
                    event.registerEnvironment(
                            environmentId,
                            new TestEnvironmentDefinition.AllOf()
                    );
            TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                    new TestData<>(
                            environment,
                            TEST_STRUCTURE,
                            TEST_TIMEOUT_TICKS,
                            0,
                            true,
                            Rotation.NONE,
                            false,
                            1,
                            1,
                            true,
                            40
                    );

            event.registerTest(
                    id("mob_tps_" + mobPath),
                    new InlineGameTest(
                            testData,
                            helper -> runBenchmark(helper, mobPath)
                    )
            );
        }
    }

    private static void runBenchmark(
            GameTestHelper helper,
            String mobPath
    ) {
        Identifier entityId = Identifier.withDefaultNamespace(mobPath);

        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            helper.fail("Profiled TPS mob is absent from the entity registry: " + entityId);
            return;
        }

        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);
        RetoldMobProfile profile = RetoldMobProfiles.get(entityType);

        if (profile.type() == RetoldMobProfileType.NONE) {
            helper.fail("No loaded Retold mob profile for TPS test: " + entityId);
            return;
        }

        ArenaKind arenaKind = arenaKind(mobPath);
        buildArena(helper, mobPath, arenaKind);

        BenchmarkRun run = new BenchmarkRun(
                mobPath,
                entityType,
                profile,
                arenaKind,
                currentClockTime(helper.getLevel()),
                helper.getLevel().getGameRules().get(GameRules.MOB_GRIEFING),
                RetoldWorldData.get(helper.getLevel()).getStage()
        );

        if (profile.type() == RetoldMobProfileType.UNDEAD_HUNGRY
                || profile.type() == RetoldMobProfileType.UNDEAD_TOLERANT) {
            RetoldWorldData.get(helper.getLevel()).setStage(RetoldWorldStage.STAGE_2);
        }

        helper.getLevel().getGameRules().set(
                GameRules.MOB_GRIEFING,
                false,
                helper.getLevel().getServer()
        );

        try {
            spawnSubjects(helper, run);
        } catch (RuntimeException exception) {
            cleanup(helper, run);
            throw exception;
        }

        if (mobPath.equals("warden")) {
            helper.runAtTickTime(1, () -> setupWardenAttention(helper, run));
        }

        helper.runAtTickTime(
                WARMUP_TICKS,
                () -> startPhase(helper, run, BenchmarkPhase.IDLE_REST)
        );

        for (int index = 1; index < PHASE_COUNT; index++) {
            BenchmarkPhase phase = BenchmarkPhase.values()[index];
            int tick = WARMUP_TICKS + index * PHASE_TICKS;

            helper.runAtTickTime(
                    tick,
                    () -> {
                        finishPhase(helper, run);
                        startPhase(helper, run, phase);
                    }
            );
        }

        helper.runAtTickTime(
                TEST_TICKS,
                () -> {
                    finishPhase(helper, run);
                    finishBenchmark(helper, run);
                }
        );
    }

    private static void spawnSubjects(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        ServerLevel level = helper.getLevel();

        for (int index = 0; index < SUBJECT_COUNT; index++) {
            Entity entity = run.mobPath.equals("warden")
                    ? new BenchmarkWarden(level)
                    : run.entityType.create(level, EntitySpawnReason.COMMAND);

            if (!(entity instanceof Mob mob)) {
                throw new IllegalStateException(
                        "Profiled entity is not a Mob: minecraft:" + run.mobPath
                );
            }

            Vec3 position = subjectPosition(helper, run.arenaKind, index);
            mob.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
            mob.setPersistenceRequired();
            mob.setInvulnerable(true);
            mob.setSilent(true);
            stabilizeDimensionSpecificMob(mob);

            if (mob instanceof AbstractCubeMob cubeMob) {
                cubeMob.setSize(4, true);
            }

            if (mob instanceof Warden warden) {
                preventWardenBurrowing(warden);
            }

            if (!level.addFreshEntity(mob)) {
                throw new IllegalStateException(
                        "Could not add TPS subject minecraft:" + run.mobPath
                );
            }

            RetoldMobStates.getOrCreate(mob, level.getGameTime()).setHunger(0);
            run.subjects.add(mob);
        }
    }

    private static void startPhase(
            GameTestHelper helper,
            BenchmarkRun run,
            BenchmarkPhase phase
    ) {
        run.currentPhase = phase;
        clearStimuli(run);

        switch (phase) {
            case IDLE_REST -> setupIdlePhase(helper, run);
            case DROPPED_FOOD_FORAGE -> setupFoodPhase(helper, run);
            case HUNT_TARGETING -> setupHuntPhase(helper, run);
            case DANGER_SOCIAL -> setupDangerPhase(helper, run);
            case HABITAT_DAY_NIGHT -> setupEcologyPhase(helper, run);
        }

        RetoldBehaviorPerf.reset();
        run.phaseStartedAtNanos = System.nanoTime();
        run.phaseStartedAtServerTick = helper.getLevel().getServer().getTickCount();
    }

    private static void setupIdlePhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(isNocturnal(run.mobPath) ? 6_000L : 18_000L);

        if (run.mobPath.equals("warden")) {
            spawnSupportMobs(helper, run, EntityTypes.COW, SUPPORT_COUNT, false);
        } else if (run.mobPath.equals("villager")) {
            // Exercise routine communal stocking while Farmers are awake; the
            // habitat phase still covers the normal species day/night setting.
            helper.setTime(6_000L);
            placeVillagerFoodFixtures(helper, run);
        }

        List<LivingEntity> sensoryTargets = run.stimuli.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);
            RetoldMobState state = RetoldMobStates.getOrCreate(
                    subject,
                    helper.getLevel().getGameTime()
            );
            state.setHunger(0);
            state.setStress(0);
            state.setConfidence(50);
            subject.setTarget(null);
            subject.getNavigation().stop();

            if (subject instanceof Warden warden) {
                preventWardenBurrowing(warden);

                if (!sensoryTargets.isEmpty()) {
                    activateWardenAgainst(
                            warden,
                            sensoryTargets.get(index % sensoryTargets.size())
                    );
                }
            }
        }

        helper.runAfterDelay(1, () -> {
            long gameTime = helper.getLevel().getGameTime();

            for (Mob subject : run.subjects) {
                if (subject instanceof Villager villager
                        && villager.getVillagerData()
                        .profession()
                        .is(VillagerProfession.FARMER)) {
                    RetoldVillagerCommunalSupply.tick(
                            helper.getLevel(),
                            villager,
                            gameTime
                    );
                }
            }
        });
    }

    private static void setupFoodPhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(6_000L);
        Item food = preferredFood(run.mobPath, run.profile.type());

        if (run.mobPath.equals("warden")) {
            spawnSupportMobs(helper, run, EntityTypes.COW, SUPPORT_COUNT, false);
        }

        List<LivingEntity> sensoryTargets = run.stimuli.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);
            RetoldMobState state = RetoldMobStates.getOrCreate(
                    subject,
                    helper.getLevel().getGameTime()
            );
            state.setHunger(100);
            subject.setTarget(null);

            if (subject instanceof Warden warden) {
                preventWardenBurrowing(warden);

                if (!sensoryTargets.isEmpty()) {
                    activateWardenAgainst(
                            warden,
                            sensoryTargets.get(index % sensoryTargets.size())
                    );
                }
            }

            if (food != null) {
                Vec3 itemPosition = stimulusPosition(helper, run.arenaKind, index);
                ItemEntity droppedFood = new ItemEntity(
                        helper.getLevel(),
                        itemPosition.x(),
                        itemPosition.y(),
                        itemPosition.z(),
                        new ItemStack(food)
                );
                droppedFood.setDefaultPickUpDelay();

                if (helper.getLevel().addFreshEntity(droppedFood)) {
                    run.stimuli.add(droppedFood);
                }
            }
        }

        placeForageFeatures(helper, run.mobPath);
    }

    private static void setupHuntPhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(18_000L);
        EntityType<?> targetType = huntTargetType(run.mobPath);
        spawnSupportMobs(helper, run, targetType, SUPPORT_COUNT, false);

        List<LivingEntity> livingTargets = run.stimuli.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);
            RetoldMobStates.getOrCreate(
                    subject,
                    helper.getLevel().getGameTime()
            ).setHunger(100);

            if (!livingTargets.isEmpty()) {
                LivingEntity target = livingTargets.get(index % livingTargets.size());
                subject.setTarget(target);

                if (subject instanceof Warden warden) {
                    activateWardenAgainst(warden, target);
                }
            }

            if (subject instanceof Warden warden) {
                preventWardenBurrowing(warden);
            }
        }
    }

    private static void setupDangerPhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(18_000L);
        EntityType<?> threatType = dangerTargetType(run.mobPath);
        spawnSupportMobs(helper, run, threatType, SUPPORT_COUNT, false);

        if (run.mobPath.equals("polar_bear")) {
            spawnSupportMobs(helper, run, EntityTypes.POLAR_BEAR, SUPPORT_COUNT, true);
        }

        List<LivingEntity> threats = run.stimuli.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .filter(entity -> !(entity instanceof AgeableMob ageable) || !ageable.isBaby())
                .toList();
        ServerPlayer parrotOwner = null;

        if (run.mobPath.equals("parrot")) {
            parrotOwner = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            Vec3 ownerPosition = helper.absoluteVec(new Vec3(7.5D, 2.0D, 7.5D));
            parrotOwner.snapTo(
                    ownerPosition.x(),
                    ownerPosition.y(),
                    ownerPosition.z(),
                    0.0F,
                    0.0F
            );
            run.stimuli.add(parrotOwner);

            for (LivingEntity threat : threats) {
                if (threat instanceof Mob threatMob) {
                    threatMob.setTarget(parrotOwner);
                    // Mock server players have no network connection. Keep the threats
                    // target-bearing for Parrot sensing without letting melee AI invoke
                    // connection-dependent player damage code in this clientless fixture.
                    threatMob.setNoAi(true);
                }
            }
        }

        long gameTime = helper.getLevel().getGameTime();

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);
            RetoldMobState state = RetoldMobStates.getOrCreate(subject, gameTime);
            state.setStress(100);
            state.setConfidence(20);
            state.markDanger(gameTime);

            if (subject instanceof Parrot parrot && parrotOwner != null) {
                parrot.setTame(true, true);
                parrot.setOwner(parrotOwner);
            }

            if (subject instanceof Warden warden) {
                preventWardenBurrowing(warden);
            }

            if (!threats.isEmpty()) {
                LivingEntity threat = threats.get(index % threats.size());
                subject.setLastHurtByMob(threat);

                // Benchmark subjects are normally invulnerable so the 50-mob sample remains
                // stable. Temporarily allow one real hit where production behavior begins from
                // the successful-damage event: shared passive flight, badly wounded
                // wild-predator flight, Bee colony defense, and undead-mount retaliation.
                if (subject instanceof PathfinderMob pathfinderMob
                        && (RetoldControlledFleeEvents.usesSharedFleeBehavior(pathfinderMob)
                        || RetoldMobRules.canUseOrdinaryPredatorSystems(pathfinderMob)
                        || run.mobPath.equals("bee")
                        || RetoldMobRules.isUndeadMount(pathfinderMob))) {
                    subject.setInvulnerable(false);
                    subject.invulnerableTime = 0;

                    if (RetoldMobRules.canUseOrdinaryPredatorSystems(pathfinderMob)) {
                        subject.setHealth(subject.getMaxHealth() * 0.25F + 0.5F);
                    }

                    subject.hurtServer(
                            helper.getLevel(),
                            helper.getLevel().damageSources().mobAttack(threat),
                            1.0F
                    );
                    subject.setInvulnerable(true);
                }

                if (subject instanceof Warden warden) {
                    activateWardenAgainst(warden, threat);
                }
            }
        }
    }

    private static void setupEcologyPhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(isNocturnal(run.mobPath) ? 18_000L : 6_000L);
        placeEcologyFeatures(helper, run.mobPath);

        if (run.mobPath.equals("bat")) {
            spawnSupportMobs(helper, run, EntityTypes.SPIDER, SUPPORT_COUNT, false);
        } else if (run.mobPath.equals("bee")) {
            spawnSupportMobs(helper, run, EntityTypes.SPIDER, 2, false);
        } else if (run.mobPath.equals("axolotl")) {
            spawnSupportMobs(helper, run, EntityTypes.GUARDIAN, SUPPORT_COUNT, false);
        } else if (run.mobPath.equals("frog")) {
            spawnSupportMobs(helper, run, EntityTypes.SLIME, SUPPORT_COUNT, false);
        } else if (run.mobPath.equals("slime") || run.mobPath.equals("magma_cube")) {
            spawnDroppedItems(helper, run, Items.COBBLESTONE, SUBJECT_COUNT);
        }

        long gameTime = helper.getLevel().getGameTime();

        for (Mob subject : run.subjects) {
            RetoldMobState state = RetoldMobStates.getOrCreate(subject, gameTime);
            state.setHunger(run.mobPath.equals("slime") || run.mobPath.equals("magma_cube") ? 0 : 60);
            state.setStress(20);
            subject.setLastHurtByMob(null);
            subject.setTarget(null);

            if (subject instanceof Warden warden) {
                warden.getBrain().eraseMemory(MemoryModuleType.DIG_COOLDOWN);
                warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
                warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
            }
        }
    }

    private static void finishPhase(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        int observedTicks = Math.max(
                1,
                helper.getLevel().getServer().getTickCount()
                        - run.phaseStartedAtServerTick
        );
        double averageTickMillis = (System.nanoTime() - run.phaseStartedAtNanos)
                / 1_000_000.0D
                / observedTicks;
        RetoldBehaviorPerf.AiWorkSnapshot work = RetoldBehaviorPerf.aiWorkSnapshot();

        run.results.put(
                run.currentPhase,
                new PhaseResult(
                        observedTicks,
                        averageTickMillis,
                        work,
                        RetoldBehaviorPerf.blockTargetPositionsChecked(),
                        activeSubjectCount(run),
                        diggingWardenCount(run)
                )
        );
    }

    private static void finishBenchmark(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        List<String> slowPhases = new ArrayList<>();
        int aliveSubjects = 0;

        for (Mob subject : run.subjects) {
            if (subject.isAlive() && !subject.isRemoved()) {
                aliveSubjects++;
            }
        }

        try {
            helper.assertValueEqual(
                    run.results.size(),
                    PHASE_COUNT,
                    "Every TPS behavior phase must produce a measurement"
            );
            assertExpectedSubjectLifecycle(helper, run, aliveSubjects);

            for (Map.Entry<BenchmarkPhase, PhaseResult> entry : run.results.entrySet()) {
                PhaseResult result = entry.getValue();
                double sustainableTps = Math.min(20.0D, 1_000.0D / result.averageTickMillis);

                if (result.averageTickMillis >= MAX_SERVER_TICK_MILLIS) {
                    slowPhases.add(
                            entry.getKey().serializedName
                                    + "="
                                    + decimal(result.averageTickMillis)
                                    + "ms"
                    );
                }

                Retold.LOGGER.info(
                        "MOB_TPS_RESULT mob=minecraft:{} profile={} managed={} subjects={} phase={} "
                                + "serverTicks={} avgTickMs={} sustainableTps={} scans={} scanHits={} "
                                + "positionScans={} paths={} pathSkips={} sight={} sightHits={} "
                                + "blockSearches={} blockSearchHits={} blockPositions={}",
                        run.mobPath,
                        run.profile.type().serializedName(),
                        run.profile.managed(),
                        SUBJECT_COUNT,
                        entry.getKey().serializedName,
                        result.observedTicks,
                        decimal(result.averageTickMillis),
                        decimal(sustainableTps),
                        result.work.entityScanRequests(),
                        result.work.entityScanCacheHits(),
                        result.work.positionScanRequests(),
                        result.work.pathRequests(),
                        result.work.pathSkips(),
                        result.work.sightRequests(),
                        result.work.sightCacheHits(),
                        result.work.blockSearchRequests(),
                        result.work.blockSearchCacheHits(),
                        result.blockTargetPositionsChecked
                );
            }

            helper.assertTrue(
                    slowPhases.isEmpty(),
                    "minecraft:"
                            + run.mobPath
                            + " cannot sustain 20 TPS with 50 mobs; slow phases: "
                            + String.join(", ", slowPhases)
            );
            helper.succeed();
        } finally {
            cleanup(helper, run);
        }
    }

    private static void assertExpectedSubjectLifecycle(
            GameTestHelper helper,
            BenchmarkRun run,
            int finalAliveSubjects
    ) {
        if (run.mobPath.equals("warden")) {
            for (BenchmarkPhase phase : BenchmarkPhase.values()) {
                PhaseResult result = run.results.get(phase);

                if (phase == BenchmarkPhase.HABITAT_DAY_NIGHT) {
                    int removedWardens = SUBJECT_COUNT - result.activeSubjects;
                    helper.assertValueEqual(
                            removedWardens + result.diggingWardens,
                            SUBJECT_COUNT,
                            "Every Warden must be digging or have completed burrowing in the ecology phase"
                    );
                } else {
                    helper.assertValueEqual(
                            result.activeSubjects,
                            SUBJECT_COUNT,
                            "All Wardens must remain present during "
                                    + phase.serializedName
                                    + " before the ecology/burrowing phase"
                    );
                }
            }

            return;
        }

        int housedBees = run.mobPath.equals("bee")
                ? housedBeeCount(helper)
                : 0;
        helper.assertValueEqual(
                finalAliveSubjects + housedBees,
                SUBJECT_COUNT,
                "The 50-mob TPS sample must remain loaded and alive for minecraft:"
                        + run.mobPath
                        + " (bees housed in benchmark hives count as alive)"
        );
    }

    private static int activeSubjectCount(BenchmarkRun run) {
        int active = 0;

        for (Mob subject : run.subjects) {
            if (subject.isAlive() && !subject.isRemoved()) {
                active++;
            }
        }

        return active;
    }

    private static int diggingWardenCount(BenchmarkRun run) {
        int digging = 0;

        for (Mob subject : run.subjects) {
            if (subject instanceof Warden warden
                    && !warden.isRemoved()
                    && warden.diggingAnimationState.isStarted()) {
                digging++;
            }
        }

        return digging;
    }

    private static void setupWardenAttention(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        spawnSupportMobs(helper, run, EntityTypes.COW, SUPPORT_COUNT, false);
        List<LivingEntity> targets = run.stimuli.stream()
                .filter(LivingEntity.class::isInstance)
                .map(LivingEntity.class::cast)
                .toList();

        if (targets.isEmpty()) {
            return;
        }

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);

            if (subject instanceof Warden warden && !warden.isRemoved()) {
                preventWardenBurrowing(warden);
                activateWardenAgainst(
                        warden,
                        targets.get(index % targets.size())
                );
            }
        }
    }

    private static void preventWardenBurrowing(Warden warden) {
        WardenAi.setDigCooldown(warden);
        WardenAi.setDisturbanceLocation(
                warden,
                warden.blockPosition().offset(4, 0, 4)
        );
    }

    private static void activateWardenAgainst(
            Warden warden,
            LivingEntity target
    ) {
        warden.increaseAngerAt(target, 150, false);
        warden.setAttackTarget(target);
    }

    private static int housedBeeCount(GameTestHelper helper) {
        int housed = 0;

        for (BlockPos relativePos : List.of(
                new BlockPos(3, 3, 3),
                new BlockPos(11, 3, 11)
        )) {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(relativePos))
                    instanceof BeehiveBlockEntity hive) {
                housed += hive.getOccupantCount();
            }
        }

        return housed;
    }

    private static void spawnSupportMobs(
            GameTestHelper helper,
            BenchmarkRun run,
            EntityType<?> entityType,
            int count,
            boolean babies
    ) {
        ServerLevel level = helper.getLevel();

        for (int index = 0; index < count; index++) {
            Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);

            if (!(entity instanceof LivingEntity livingEntity)) {
                continue;
            }

            Vec3 position = supportPosition(helper, run.arenaKind, index);
            livingEntity.snapTo(position.x(), position.y(), position.z(), 0.0F, 0.0F);
            livingEntity.setInvulnerable(true);
            livingEntity.setSilent(true);

            if (livingEntity instanceof Mob mob) {
                mob.setPersistenceRequired();
                stabilizeDimensionSpecificMob(mob);
            }

            if (babies && livingEntity instanceof AgeableMob ageableMob) {
                ageableMob.setBaby(true);
            }

            if (level.addFreshEntity(livingEntity)) {
                run.stimuli.add(livingEntity);
            }
        }
    }

    private static void spawnDroppedItems(
            GameTestHelper helper,
            BenchmarkRun run,
            Item item,
            int count
    ) {
        for (int index = 0; index < count; index++) {
            Vec3 position = stimulusPosition(helper, run.arenaKind, index);
            ItemEntity itemEntity = new ItemEntity(
                    helper.getLevel(),
                    position.x(),
                    position.y(),
                    position.z(),
                    new ItemStack(item)
            );
            itemEntity.setDefaultPickUpDelay();

            if (helper.getLevel().addFreshEntity(itemEntity)) {
                run.stimuli.add(itemEntity);
            }
        }
    }

    private static void stabilizeDimensionSpecificMob(Mob mob) {
        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }

        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }

        if (mob instanceof PolarBear polarBear) {
            polarBear.setBaby(false);
        }
    }

    private static void clearStimuli(BenchmarkRun run) {
        for (Entity stimulus : run.stimuli) {
            for (Mob subject : run.subjects) {
                if (subject instanceof Warden warden) {
                    warden.clearAnger(stimulus);
                }
            }

            if (stimulus instanceof Mob mob) {
                RetoldMobStates.remove(mob);
            }

            stimulus.discard();
        }

        run.stimuli.clear();
    }

    private static void cleanup(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        helper.setTime(run.originalDayTime);
        helper.getLevel().getGameRules().set(
                GameRules.MOB_GRIEFING,
                run.originalMobGriefing,
                helper.getLevel().getServer()
        );
        RetoldWorldData.get(helper.getLevel()).setStage(run.originalStage);
        clearStimuli(run);

        for (Mob subject : run.subjects) {
            RetoldMobStates.remove(subject);
            subject.discard();
        }

        run.subjects.clear();
        clearArena(helper);
        RetoldBehaviorPerf.reset();
    }

    private static void buildArena(
            GameTestHelper helper,
            String mobPath,
            ArenaKind kind
    ) {
        clearArena(helper);

        Block floor = kind == ArenaKind.NETHER ? Blocks.NETHERRACK : Blocks.GRASS_BLOCK;

        if (mobPath.equals("turtle")) {
            floor = Blocks.SAND;
        } else if (mobPath.equals("sniffer")) {
            floor = Blocks.DIRT;
        } else if (kind == ArenaKind.CAVE || kind == ArenaKind.FLYING_CAVE) {
            floor = Blocks.STONE;
        }

        for (int x = ARENA_MIN; x <= ARENA_MAX; x++) {
            for (int z = ARENA_MIN; z <= ARENA_MAX; z++) {
                helper.setBlock(new BlockPos(x, 1, z), floor);
            }
        }

        if (kind == ArenaKind.AQUATIC || kind == ArenaKind.WETLAND) {
            int waterMaxX = kind == ArenaKind.WETLAND ? 7 : ARENA_MAX - 1;

            for (int x = 1; x <= waterMaxX; x++) {
                for (int y = 2; y <= 6; y++) {
                    for (int z = 1; z < ARENA_MAX; z++) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.WATER);
                    }
                }
            }
        }

        int wallTop = kind == ArenaKind.FLYING_CAVE ? 12 : 5;

        for (int offset = ARENA_MIN; offset <= ARENA_MAX; offset++) {
            for (int y = 2; y <= wallTop; y++) {
                Block wall = kind == ArenaKind.AQUATIC || kind == ArenaKind.WETLAND
                        ? Blocks.GLASS
                        : Blocks.STONE;
                helper.setBlock(new BlockPos(ARENA_MIN, y, offset), wall);
                helper.setBlock(new BlockPos(ARENA_MAX, y, offset), wall);
                helper.setBlock(new BlockPos(offset, y, ARENA_MIN), wall);
                helper.setBlock(new BlockPos(offset, y, ARENA_MAX), wall);
            }
        }

        if (kind == ArenaKind.CAVE || kind == ArenaKind.FLYING_CAVE) {
            int roofY = kind == ArenaKind.FLYING_CAVE ? 12 : 7;

            for (int x = ARENA_MIN; x <= ARENA_MAX; x++) {
                for (int z = ARENA_MIN; z <= ARENA_MAX; z++) {
                    helper.setBlock(new BlockPos(x, roofY, z), Blocks.STONE);
                }
            }
        }

        placeEcologyFeatures(helper, mobPath);
    }

    private static void placeForageFeatures(
            GameTestHelper helper,
            String mobPath
    ) {
        Block feature = switch (mobPath) {
            case "panda" -> Blocks.BAMBOO;
            case "parrot" -> Blocks.WHEAT;
            case "bee" -> Blocks.DANDELION;
            case "hoglin", "piglin" -> Blocks.CRIMSON_FUNGUS;
            case "cod", "salmon", "tropical_fish", "pufferfish" -> Blocks.SEAGRASS;
            default -> Blocks.SHORT_GRASS;
        };

        for (int index = 0; index < 12; index++) {
            int x = 2 + index % 6 * 2;
            int z = 2 + index / 6 * 8;
            helper.setBlock(new BlockPos(x, 2, z), feature);
        }
    }

    private static void placeVillagerFoodFixtures(
            GameTestHelper helper,
            BenchmarkRun run
    ) {
        List<BlockPos> storagePositions = List.of(
                new BlockPos(2, 2, 2),
                new BlockPos(12, 2, 2),
                new BlockPos(2, 2, 12),
                new BlockPos(12, 2, 12)
        );

        for (BlockPos pos : storagePositions) {
            helper.setBlock(pos, Blocks.BARREL);
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(
                    helper.absolutePos(pos)
            );

            if (blockEntity instanceof Container container) {
                container.setItem(0, new ItemStack(Items.BREAD, 64));
                container.setChanged();
            }
        }

        GlobalPos home = GlobalPos.of(
                helper.getLevel().dimension(),
                helper.absolutePos(new BlockPos(7, 2, 7))
        );
        BlockPos jobSitePos = new BlockPos(7, 2, 6);
        helper.setBlock(jobSitePos, Blocks.COMPOSTER);
        GlobalPos jobSite = GlobalPos.of(
                helper.getLevel().dimension(),
                helper.absolutePos(jobSitePos)
        );

        for (int index = 0; index < run.subjects.size(); index++) {
            Mob subject = run.subjects.get(index);

            if (subject instanceof Villager villager) {
                villager.getBrain().setMemory(MemoryModuleType.HOME, home);

                if (index % 2 == 0) {
                    villager.setVillagerData(
                            villager.getVillagerData().withProfession(
                                    helper.getLevel().registryAccess(),
                                    VillagerProfession.FARMER
                            )
                    );
                    villager.getBrain().setMemory(
                            MemoryModuleType.JOB_SITE,
                            jobSite
                    );
                    villager.getInventory().setItem(
                            0,
                            new ItemStack(Items.BREAD, 8)
                    );
                }
            }
        }
    }

    private static void placeEcologyFeatures(
            GameTestHelper helper,
            String mobPath
    ) {
        if (mobPath.equals("bee")) {
            helper.setBlock(new BlockPos(3, 3, 3), Blocks.BEE_NEST);
            helper.setBlock(new BlockPos(11, 3, 11), Blocks.BEEHIVE);
            placeForageFeatures(helper, mobPath);
        } else if (mobPath.equals("panda")) {
            placeForageFeatures(helper, mobPath);
        } else if (mobPath.equals("spider") || mobPath.equals("cave_spider")) {
            for (int index = 0; index < 12; index++) {
                helper.setBlock(
                        new BlockPos(2 + index % 6 * 2, 2, 2 + index / 6 * 8),
                        Blocks.COBWEB
                );
            }
        } else if (mobPath.equals("sniffer")) {
            for (int x = 3; x <= 11; x += 2) {
                for (int z = 3; z <= 11; z += 2) {
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.MUD);
                }
            }
        } else if (mobPath.equals("turtle")) {
            for (int x = 1; x <= 6; x++) {
                for (int z = 1; z < ARENA_MAX; z++) {
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.SAND);
                }
            }
        }
    }

    private static void clearArena(GameTestHelper helper) {
        for (int x = ARENA_MIN; x <= ARENA_MAX; x++) {
            for (int y = 0; y <= ARENA_CLEAR_TOP; y++) {
                for (int z = ARENA_MIN; z <= ARENA_MAX; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static Vec3 subjectPosition(
            GameTestHelper helper,
            ArenaKind kind,
            int index
    ) {
        double x = 1.6D + index % 8 * 1.6D;
        double z = 1.6D + index / 8 * 1.8D;
        double y = switch (kind) {
            case AQUATIC -> 2.5D + index % 3 * 1.25D;
            case WETLAND -> index % 2 == 0 ? 2.5D : 2.0D;
            case FLYING_CAVE -> 3.0D + index % 4 * 1.8D;
            default -> 2.0D;
        };

        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static Vec3 stimulusPosition(
            GameTestHelper helper,
            ArenaKind kind,
            int index
    ) {
        double x = 2.0D + (index * 3) % 11;
        double z = 2.0D + (index * 7) % 11;
        double y = kind == ArenaKind.AQUATIC ? 3.5D : 2.2D;
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static Vec3 supportPosition(
            GameTestHelper helper,
            ArenaKind kind,
            int index
    ) {
        double x = 2.0D + index % 4 * 3.2D;
        double z = 3.0D + index / 4 * 7.0D;
        double y = kind == ArenaKind.AQUATIC ? 3.5D : 2.0D;
        return helper.absoluteVec(new Vec3(x, y, z));
    }

    private static ArenaKind arenaKind(String mobPath) {
        return switch (mobPath) {
            case "axolotl", "cod", "dolphin", "drowned", "elder_guardian", "glow_squid", "guardian", "nautilus", "pufferfish", "salmon", "squid", "tropical_fish", "zombie_nautilus" -> ArenaKind.AQUATIC;
            case "frog", "turtle" -> ArenaKind.WETLAND;
            case "bat", "bee", "blaze", "breeze", "ender_dragon", "ghast", "parrot", "phantom", "vex", "wither" -> ArenaKind.FLYING_CAVE;
            case "cave_spider", "creaking", "enderman", "endermite", "shulker", "silverfish", "spider", "warden" -> ArenaKind.CAVE;
            case "hoglin", "magma_cube", "piglin", "piglin_brute", "strider", "wither_skeleton", "zoglin", "zombified_piglin" -> ArenaKind.NETHER;
            default -> ArenaKind.LAND;
        };
    }

    private static boolean isNocturnal(String mobPath) {
        return switch (mobPath) {
            case "bat", "cave_spider", "creaking", "drowned", "enderman", "endermite", "husk", "phantom", "silverfish", "skeleton", "spider", "stray", "warden", "zombie", "zombie_villager" -> true;
            default -> false;
        };
    }

    private static Item preferredFood(
            String mobPath,
            RetoldMobProfileType profileType
    ) {
        if (mobPath.equals("bat")) {
            return Items.SPIDER_EYE;
        }

        if (mobPath.equals("nautilus")) {
            return Items.COD;
        }

        if (mobPath.equals("squid") || mobPath.equals("glow_squid")) {
            return Items.COD;
        }

        if (mobPath.equals("strider")) {
            return Items.WARPED_FUNGUS;
        }

        return switch (profileType) {
            case HUNGRY_GRAZER -> Items.WHEAT;
            case SMALL_FORAGER -> Items.WHEAT_SEEDS;
            case PARROT_FORAGER -> Items.WHEAT_SEEDS;
            case PACK_PREDATOR, AQUATIC_PREDATOR, HUNGRY_SWARM_PREDATOR, SOLO_OPPORTUNIST -> Items.BEEF;
            case HIVE_COLONY -> Items.DANDELION;
            case NETHER_HUNGRY -> Items.CRIMSON_FUNGUS;
            case UNDEAD_HUNGRY -> Items.ROTTEN_FLESH;
            case SLIME_HUNGRY -> Items.COBBLESTONE;
            case PANDA_BAMBOO -> Items.BAMBOO;
            case AQUATIC_TERRITORY_GUARD -> Items.COD;
            case VILLAGER_COMMUNAL -> null;
            default -> null;
        };
    }

    private static EntityType<?> huntTargetType(String mobPath) {
        return switch (mobPath) {
            case "wolf" -> EntityTypes.SHEEP;
            case "fox" -> EntityTypes.CHICKEN;
            case "cat", "ocelot" -> EntityTypes.RABBIT;
            case "spider", "cave_spider" -> EntityTypes.COW;
            case "dolphin", "axolotl" -> EntityTypes.COD;
            case "zombie_nautilus" -> EntityTypes.COD;
            case "bat" -> EntityTypes.SPIDER;
            case "frog" -> EntityTypes.SLIME;
            case "guardian", "elder_guardian" -> EntityTypes.SQUID;
            case "iron_golem", "snow_golem" -> EntityTypes.ZOMBIE;
            case "evoker", "illusioner", "pillager", "ravager", "vex", "vindicator", "witch" -> EntityTypes.VILLAGER;
            default -> EntityTypes.COW;
        };
    }

    private static EntityType<?> dangerTargetType(String mobPath) {
        return switch (mobPath) {
            case "cow", "sheep", "pig", "chicken", "rabbit", "horse", "donkey", "mule", "llama", "trader_llama", "camel", "goat", "mooshroom", "sniffer", "panda", "parrot", "armadillo", "turtle", "frog", "axolotl", "dolphin", "polar_bear", "bee", "wolf", "fox", "cat", "ocelot", "iron_golem", "snow_golem", "villager" -> EntityTypes.ZOMBIE;
            default -> EntityTypes.IRON_GOLEM;
        };
    }

    private static long currentClockTime(ServerLevel level) {
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow();
        return level.getServer().clockManager().getTotalTicks(clock);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
    }

    private enum ArenaKind {
        LAND,
        AQUATIC,
        WETLAND,
        CAVE,
        FLYING_CAVE,
        NETHER
    }

    private enum BenchmarkPhase {
        IDLE_REST("idle_rest"),
        DROPPED_FOOD_FORAGE("dropped_food_forage"),
        HUNT_TARGETING("hunt_targeting"),
        DANGER_SOCIAL("danger_social"),
        HABITAT_DAY_NIGHT("habitat_day_night");

        private final String serializedName;

        BenchmarkPhase(String serializedName) {
            this.serializedName = serializedName;
        }
    }

    private static final class BenchmarkRun {
        private final String mobPath;
        private final EntityType<?> entityType;
        private final RetoldMobProfile profile;
        private final ArenaKind arenaKind;
        private final long originalDayTime;
        private final boolean originalMobGriefing;
        private final RetoldWorldStage originalStage;
        private final List<Mob> subjects = new ArrayList<>(SUBJECT_COUNT);
        private final List<Entity> stimuli = new ArrayList<>();
        private final Map<BenchmarkPhase, PhaseResult> results =
                new EnumMap<>(BenchmarkPhase.class);

        private BenchmarkPhase currentPhase;
        private long phaseStartedAtNanos;
        private int phaseStartedAtServerTick;

        private BenchmarkRun(
                String mobPath,
                EntityType<?> entityType,
                RetoldMobProfile profile,
                ArenaKind arenaKind,
                long originalDayTime,
                boolean originalMobGriefing,
                RetoldWorldStage originalStage
        ) {
            this.mobPath = mobPath;
            this.entityType = entityType;
            this.profile = profile;
            this.arenaKind = arenaKind;
            this.originalDayTime = originalDayTime;
            this.originalMobGriefing = originalMobGriefing;
            this.originalStage = originalStage;
        }
    }

    private record PhaseResult(
            int observedTicks,
            double averageTickMillis,
            RetoldBehaviorPerf.AiWorkSnapshot work,
            long blockTargetPositionsChecked,
            int activeSubjects,
            int diggingWardens
    ) {
    }

    /** Keeps the no-player GameTest fixture from using distance despawning. */
    private static final class BenchmarkWarden extends Warden {
        private BenchmarkWarden(ServerLevel level) {
            super(EntityTypes.WARDEN, level);
        }

        @Override
        public void checkDespawn() {
        }

        @Override
        public boolean removeWhenFarAway(double distanceSquared) {
            return false;
        }

        @Override
        public boolean isAlwaysTicking() {
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
