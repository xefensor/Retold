package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.event.RetoldUndeadEvents;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
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
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldWildfireGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldWildfireGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true);
        event.registerTest(
                id("wildfire_targets_undead_and_drops_fire_artifact"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::targetsUndeadAndDropsArtifact
                )
        );
        event.registerTest(
                id("wildfire_encounter_targets_only_undead_and_players"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::encounterTargetsOnlyUndeadAndPlayers
                )
        );
        event.registerTest(
                id("zombified_piglins_take_blaze_and_wildfire_fire_damage"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::zombifiedPiglinsTakeRemnantFireDamage
                )
        );
        event.registerTest(
                id("wither_skeletons_take_remnant_fireball_damage"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::witherSkeletonsTakeRemnantFireballDamage
                )
        );
        event.registerTest(
                id("wildfire_targets_and_damages_visible_ghast"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 100, 0, true),
                        RetoldWildfireGameTests::wildfireTargetsVisibleGhast
                )
        );
        event.registerTest(
                id("natural_wildfire_spawns_with_blaze_escort"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::naturalWildfireSpawnsWithBlazeEscort
                )
        );
        event.registerTest(
                id("wildfire_uses_independent_stage_two_spawner"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::usesIndependentStageTwoSpawner
                )
        );
        event.registerTest(
                id("wildfire_patrols_with_single_file_blaze_escort"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 700, 0, true),
                        RetoldWildfireGameTests::wildfirePatrolsWithSingleFileBlazeEscort
                )
        );
        event.registerTest(
                id("wildfire_blaze_escorts_follow_into_combat"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 500, 0, true),
                        RetoldWildfireGameTests::wildfireBlazeEscortsFollowIntoCombat
                )
        );
        event.registerTest(
                id("wildfire_rises_out_of_lava_when_idle"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 100, 0, true),
                        RetoldWildfireGameTests::wildfireRisesOutOfLavaWhenIdle
                )
        );
        event.registerTest(
                id("wildfire_repositions_during_ranged_combat"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 120, 0, true),
                        RetoldWildfireGameTests::wildfireRepositionsDuringRangedCombat
                )
        );
        event.registerTest(
                id("wildfire_submerges_heals_fully_and_resurfaces"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 600, 0, true),
                        RetoldWildfireGameTests::wildfireSubmergesHealsFullyAndResurfaces
                )
        );
        event.registerTest(
                id("wildfire_shields_gate_damage_and_shockwave_hostiles"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::shieldsGateDamageAndShockwaveHostiles
                )
        );
        event.registerTest(
                id("wildfire_requires_deep_lava_submersion_for_recovery"),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 460, 0, true),
                        RetoldWildfireGameTests::requiresDeepLavaSubmersionForRecovery
                )
        );
        event.registerTest(
                id("wounded_wildfire_retreats_toward_lava"),
                new InlineGameTest(
                        testData,
                        RetoldWildfireGameTests::woundedWildfireRetreatsTowardLava
                )
        );
    }

    private static void targetsUndeadAndDropsArtifact(GameTestHelper helper) {
        clearCombatArena(helper);
        ServerLevel level = helper.getLevel();
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 2, 3, 2);
        var blaze = helper.spawn(EntityTypes.BLAZE, 4, 3, 2);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 6, 3, 2);

        helper.assertValueEqual(
                wildfire.getMaxHealth(),
                120.0F,
                "The Wildfire must retain its boss-tier health"
        );
        helper.assertValueEqual(
                wildfire.getAttributeValue(Attributes.ARMOR),
                10.0D,
                "The Wildfire must retain its boss-tier armor"
        );
        helper.assertValueEqual(
                wildfire.getAttributeValue(Attributes.ATTACK_DAMAGE),
                10.0D,
                "The Wildfire must retain its stronger contact damage"
        );
        helper.assertValueEqual(
                wildfire.getBbWidth(),
                1.0F,
                "The Wildfire must retain its enlarged hitbox width"
        );
        helper.assertValueEqual(
                wildfire.getBbHeight(),
                3.0F,
                "The Wildfire must retain its enlarged hitbox height"
        );
        helper.assertValueEqual(
                RetoldFactionMembers.getFaction(wildfire),
                RetoldFaction.NETHER_REMNANTS,
                "Wildfire must belong to the Nether Remnants"
        );
        helper.assertTrue(
                !RetoldFactionRelations.shouldAttack(wildfire, blaze)
                        && RetoldFactionRelations.shouldAttack(wildfire, zombie),
                "Wildfire must remain allied with Blazes and hostile to Undead"
        );

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        wildfire.getTarget() == zombie,
                        "Wildfire must acquire a nearby Undead target through faction combat"
                ))
                .thenExecute(() -> {
                    Vec3 position = wildfire.position();

                    try {
                        wildfire.dropCustomDeathLoot(
                                level,
                                level.damageSources().generic(),
                                true
                        );
                        List<ItemEntity> drops = level.getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(position, position).inflate(2.0D)
                        );
                        helper.assertValueEqual(
                                drops.stream()
                                        .filter(drop -> drop.getItem().is(
                                                RetoldBlocks.NETHER_REACTOR_CORE
                                        ))
                                        .count(),
                                1L,
                                "A defeated Wildfire must drop exactly one Nether Reactor Core"
                        );
                    } finally {
                        level.getEntitiesOfClass(
                                ItemEntity.class,
                                new AABB(position, position).inflate(2.0D)
                        ).forEach(Entity::discard);
                        wildfire.discard();
                        blaze.discard();
                        zombie.discard();
                    }
                })
                .thenSucceed();
    }

    private static void encounterTargetsOnlyUndeadAndPlayers(GameTestHelper helper) {
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(3.5D, 3.0D, 3.5D),
                EntitySpawnReason.NATURAL
        );
        List<net.minecraft.world.entity.monster.Blaze> escorts = helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Blaze.class,
                        wildfire.getBoundingBox().inflate(6.0D),
                        blaze -> blaze != wildfire
                                && WildfireEncounterTargets.isEncounterMember(blaze)
                );
        var ordinaryBlaze = helper.spawn(EntityTypes.BLAZE, 1, 3, 7);
        var pillager = helper.spawn(EntityTypes.PILLAGER, 7, 3, 3);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 7, 3, 5);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 playerPosition = helper.absoluteVec(new Vec3(7.5D, 3.0D, 7.5D));

        player.snapTo(
                playerPosition.x(),
                playerPosition.y(),
                playerPosition.z(),
                0.0F,
                0.0F
        );
        wildfire.setNoAi(true);
        ordinaryBlaze.setNoAi(true);
        pillager.setNoAi(true);
        zombie.setNoAi(true);
        escorts.forEach(escort -> escort.setNoAi(true));

        helper.assertTrue(
                !escorts.isEmpty(),
                "A natural Wildfire must provide at least one marked Blaze escort"
        );
        var escort = escorts.getFirst();

        helper.assertTrue(
                WildfireEncounterTargets.shouldBlockTarget(wildfire, pillager)
                        && WildfireEncounterTargets.shouldBlockTarget(escort, pillager),
                "The Wildfire and its Blaze escort must reject non-Undead mobs"
        );
        helper.assertFalse(
                WildfireEncounterTargets.shouldBlockTarget(ordinaryBlaze, pillager),
                "The encounter restriction must not change an ordinary Blaze"
        );
        helper.assertFalse(
                wildfire.canAttack(pillager) || escort.canAttack(pillager),
                "Encounter members must reject non-Undead mobs through canAttack"
        );
        helper.assertFalse(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        pillager,
                        RetoldTargetSource.FACTION_COMBAT
                ) || RetoldFactionTargetMemory.trySetTarget(
                        escort,
                        pillager,
                        RetoldTargetSource.RETALIATION
                ),
                "Faction combat and retaliation must not bypass the encounter target gate"
        );
        helper.assertTrue(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        zombie,
                        RetoldTargetSource.FACTION_COMBAT
                ) && RetoldFactionTargetMemory.trySetTarget(
                        escort,
                        zombie,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "The Wildfire and its Blaze escort must still accept Undead targets"
        );
        wildfire.setTarget(null);
        escort.setTarget(null);
        helper.assertTrue(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        player,
                        RetoldTargetSource.RETALIATION
                ) && RetoldFactionTargetMemory.trySetTarget(
                        escort,
                        player,
                        RetoldTargetSource.RETALIATION
                ),
                "The Wildfire and its Blaze escort must still accept survival players"
        );

        try {
            helper.succeed();
        } finally {
            wildfire.discard();
            escorts.forEach(Entity::discard);
            ordinaryBlaze.discard();
            pillager.discard();
            zombie.discard();
            player.discard();
        }
    }

    private static void zombifiedPiglinsTakeRemnantFireDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = worldData.getStage();
        worldData.setStage(RetoldWorldStage.STAGE_2);
        var blaze = helper.spawn(EntityTypes.BLAZE, 2, 3, 2);
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 2, 3, 5);
        var blazeTarget = helper.spawn(EntityTypes.ZOMBIFIED_PIGLIN, 5, 3, 2);
        var wildfireTarget = helper.spawn(EntityTypes.ZOMBIFIED_PIGLIN, 5, 3, 5);

        blaze.setNoAi(true);
        wildfire.setNoAi(true);
        blazeTarget.setNoAi(true);
        wildfireTarget.setNoAi(true);

        helper.assertTrue(
                blaze.fireImmune() && wildfire.fireImmune(),
                "Blazes and Wildfires must retain their own fire immunity"
        );
        helper.assertTrue(
                !blazeTarget.fireImmune() && !wildfireTarget.fireImmune(),
                "Zombified Piglins must no longer inherit vanilla fire immunity"
        );
        helper.assertTrue(
                RetoldUndeadEvents.shouldPreventSunBurn(blazeTarget)
                        && RetoldUndeadEvents.shouldPreventSunBurn(wildfireTarget),
                "Stage 2 Zombified Piglins must retain the sunlight-only protection"
        );

        SmallFireball blazeFireball = new SmallFireball(level, blaze, Vec3.ZERO);
        SmallFireball wildfireFireball = new SmallFireball(level, wildfire, Vec3.ZERO);
        boolean blazeDamaged = blazeTarget.hurtServer(
                level,
                level.damageSources().fireball(blazeFireball, blaze),
                5.0F
        );
        boolean wildfireDamaged = wildfireTarget.hurtServer(
                level,
                level.damageSources().fireball(wildfireFireball, wildfire),
                5.0F
        );

        blazeTarget.igniteForSeconds(5.0F);
        wildfireTarget.igniteForSeconds(5.0F);

        helper.assertTrue(
                blazeDamaged && blazeTarget.getHealth() < blazeTarget.getMaxHealth(),
                "A Blaze-owned fireball must damage a Zombified Piglin"
        );
        helper.assertTrue(
                wildfireDamaged && wildfireTarget.getHealth() < wildfireTarget.getMaxHealth(),
                "A Wildfire-owned fireball must damage a Zombified Piglin"
        );
        helper.assertTrue(
                wildfireTarget.getMaxHealth() - wildfireTarget.getHealth()
                        > blazeTarget.getMaxHealth() - blazeTarget.getHealth(),
                "A Wildfire-owned fireball must hit harder than a Blaze-owned fireball"
        );

        helper.runAfterDelay(20, () -> {
            try {
                helper.assertTrue(
                        blazeTarget.getRemainingFireTicks() > 0
                                && wildfireTarget.getRemainingFireTicks() > 0,
                        "Zombified Piglins must remain burning after a Remnant fireball hit"
                );
                helper.succeed();
            } finally {
                blazeFireball.discard();
                wildfireFireball.discard();
                blaze.discard();
                wildfire.discard();
                blazeTarget.discard();
                wildfireTarget.discard();
                worldData.setStage(originalStage);
            }
        });
    }

    private static void witherSkeletonsTakeRemnantFireballDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var blaze = helper.spawn(EntityTypes.BLAZE, 2, 3, 2);
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 2, 3, 5);
        var blazeTarget = helper.spawn(EntityTypes.WITHER_SKELETON, 5, 3, 2);
        var wildfireTarget = helper.spawn(EntityTypes.WITHER_SKELETON, 5, 3, 5);
        SmallFireball blazeFireball = new SmallFireball(level, blaze, Vec3.ZERO);
        SmallFireball wildfireFireball = new SmallFireball(level, wildfire, Vec3.ZERO);

        blaze.setNoAi(true);
        wildfire.setNoAi(true);
        blazeTarget.setNoAi(true);
        wildfireTarget.setNoAi(true);

        helper.assertTrue(
                blazeTarget.fireImmune() && wildfireTarget.fireImmune(),
                "Wither Skeletons must retain ordinary fire and lava immunity"
        );

        boolean blazeDamaged = blazeTarget.hurtServer(
                level,
                level.damageSources().fireball(blazeFireball, blaze),
                5.0F
        );
        boolean wildfireDamaged = wildfireTarget.hurtServer(
                level,
                level.damageSources().fireball(wildfireFireball, wildfire),
                5.0F
        );

        helper.assertTrue(
                blazeDamaged && blazeTarget.getHealth() < blazeTarget.getMaxHealth(),
                "A Blaze-owned Small Fireball must damage a Wither Skeleton"
        );
        helper.assertTrue(
                wildfireDamaged && wildfireTarget.getHealth() < wildfireTarget.getMaxHealth(),
                "A Wildfire-owned Small Fireball must damage a Wither Skeleton"
        );

        float healthAfterFireballs = blazeTarget.getHealth();
        boolean ordinaryFireDamaged = blazeTarget.hurtServer(
                level,
                level.damageSources().inFire(),
                5.0F
        );
        helper.assertTrue(
                !ordinaryFireDamaged && blazeTarget.getHealth() == healthAfterFireballs,
                "The fireball exception must not remove ordinary Wither Skeleton fire immunity"
        );

        blazeFireball.discard();
        wildfireFireball.discard();
        blaze.discard();
        wildfire.discard();
        blazeTarget.discard();
        wildfireTarget.discard();
        helper.succeed();
    }

    private static void wildfireTargetsVisibleGhast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos arenaCenter = helper.absolutePos(new BlockPos(30, 6, 4));
        ChunkPos arenaTicketCenter = new ChunkPos(
                arenaCenter.getX() >> 4,
                arenaCenter.getZ() >> 4
        );
        level.getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                arenaTicketCenter,
                4
        );
        clearDistantGhastArena(helper);
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 2, 6, 2);
        var blaze = helper.spawn(EntityTypes.BLAZE, 2, 6, 6);
        var distantZombie = helper.spawn(EntityTypes.ZOMBIE, 52, 6, 2);
        var ghast = helper.spawn(EntityTypes.GHAST, 58, 6, 2);

        wildfire.setNoAi(true);
        wildfire.setNoGravity(true);
        blaze.setNoAi(true);
        blaze.setNoGravity(true);
        distantZombie.setNoAi(true);
        distantZombie.setNoGravity(true);
        ghast.setNoAi(true);
        ghast.setNoGravity(true);

        helper.assertTrue(
                RetoldFactionMembers.isUndead(ghast)
                        && RetoldFactionRelations.shouldAttack(wildfire, ghast),
                "A Ghast must be an enemy Undead target for a Wildfire"
        );

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        wildfire.getTarget() == ghast,
                        "A Wildfire must acquire a visible Ghast across its full combat range"
                                + " (distance squared: "
                                + wildfire.distanceToSqr(ghast)
                                + ", current target: " + wildfire.getTarget()
                                + ", ownership: "
                                + RetoldFactionTargetMemory.debugOwnershipText(wildfire) + ")"
                ))
                .thenExecuteAfter(40, () -> {
                    helper.assertTrue(
                            wildfire.getTarget() == ghast,
                            "A Wildfire must retain a distant Ghast target instead of becoming helpless"
                    );
                    helper.assertTrue(
                            RetoldFactionTargetMemory.isOwnedByAny(
                                    wildfire,
                                    ghast,
                                    RetoldTargetSource.FACTION_COMBAT
                            ),
                            "Distant Ghast retention must remain owned by faction combat"
                    );
                })
                .thenExecute(() -> {
                    double followRange = wildfire.getAttributeValue(Attributes.FOLLOW_RANGE);

                    helper.assertTrue(
                            wildfire.getSensing().hasLineOfSight(ghast),
                            "A Wildfire must have line of sight to the test Ghast"
                    );
                    helper.assertTrue(
                            wildfire.distanceToSqr(ghast) < followRange * followRange,
                            "The distant Ghast must remain inside the Wildfire's ranged-combat envelope"
                    );
                    helper.assertTrue(
                            wildfire.getTarget() != distantZombie,
                            "The Ghast exception must not widen ordinary faction targeting"
                    );
                    helper.assertTrue(
                            wildfire.canAttack(ghast),
                            "A Wildfire must be allowed to attack the test Ghast"
                    );
                    helper.assertTrue(
                            blaze.canAttack(ghast),
                            "A Blaze must be allowed to attack an enemy Ghast"
                    );
                    helper.assertFalse(
                            wildfire.isAlliedTo(ghast),
                            "A Wildfire and an enemy Ghast must not be vanilla allies"
                    );
                    float healthBefore = ghast.getHealth();
                    SmallFireball fireball = new SmallFireball(level, wildfire, Vec3.ZERO);
                    boolean damaged = ghast.hurtServer(
                            level,
                            level.damageSources().fireball(fireball, wildfire),
                            2.0F
                    );
                    fireball.discard();
                    helper.assertTrue(
                            damaged && ghast.getHealth() < healthBefore,
                            "A Wildfire Small Fireball must damage its fire-immune Ghast target"
                    );
                    ghast.invulnerableTime = 0;
                    float healthAfterWildfireHit = ghast.getHealth();
                    SmallFireball blazeFireball = new SmallFireball(level, blaze, Vec3.ZERO);
                    boolean blazeDamaged = ghast.hurtServer(
                            level,
                            level.damageSources().fireball(blazeFireball, blaze),
                            2.0F
                    );
                    blazeFireball.discard();
                    helper.assertTrue(
                            blazeDamaged && ghast.getHealth() < healthAfterWildfireHit,
                            "A Blaze Small Fireball must damage its fire-immune Ghast target"
                    );
                    helper.assertTrue(
                            ghast.fireImmune(),
                            "The direct-hit exception must preserve ordinary Ghast fire immunity"
                    );
                })
                .thenExecute(() -> {
                    wildfire.discard();
                    blaze.discard();
                    distantZombie.discard();
                    ghast.discard();
                    level.getChunkSource().removeTicketWithRadius(
                            TicketType.FORCED,
                            arenaTicketCenter,
                            4
                    );
                })
                .thenSucceed();
    }

    private static void clearDistantGhastArena(GameTestHelper helper) {
        for (int x = 0; x <= 62; x++) {
            for (int y = 2; y <= 12; y++) {
                for (int z = 0; z <= 8; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void clearCombatArena(GameTestHelper helper) {
        for (int x = 0; x <= 9; x++) {
            for (int y = 2; y <= 9; y++) {
                for (int z = 0; z <= 9; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void naturalWildfireSpawnsWithBlazeEscort(GameTestHelper helper) {
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(2.5D, 3.0D, 2.5D),
                EntitySpawnReason.NATURAL
        );
        wildfire.setNoAi(true);

        helper.assertFalse(
                Wildfire.isNaturalSpawnAreaClear(helper.getLevel(), wildfire.blockPosition()),
                "A living Wildfire must suppress another natural Wildfire within 128 blocks"
        );
        helper.assertTrue(
                Wildfire.isNaturalSpawnAreaClear(
                        helper.getLevel(),
                        wildfire.blockPosition().offset(
                                Wildfire.NATURAL_SPAWN_EXCLUSION_RADIUS + 1,
                                0,
                                0
                        )
                ),
                "A Wildfire beyond the exclusion radius must not suppress natural placement"
        );

        helper.runAfterDelay(1, () -> {
            List<net.minecraft.world.entity.monster.Blaze> blazes = helper.getLevel()
                    .getEntitiesOfClass(
                            net.minecraft.world.entity.monster.Blaze.class,
                            wildfire.getBoundingBox().inflate(5.0D),
                            blaze -> blaze != wildfire
                    );

            try {
                helper.assertValueInBetween(
                        3,
                        blazes.size(),
                        5,
                        "A naturally spawned Wildfire must arrive with three to five Blazes"
                );
                helper.succeed();
            } finally {
                blazes.forEach(Entity::discard);
                wildfire.discard();
            }
        });
    }

    private static void usesIndependentStageTwoSpawner(GameTestHelper helper) {
        ServerLevel level = java.util.Objects.requireNonNull(
                helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER),
                "GameTest server must provide the Nether level"
        );
        RetoldWorldData data = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = data.getStage();
        Difficulty originalDifficulty = level.getDifficulty();
        boolean originalSpawnMobs = level.getGameRules().get(GameRules.SPAWN_MOBS);
        BlockPos spawnPos = new BlockPos(8, 64, 8);
        ChunkPos ticketCenter = new ChunkPos(
                spawnPos.getX() >> 4,
                spawnPos.getZ() >> 4
        );
        List<Entity> crowd = new java.util.ArrayList<>();
        Runnable cleanup = () -> {
            data.setStage(originalStage);
            level.getServer().setDifficulty(originalDifficulty, true);
            level.getGameRules().set(
                    GameRules.SPAWN_MOBS,
                    originalSpawnMobs,
                    level.getServer()
            );
            crowd.forEach(Entity::discard);
            level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Blaze.class,
                    new AABB(spawnPos).inflate(32.0D)
            ).forEach(Entity::discard);
            level.getChunkSource().removeTicketWithRadius(
                    TicketType.FORCED,
                    ticketCenter,
                    1
            );
        };

        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticketCenter, 1);

        try {
            level.getServer().setDifficulty(Difficulty.NORMAL, true);
            level.getGameRules().set(
                    GameRules.SPAWN_MOBS,
                    true,
                    level.getServer()
            );

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(
                            spawnPos.offset(x, -1, z),
                            Blocks.NETHERRACK.defaultBlockState()
                    );

                    for (int y = 0; y <= 3; y++) {
                        level.setBlockAndUpdate(spawnPos.offset(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }

            for (int index = 0; index < 80; index++) {
                var blaze = EntityTypes.BLAZE.create(level, EntitySpawnReason.MOB_SUMMONED);

                if (blaze != null) {
                    blaze.setNoAi(true);
                    blaze.setNoGravity(true);
                    blaze.setPos(
                            spawnPos.getX() + 10.5D + index % 4,
                            spawnPos.getY() + index / 16,
                            spawnPos.getZ() + 10.5D + index / 4 % 4
                    );
                    level.addFreshEntity(blaze);
                    crowd.add(blaze);
                }
            }

            helper.assertValueEqual(
                    crowd.size(),
                    80,
                    "The test must exceed the ordinary 70-monster cap"
            );

            data.setStage(RetoldWorldStage.STAGE_1);
            helper.assertFalse(
                    WildfireSpawnEvents.spawnAt(level, spawnPos) != null,
                    "The dedicated Wildfire spawner must remain disabled in Stage 1"
            );

            data.setStage(RetoldWorldStage.STAGE_2);
            helper.assertTrue(
                    WildfireSpawnEvents.isDedicatedSpawningEnabled(level),
                    "The dedicated Wildfire spawner must be enabled in a Stage 2 Nether"
            );
            helper.assertTrue(
                    SpawnPlacements.isSpawnPositionOk(
                            RetoldEntityTypes.WILDFIRE.get(),
                            level,
                            spawnPos
                    ),
                    "The prepared Nether floor must satisfy Wildfire placement"
            );
            helper.assertTrue(
                    SpawnPlacements.checkSpawnRules(
                            RetoldEntityTypes.WILDFIRE.get(),
                            level,
                            EntitySpawnReason.NATURAL,
                            spawnPos,
                            level.getRandom()
                    ),
                    "The prepared position must satisfy Stage 2 Wildfire spawn rules"
            );
            Wildfire wildfire = WildfireSpawnEvents.spawnAt(level, spawnPos);
            helper.assertTrue(
                    wildfire != null,
                    "The dedicated Stage 2 spawner must ignore the ordinary monster population cap"
            );
            helper.assertFalse(
                    wildfire.isRemoved(),
                    "The independent spawn attempt must create a live Wildfire encounter"
            );
            helper.succeed();
            cleanup.run();
        } catch (RuntimeException exception) {
            cleanup.run();
            throw exception;
        }
    }

    private static void wildfireRisesOutOfLavaWhenIdle(GameTestHelper helper) {
        clearLavaSurfaceArena(helper);
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(4.5D, 3.0D, 4.5D),
                EntitySpawnReason.MOB_SUMMONED
        );
        double startingY = wildfire.getY();

        wildfire.setDeltaMovement(0.0D, -0.2D, 0.0D);
        helper.assertTrue(
                helper.getLevel().getFluidState(wildfire.blockPosition()).is(FluidTags.LAVA),
                "The Wildfire must begin inside the prepared lava column"
        );

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            wildfire.getY() > startingY + 1.0D,
                            "An idle Wildfire must rise toward the lava surface"
                                    + " (y=" + wildfire.getY()
                                    + ", inLava=" + wildfire.isInLava()
                                    + ", fluidHeight=" + wildfire.getFluidHeight(FluidTags.LAVA)
                                    + ", movement=" + wildfire.getDeltaMovement() + ")"
                    );
                    helper.assertFalse(
                            wildfire.isEyeInFluid(FluidTags.LAVA),
                            "An idle Wildfire must keep its head above lava"
                    );
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        wildfire.discard();
                    }
                });
    }

    private static void wildfireRepositionsDuringRangedCombat(GameTestHelper helper) {
        clearCombatMovementArena(helper);
        BlockPos arenaCenter = helper.absolutePos(new BlockPos(15, 6, 10));
        ChunkPos ticketCenter = new ChunkPos(
                arenaCenter.getX() >> 4,
                arenaCenter.getZ() >> 4
        );
        helper.getLevel().getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                ticketCenter,
                2
        );
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(5.5D, 6.0D, 10.5D),
                EntitySpawnReason.MOB_SUMMONED
        );
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 20, 3, 10);
        Vec3 startingPosition = wildfire.position();

        wildfire.setNoGravity(true);
        zombie.setNoAi(true);
        var zombieHealth = zombie.getAttribute(Attributes.MAX_HEALTH);
        helper.assertTrue(
                zombieHealth != null,
                "The test Zombie must expose its maximum-health attribute"
        );
        zombieHealth.setBaseValue(200.0D);
        zombie.setHealth(200.0F);
        helper.assertTrue(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        zombie,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "The Wildfire must accept the test Undead combat target"
        );

        helper.startSequence()
                .thenWaitUntil(() -> {
                    Vec3 movement = wildfire.position().subtract(startingPosition);

                    helper.assertTrue(
                            RetoldAiControl.isControlledAsBy(
                                    wildfire,
                                    RetoldAiControlMode.ATTACK,
                                    RetoldAiControlOwner.WILDFIRE_COMBAT
                            ),
                            "Ranged combat must own Wildfire repositioning"
                                    + " (control=" + RetoldAiControl.getMode(wildfire)
                                    + "/" + RetoldAiControl.getOwner(wildfire)
                                    + ", target=" + wildfire.getTarget()
                                    + ", canAttack=" + wildfire.canAttack(zombie)
                                    + ", health=" + wildfire.getHealth()
                                    + ", shields=" + wildfire.getShieldCount()
                                    + ", lavaTarget="
                                    + wildfire.hasLavaRetreatTarget(helper.getLevel()) + ")"
                    );
                    helper.assertTrue(
                            movement.horizontalDistanceSqr() > 1.0D,
                            "A Wildfire must move laterally instead of firing from one fixed point"
                    );
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        RetoldAiControl.clear(wildfire);
                        wildfire.discard();
                        zombie.discard();
                        helper.getLevel().getChunkSource().removeTicketWithRadius(
                                TicketType.FORCED,
                                ticketCenter,
                                2
                        );
                    }
                });
    }

    private static void wildfireSubmergesHealsFullyAndResurfaces(GameTestHelper helper) {
        clearLavaSurfaceArena(helper);
        BlockPos arenaCenter = helper.absolutePos(new BlockPos(4, 4, 4));
        ChunkPos ticketCenter = new ChunkPos(
                arenaCenter.getX() >> 4,
                arenaCenter.getZ() >> 4
        );
        helper.getLevel().getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                ticketCenter,
                2
        );
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(4.5D, 4.0D, 4.5D),
                EntitySpawnReason.MOB_SUMMONED
        );
        var combatTarget = helper.spawn(EntityTypes.ZOMBIE, 1, 3, 1);
        double[] submergedY = new double[1];

        helper.getLevel().getChunkAt(wildfire.blockPosition());
        helper.getLevel().getChunkAt(combatTarget.blockPosition());
        combatTarget.setNoAi(true);
        var combatTargetHealth = combatTarget.getAttribute(Attributes.MAX_HEALTH);
        helper.assertTrue(
                combatTargetHealth != null,
                "The recovery combat target must expose maximum health"
        );
        combatTargetHealth.setBaseValue(200.0D);
        combatTarget.setHealth(200.0F);
        wildfire.setShieldCount(Wildfire.MAX_SHIELDS - 1);
        wildfire.setHealth(wildfire.getMaxHealth() * 0.5F);
        helper.assertTrue(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        combatTarget,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "The wounded Wildfire must begin with an active combat target"
        );
        helper.assertTrue(
                helper.getLevel().getFluidState(wildfire.blockPosition()).is(FluidTags.LAVA),
                "The recovery test must begin with the Wildfire touching the prepared lava pool"
        );
        helper.assertFalse(
                wildfire.isEyeInFluid(FluidTags.LAVA),
                "The recovery test must begin before the Wildfire submerges"
        );
        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            wildfire.isLavaRecoveryActive(),
                            "A wounded Wildfire touching lava must enter recovery"
                                    + " (position=" + wildfire.position()
                                    + ", inLava=" + wildfire.isInLava()
                                    + ", control=" + RetoldAiControl.getMode(wildfire)
                                    + "/" + RetoldAiControl.getOwner(wildfire)
                                    + ", health=" + wildfire.getHealth()
                                    + ", shields=" + wildfire.getShieldCount()
                                    + ", target=" + wildfire.getTarget()
                                    + ", tickCount=" + wildfire.tickCount
                                    + ", noAi=" + wildfire.isNoAi()
                                    + ", moveWanted=" + wildfire.getMoveControl().hasWanted()
                                    + ", movement=" + wildfire.getDeltaMovement() + ")"
                    );
                    helper.assertTrue(
                            wildfire.isEyeInFluid(FluidTags.LAVA),
                            "Lava recovery must pull the Wildfire's head below the surface"
                    );
                    helper.assertTrue(
                            wildfire.getTarget() == null,
                            "Submerged recovery must clear the Wildfire's combat target"
                    );
                    helper.assertFalse(
                            RetoldFactionTargetMemory.trySetTarget(
                                    wildfire,
                                    combatTarget,
                                    RetoldTargetSource.FACTION_COMBAT
                            ),
                            "A submerged Wildfire must reject new combat targets"
                    );
                })
                .thenExecute(() -> submergedY[0] = wildfire.getY())
                .thenWaitUntil(() -> {
                    helper.assertValueEqual(
                            wildfire.getShieldCount(),
                            Wildfire.MAX_SHIELDS,
                            "A submerged Wildfire must restore every shield"
                                    + " (position=" + wildfire.position()
                                    + ", inLava=" + wildfire.isInLava()
                                    + ", eyeInLava=" + wildfire.isEyeInFluid(FluidTags.LAVA)
                                    + ", health=" + wildfire.getHealth()
                                    + ", recovering=" + wildfire.isLavaRecoveryActive() + ")"
                    );
                    helper.assertTrue(
                            wildfire.getHealth() >= wildfire.getMaxHealth(),
                            "A submerged Wildfire must restore all body health"
                    );
                    helper.assertFalse(
                            wildfire.isLavaRecoveryActive(),
                            "Full restoration must end the lava recovery state"
                    );
                    helper.assertFalse(
                            wildfire.isEyeInFluid(FluidTags.LAVA),
                            "A fully restored Wildfire must rise above the lava surface"
                    );
                    helper.assertTrue(
                            wildfire.getY() > submergedY[0] + 1.0D,
                            "A fully restored Wildfire must rise from its submerged depth"
                    );
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        RetoldAiControl.clear(wildfire);
                        wildfire.discard();
                        combatTarget.discard();
                        helper.getLevel().getChunkSource().removeTicketWithRadius(
                                TicketType.FORCED,
                                ticketCenter,
                                2
                        );
                    }
                });
    }

    private static void clearLavaSurfaceArena(GameTestHelper helper) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.NETHERRACK);

                for (int y = 3; y <= 10; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                for (int y = 3; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.LAVA);
                }
            }
        }
    }

    private static void clearCombatMovementArena(GameTestHelper helper) {
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 20; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);

                for (int y = 3; y <= 10; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    private static void wildfirePatrolsWithSingleFileBlazeEscort(GameTestHelper helper) {
        BlockPos corridorCenter = helper.absolutePos(new BlockPos(15, 3, 4));
        ChunkPos corridorTicketCenter = new ChunkPos(
                corridorCenter.getX() >> 4,
                corridorCenter.getZ() >> 4
        );
        helper.getLevel().getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                corridorTicketCenter,
                2
        );
        clearFormationCorridor(helper);
        int obstacleClearanceY = helper.absolutePos(new BlockPos(10, 6, 4)).getY();
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(5.5D, 3.0D, 4.5D),
                EntitySpawnReason.NATURAL
        );
        List<net.minecraft.world.entity.monster.Blaze> blazes = helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Blaze.class,
                        wildfire.getBoundingBox().inflate(6.0D),
                        blaze -> blaze != wildfire
                );
        helper.assertFalse(
                blazes.isEmpty(),
                "The natural Wildfire must provide combat escorts"
        );
        Vec3 start = wildfire.position();
        net.minecraft.world.entity.LivingEntity[] combatTarget =
                new net.minecraft.world.entity.LivingEntity[1];
        wildfire.setPersistenceRequired();
        wildfire.setFormationDirection(new Vec3(1.0D, 0.0D, 0.0D));
        helper.getLevel().getChunkAt(wildfire.blockPosition());

        for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
            blaze.setPersistenceRequired();
            int slot = wildfire.formationSlot(blaze.getUUID());
            helper.assertTrue(slot >= 0, "Every natural Blaze escort must retain a formation slot");
            blaze.setPos(WildfireFormation.followPosition(wildfire, slot));
            helper.getLevel().getChunkAt(blaze.blockPosition());
        }

        helper.startSequence()
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            RetoldAiControl.isControlledAsBy(
                                    wildfire,
                                    RetoldAiControlMode.REGROUP,
                                    RetoldAiControlOwner.WILDFIRE_FORMATION
                            ),
                            "The Wildfire must own its low-priority patrol movement"
                                    + " (position=" + wildfire.position()
                                    + ", movement=" + wildfire.getDeltaMovement()
                                    + ", control=" + RetoldAiControl.getMode(wildfire)
                                    + "/" + RetoldAiControl.getOwner(wildfire)
                                    + ", target=" + wildfire.getTarget()
                                    + ", health=" + wildfire.getHealth()
                                    + ", shields=" + wildfire.getShieldCount()
                                    + ", recovery=" + wildfire.isLavaRecoveryActive()
                                    + ", lavaTarget="
                                    + wildfire.hasLavaRetreatTarget(helper.getLevel()) + ")"
                    );

                })
                .thenWaitUntil(() -> {
                    helper.assertTrue(
                            wildfire.distanceToSqr(start) > 0.25D * 0.25D,
                            "The Wildfire patrol leader must make forward roaming progress"
                                    + " (position=" + wildfire.position()
                                    + ", control=" + RetoldAiControl.getMode(wildfire)
                                    + "/" + RetoldAiControl.getOwner(wildfire)
                                    + ", target=" + wildfire.getTarget()
                                    + ", companions=" + blazes.size()
                                    + ", tickCount=" + wildfire.tickCount
                                    + ", moveWanted=" + wildfire.getMoveControl().hasWanted()
                                    + ")"
                    );
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        wildfire.getY() >= obstacleClearanceY,
                        "The Wildfire patrol must climb above a blocking wall"
                                + " (position=" + wildfire.position()
                                + ", movement=" + wildfire.getDeltaMovement()
                                + ", moveWanted=" + wildfire.getMoveControl().hasWanted() + ")"
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        wildfire.getX() > helper.absolutePos(new BlockPos(10, 3, 4)).getX() + 0.5D,
                        "The Wildfire patrol must cross the wall instead of getting stuck below it"
                                + " (position=" + wildfire.position()
                                + ", movement=" + wildfire.getDeltaMovement() + ")"
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        blazes.stream().anyMatch(blaze -> blaze.getY() >= obstacleClearanceY),
                        "At least one leading Blaze escort must climb the patrol obstacle"
                                + " (positions="
                                + blazes.stream().map(Entity::position).toList() + ")"
                ))
                .thenExecute(() -> {
                    Vec3 direction = wildfire.formationDirection();
                    double previousDistanceBehind = 0.0D;

                    blazes.sort((left, right) -> Integer.compare(
                            wildfire.formationSlot(left.getUUID()),
                            wildfire.formationSlot(right.getUUID())
                    ));

                    for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
                        Vec3 relative = blaze.position().subtract(wildfire.position());
                        double distanceBehind = -relative.dot(direction);
                        Vec3 lateral = relative.add(direction.scale(distanceBehind));

                        helper.assertTrue(
                                distanceBehind > previousDistanceBehind - 0.5D,
                                "Blaze escorts must remain ordered behind the Wildfire"
                        );
                        helper.assertTrue(
                                lateral.horizontalDistanceSqr() < 4.0D,
                                "Blaze escorts must remain close to the patrol's single-file line"
                        );
                        previousDistanceBehind = distanceBehind;
                    }

                    combatTarget[0] = helper.spawn(EntityTypes.ZOMBIE, 16, 3, 4);
                    wildfire.setTarget(combatTarget[0]);
                })
                .thenWaitUntil(() -> {
                    helper.assertFalse(
                            RetoldAiControl.isControlledBy(
                                    wildfire,
                                    RetoldAiControlOwner.WILDFIRE_FORMATION
                            ),
                            "Combat must release the Wildfire patrol owner"
                    );

                    for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
                        helper.assertFalse(
                                RetoldAiControl.isControlledBy(
                                        blaze,
                                        RetoldAiControlOwner.WILDFIRE_FORMATION
                                ),
                                "Combat must release every Blaze escort from formation movement"
                        );
                    }
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        RetoldAiControl.clear(wildfire);
                        blazes.forEach(blaze -> {
                            RetoldAiControl.clear(blaze);
                            blaze.discard();
                        });
                        combatTarget[0].discard();
                        wildfire.discard();
                        helper.getLevel().getChunkSource().removeTicketWithRadius(
                                TicketType.FORCED,
                                corridorTicketCenter,
                                2
                        );
                    }
                });
    }

    private static void wildfireBlazeEscortsFollowIntoCombat(GameTestHelper helper) {
        BlockPos corridorCenter = helper.absolutePos(new BlockPos(15, 3, 4));
        ChunkPos corridorTicketCenter = new ChunkPos(
                corridorCenter.getX() >> 4,
                corridorCenter.getZ() >> 4
        );
        helper.getLevel().getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                corridorTicketCenter,
                2
        );
        clearFormationCorridor(helper);
        Wildfire wildfire = helper.spawn(
                RetoldEntityTypes.WILDFIRE.get(),
                new Vec3(20.5D, 6.0D, 4.5D),
                EntitySpawnReason.NATURAL
        );
        List<net.minecraft.world.entity.monster.Blaze> blazes = helper.getLevel()
                .getEntitiesOfClass(
                        net.minecraft.world.entity.monster.Blaze.class,
                        wildfire.getBoundingBox().inflate(6.0D),
                        blaze -> blaze != wildfire
                );
        helper.assertTrue(
                blazes.size() >= 3 && blazes.size() <= 5,
                "The combat-follow fixture requires the natural Wildfire escort group"
        );
        wildfire.setPersistenceRequired();

        for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
            blaze.setPersistenceRequired();
            int slot = wildfire.formationSlot(blaze.getUUID());
            helper.assertTrue(slot >= 0, "Every combat escort must retain a formation slot");
            BlockPos escortStart = helper.absolutePos(new BlockPos(4, 3, 2 + slot));
            blaze.setPos(
                    escortStart.getX() + 0.5D,
                    escortStart.getY(),
                    escortStart.getZ() + 0.5D
            );
            helper.getLevel().getChunkAt(blaze.blockPosition());
        }

        var zombie = helper.spawn(EntityTypes.ZOMBIE, 25, 3, 4);
        zombie.setNoAi(true);
        var targetHealth = zombie.getAttribute(Attributes.MAX_HEALTH);
        helper.assertTrue(
                targetHealth != null,
                "The combat-follow target must expose maximum health"
        );
        targetHealth.setBaseValue(1000.0D);
        zombie.setHealth(1000.0F);
        helper.assertTrue(
                RetoldFactionTargetMemory.trySetTarget(
                        wildfire,
                        zombie,
                        RetoldTargetSource.FACTION_COMBAT
                ),
                "The Wildfire must begin the escort combat fixture"
        );

        helper.startSequence()
                .thenWaitUntil(() -> {
                    for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
                        helper.assertTrue(
                                RetoldAiControl.isControlledAsBy(
                                        blaze,
                                        RetoldAiControlMode.ATTACK,
                                        RetoldAiControlOwner.WILDFIRE_ESCORT_COMBAT
                                ),
                                "Every Blaze escort must own combat-follow flight"
                                        + " (slot=" + wildfire.formationSlot(blaze.getUUID())
                                        + ", control=" + RetoldAiControl.getMode(blaze)
                                        + "/" + RetoldAiControl.getOwner(blaze) + ")"
                        );
                    }
                })
                .thenWaitUntil(() -> {
                    double wallFarEdge = helper.absolutePos(
                            new BlockPos(10, 3, 4)
                    ).getX() + 1.0D;

                    for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
                        helper.assertTrue(
                                blaze.getX() > wallFarEdge,
                                "Every Blaze escort must cross the obstacle and follow the"
                                        + " Wildfire into combat (slot="
                                        + wildfire.formationSlot(blaze.getUUID())
                                        + ", position=" + blaze.position()
                                        + ", movement=" + blaze.getDeltaMovement() + ")"
                        );
                    }
                })
                .thenWaitUntil(() -> {
                    for (net.minecraft.world.entity.monster.Blaze blaze : blazes) {
                        helper.assertTrue(
                                blaze.getTarget() == zombie,
                                "Every arrived Blaze escort must inherit the Wildfire's"
                                        + " combat target (slot="
                                        + wildfire.formationSlot(blaze.getUUID())
                                        + ", target=" + blaze.getTarget() + ")"
                        );
                    }
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        RetoldAiControl.clear(wildfire);
                        blazes.forEach(blaze -> {
                            RetoldAiControl.clear(blaze);
                            blaze.discard();
                        });
                        zombie.discard();
                        wildfire.discard();
                        helper.getLevel().getChunkSource().removeTicketWithRadius(
                                TicketType.FORCED,
                                corridorTicketCenter,
                                2
                        );
                    }
                });
    }

    private static void clearFormationCorridor(GameTestHelper helper) {
        for (int x = 0; x <= 30; x++) {
            for (int z = 0; z <= 8; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.STONE);

                for (int y = 3; y <= 12; y++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }

        for (int x = 0; x <= 30; x++) {
            for (int y = 3; y <= 8; y++) {
                helper.setBlock(new BlockPos(x, y, 0), Blocks.STONE);
                helper.setBlock(new BlockPos(x, y, 8), Blocks.STONE);
            }
        }

        for (int z = 0; z <= 8; z++) {
            for (int y = 3; y <= 5; y++) {
                helper.setBlock(new BlockPos(10, y, z), Blocks.STONE);
            }
        }
    }

    private static void shieldsGateDamageAndShockwaveHostiles(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 3, 3, 3);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, 5, 3, 3);
        float wildfireHealth = wildfire.getHealth();
        float zombieHealth = zombie.getHealth();

        wildfire.setNoAi(true);
        zombie.setNoAi(true);
        wildfire.setTarget(zombie);
        wildfire.hurtServer(level, level.damageSources().generic(), Wildfire.SHIELD_DURABILITY);

        helper.assertValueEqual(
                wildfire.getShieldCount(),
                3,
                "One shield must break after its durability is exhausted"
        );
        helper.assertValueEqual(
                wildfire.getHealth(),
                wildfireHealth,
                "Shield damage must not leak into Wildfire health"
        );

        helper.runAfterDelay(2, () -> helper.assertTrue(
                zombie.getHealth() < zombieHealth,
                "A nearby hostile must take Wildfire shockwave damage"
        ));
        breakShieldAfterDelay(helper, wildfire, level, 13, 2);
        breakShieldAfterDelay(helper, wildfire, level, 26, 1);
        breakShieldAfterDelay(helper, wildfire, level, 39, 0);
        helper.runAfterDelay(52, () -> {
            try {
                wildfire.hurtServer(level, level.damageSources().generic(), 4.0F);
                helper.assertTrue(
                        wildfire.getHealth() < wildfireHealth,
                        "Damage must reach Wildfire health after all four shields break"
                );
                helper.succeed();
            } finally {
                wildfire.discard();
                zombie.discard();
            }
        });
    }

    private static void breakShieldAfterDelay(
            GameTestHelper helper,
            Wildfire wildfire,
            ServerLevel level,
            long delay,
            int expectedShields
    ) {
        helper.runAfterDelay(delay, () -> {
            wildfire.hurtServer(
                    level,
                    level.damageSources().generic(),
                    Wildfire.SHIELD_DURABILITY
            );
            helper.assertValueEqual(
                    wildfire.getShieldCount(),
                    expectedShields,
                    "Each completed shield layer must break independently"
            );
        });
    }

    private static void requiresDeepLavaSubmersionForRecovery(GameTestHelper helper) {
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 3, 3, 3);
        BlockPos recoveryPos = new BlockPos(3, 3, 3);
        BlockPos absoluteRecoveryPos = helper.absolutePos(recoveryPos);
        ChunkPos ticketCenter = new ChunkPos(
                absoluteRecoveryPos.getX() >> 4,
                absoluteRecoveryPos.getZ() >> 4
        );
        helper.getLevel().getChunkSource().addTicketWithRadius(
                TicketType.FORCED,
                ticketCenter,
                2
        );
        helper.getLevel().getChunkAt(absoluteRecoveryPos);
        wildfire.setNoAi(true);
        wildfire.setShieldCount(0);
        wildfire.setHealth(wildfire.getMaxHealth() * 0.5F);
        float woundedHealth = wildfire.getHealth();
        helper.setBlock(recoveryPos.below(), Blocks.NETHERRACK);
        helper.setBlock(recoveryPos, Blocks.FIRE);

        helper.startSequence()
                .thenIdle(Wildfire.FIRST_SHIELD_REGEN_TICKS + 2)
                .thenExecute(() -> {
                    helper.assertValueEqual(
                            wildfire.getShieldCount(),
                            0,
                            "Ordinary fire must not restore a Wildfire shield"
                    );
                    helper.assertValueEqual(
                            wildfire.getHealth(),
                            woundedHealth,
                            "Ordinary fire must not heal a wounded Wildfire"
                    );
                    helper.setBlock(recoveryPos, Blocks.LAVA);
                })
                .thenIdle(Wildfire.FIRST_SHIELD_REGEN_TICKS + 2)
                .thenExecute(() -> {
                    helper.assertFalse(
                            wildfire.isEyeInFluid(FluidTags.LAVA),
                            "The wounded test Wildfire must remain above shallow lava"
                    );
                    helper.assertFalse(
                            wildfire.isLavaRecoveryActive(),
                            "A one-block-deep lava pool must not start recovery"
                    );
                    helper.assertValueEqual(
                            wildfire.getShieldCount(),
                            0,
                            "Shallow lava must not restore a shield"
                    );
                    helper.assertValueEqual(
                            wildfire.getHealth(),
                            woundedHealth,
                            "Shallow lava must not heal a wounded Wildfire"
                    );
                })
                .thenExecute(() -> {
                    try {
                        helper.succeed();
                    } finally {
                        wildfire.discard();
                        helper.setBlock(recoveryPos, Blocks.AIR);
                        helper.getLevel().getChunkSource().removeTicketWithRadius(
                                TicketType.FORCED,
                                ticketCenter,
                                2
                        );
                    }
                });
    }

    private static void woundedWildfireRetreatsTowardLava(GameTestHelper helper) {
        Wildfire wildfire = helper.spawn(RetoldEntityTypes.WILDFIRE.get(), 3, 3, 3);
        BlockPos shallowLavaPos = new BlockPos(5, 3, 3);
        BlockPos deepLavaSurface = new BlockPos(8, 5, 3);
        wildfire.setShieldCount(2);
        wildfire.setHealth(wildfire.getMaxHealth() * 0.5F);

        for (int x = 2; x <= 9; x++) {
            helper.setBlock(new BlockPos(x, 2, 3), Blocks.NETHERRACK);
        }

        helper.setBlock(shallowLavaPos, Blocks.LAVA);
        helper.setBlock(deepLavaSurface, Blocks.LAVA);
        helper.setBlock(deepLavaSurface.below(), Blocks.LAVA);
        helper.setBlock(deepLavaSurface.below(2), Blocks.LAVA);
        helper.assertFalse(
                RetoldBlockTargetSearch.isDeepLavaSourceAt(
                        helper.getLevel(),
                        helper.absolutePos(shallowLavaPos)
                ),
                "A one-block pool must not qualify as protected recovery lava"
        );
        helper.assertTrue(
                RetoldBlockTargetSearch.isDeepLavaSourceAt(
                        helper.getLevel(),
                        helper.absolutePos(deepLavaSurface)
                ),
                "The surface of a three-block lava column must qualify for recovery"
        );
        helper.assertTrue(
                RetoldAiControl.tryClaim(
                        wildfire,
                        RetoldAiControlMode.ATTACK,
                        RetoldAiControlOwner.WILDFIRE_COMBAT,
                        RetoldAiPriorities.SPECIAL_RANGED,
                        "wildfire_reposition",
                        helper.getLevel().getGameTime(),
                        25
                ),
                "The test Wildfire must begin under combat movement ownership"
        );
        wildfire.tickLavaRetreat(helper.getLevel());

        try {
            helper.assertTrue(
                    RetoldAiControl.isControlledAsBy(
                            wildfire,
                            RetoldAiControlMode.SHELTER,
                            RetoldAiControlOwner.WILDFIRE_RECOVERY
                    ),
                    "A wounded shield-damaged Wildfire must own shelter movement toward lava"
            );
            helper.assertTrue(
                    wildfire.getMoveControl().hasWanted(),
                    "Lava retreat must set a movement target under Retold ownership"
            );
            helper.assertTrue(
                    wildfire.hasLavaRetreatTarget(helper.getLevel()),
                    "The recovery route must lead to deep lava"
            );
            helper.assertTrue(
                    wildfire.isRetreatingToLavaSource(
                            helper.absolutePos(deepLavaSurface)
                    ),
                    "The recovery route must reject closer shallow lava"
            );
            helper.succeed();
        } finally {
            RetoldAiControl.clear(wildfire);
            wildfire.discard();
            helper.setBlock(shallowLavaPos, Blocks.AIR);
            helper.setBlock(deepLavaSurface, Blocks.AIR);
            helper.setBlock(deepLavaSurface.below(), Blocks.AIR);
            helper.setBlock(deepLavaSurface.below(2), Blocks.AIR);
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
