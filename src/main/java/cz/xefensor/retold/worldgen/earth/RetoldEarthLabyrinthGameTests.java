package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.api.world.RetoldWorldMutationType;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldEarthLabyrinthGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldEarthLabyrinthGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);
        TestData<Holder<TestEnvironmentDefinition<?>>> environmentalTickTestData =
                new TestData<>(environment, EMPTY_STRUCTURE, 100, 0, true);
        TestData<Holder<TestEnvironmentDefinition<?>>> patrolTickTestData =
                new TestData<>(environment, EMPTY_STRUCTURE, 240, 0, true);
        RetoldEarthRoomGameTests.register(event, environmentalTickTestData);
        RetoldEarthTraversalGameTests.register(event, environment);
        RetoldEarthCombatGameTests.register(event, environment);
        RetoldEarthTerrainGameTests.register(event, environment);
        RetoldEarthMazePursuitGameTests.register(event, environment);

        register(
                event,
                testData,
                "jungle_pyramids_use_earth_labyrinth_structure",
                RetoldEarthLabyrinthGameTests::junglePyramidsUseEarthLabyrinthStructure
        );
        register(
                event,
                testData,
                "earth_labyrinth_pieces_round_trip_without_upgrading_old_pyramids",
                RetoldEarthLabyrinthGameTests::earthLabyrinthPiecesRoundTrip
        );
        register(
                event,
                testData,
                "earth_labyrinth_staircase_leaves_puzzle_room_safely",
                RetoldEarthLabyrinthGameTests::earthLabyrinthStaircaseLeavesPuzzleRoom
        );
        register(
                event,
                testData,
                "earth_labyrinth_piece_places_connected_maze_geometry",
                RetoldEarthLabyrinthGameTests::earthLabyrinthPiecePlacesGeometry
        );
        register(
                event,
                testData,
                "earth_guardian_foundation_spawns_and_persists_lifecycle",
                RetoldEarthLabyrinthGameTests::earthGuardianFoundationSpawnsAndPersistsLifecycle
        );
        register(
                event,
                testData,
                "earth_guardian_tracks_weighted_vibrations_without_sight",
                RetoldEarthLabyrinthGameTests::earthGuardianTracksWeightedVibrationsWithoutSight
        );
        register(
                event,
                testData,
                "earth_guardian_uses_environmental_attacks",
                RetoldEarthLabyrinthGameTests::earthGuardianUsesEnvironmentalAttacks
        );
        register(
                event,
                environmentalTickTestData,
                "earth_guardian_ceiling_debris_hits_on_server_ticks",
                RetoldEarthLabyrinthGameTests::earthGuardianCeilingDebrisHitsOnServerTicks
        );
        register(
                event,
                environmentalTickTestData,
                "earth_guardian_lava_ground_burns_on_server_ticks",
                RetoldEarthLabyrinthGameTests::earthGuardianLavaGroundBurnsOnServerTicks
        );
        register(
                event,
                environmentalTickTestData,
                "earth_guardian_floor_collapse_causes_real_fall",
                RetoldEarthLabyrinthGameTests::earthGuardianFloorCollapseCausesRealFall
        );
        register(
                event,
                environmentalTickTestData,
                "earth_guardian_physically_investigates_vibrations",
                RetoldEarthLabyrinthGameTests::earthGuardianPhysicallyInvestigatesVibrations
        );
        register(
                event,
                patrolTickTestData,
                "earth_guardian_physically_patrols_maze",
                RetoldEarthLabyrinthGameTests::earthGuardianPhysicallyPatrolsMaze
        );
        register(
                event,
                testData,
                "earth_guardian_relocates_any_loaded_block",
                RetoldEarthLabyrinthGameTests::earthGuardianRelocatesAnyLoadedBlock
        );
    }

    private static void junglePyramidsUseEarthLabyrinthStructure(GameTestHelper helper) {
        Registry<Structure> structures = helper.getLevel()
                .registryAccess()
                .lookupOrThrow(Registries.STRUCTURE);
        Structure junglePyramid = structures.getValueOrThrow(BuiltinStructures.JUNGLE_TEMPLE);

        helper.assertTrue(
                junglePyramid instanceof EarthJungleTempleStructure,
                "New Jungle Pyramid starts must use the Retold composite structure"
        );
        helper.assertTrue(
                junglePyramid.biomes().isBound() && junglePyramid.biomes().size() > 0,
                "The replacement must retain the vanilla Jungle Pyramid biome boundary"
        );
        helper.succeed();
    }

    private static void earthLabyrinthPiecesRoundTrip(GameTestHelper helper) {
        StructurePieceSerializationContext context =
                StructurePieceSerializationContext.fromLevel(helper.getLevel());
        EarthJungleTemplePiece temple = new EarthJungleTemplePiece(
                RandomSource.create(19L),
                0,
                0
        );
        temple.moveToGround(80);
        EarthLabyrinthPiece labyrinth = new EarthLabyrinthPiece(
                temple.getBoundingBox().getCenter().getX(),
                temple.getBoundingBox().getCenter().getZ(),
                80,
                temple.labyrinthConnectionPosition(),
                temple.labyrinthEgressDirection(),
                0x4E415A45L
        );
        PiecesContainer loadedNewPieces = roundTrip(
                context,
                new PiecesContainer(List.of(temple, labyrinth))
        );

        helper.assertValueEqual(loadedNewPieces.pieces().size(), 2, "New composite piece count");
        helper.assertTrue(
                loadedNewPieces.pieces().get(0) instanceof EarthJungleTemplePiece,
                "The grounded Retold temple wrapper must survive structure serialization"
        );
        helper.assertTrue(
                loadedNewPieces.pieces().get(1) instanceof EarthLabyrinthPiece loadedLabyrinth
                        && loadedLabyrinth.layoutSeed() == labyrinth.layoutSeed(),
                "The labyrinth topology seed must survive structure serialization"
        );

        JungleTemplePiece oldTemple = new JungleTemplePiece(RandomSource.create(23L), 32, 32);
        CompoundTag savedLabyrinth = labyrinth.createTag(context);
        helper.assertValueEqual(savedLabyrinth.getIntOr("ContentVersion", -1), 1,
                "New starts must persist room-content version");
        savedLabyrinth.remove("ContentVersion");
        CompoundTag legacyLabyrinth = new EarthLabyrinthPiece(savedLabyrinth).createTag(context);
        helper.assertValueEqual(legacyLabyrinth.getIntOr("ContentVersion", -1), 0,
                "Older partially generated starts must retain empty-room content");
        PiecesContainer loadedOldPieces = roundTrip(
                context,
                new PiecesContainer(List.of(oldTemple))
        );
        StructurePiece loadedOldPiece = loadedOldPieces.pieces().getFirst();

        helper.assertTrue(
                loadedOldPiece.getClass() == JungleTemplePiece.class,
                "A saved vanilla pyramid piece must remain vanilla and gain no labyrinth piece"
        );
        helper.succeed();
    }

    private static void earthLabyrinthStaircaseLeavesPuzzleRoom(GameTestHelper helper) {
        BlockPos connection = new BlockPos(0, 80, 0);
        BlockPos entrance = new BlockPos(0, 20, -20);
        BlockPos staircaseStart = EarthLabyrinthGenerator.entranceStaircaseStart(connection);
        helper.assertValueEqual(
                staircaseStart,
                connection.below(EarthLabyrinthDimensions.ENTRANCE_SHAFT_DEPTH),
                "Staircase must start beneath the puzzle-clearance ladder shaft"
        );
        for (Direction egress : List.of(
                Direction.NORTH,
                Direction.EAST,
                Direction.SOUTH,
                Direction.WEST
        )) {
            List<BlockPos> path = EarthLabyrinthGenerator.entrancePath(
                    staircaseStart,
                    egress,
                    entrance
            );

            helper.assertValueEqual(path.getFirst(), staircaseStart, "Staircase start");
            helper.assertValueEqual(
                    path.get(1),
                    staircaseStart.relative(egress).below(),
                    "First safe egress step"
            );
            helper.assertValueEqual(
                    path.get(2),
                    staircaseStart.relative(egress, 2).below(2),
                    "Second safe egress step"
            );
            helper.assertValueEqual(
                    path.get(3),
                    staircaseStart.relative(egress, 3).below(3),
                    "Third safe egress step"
            );
            helper.assertValueEqual(path.getLast(), entrance, "Maze entrance destination");
            helper.assertValueEqual(path.size(), 53, "Deep switchback path length");
            helper.assertTrue(
                    path.stream().allMatch(node -> node.getY() + 4 < connection.getY()),
                    "Staircase shell must remain below every vanilla puzzle component"
            );

            for (int index = 1; index < path.size(); index++) {
                BlockPos previous = path.get(index - 1);
                BlockPos current = path.get(index);
                int horizontalDistance = Math.abs(current.getX() - previous.getX())
                        + Math.abs(current.getZ() - previous.getZ());
                int verticalDrop = previous.getY() - current.getY();

                helper.assertValueEqual(horizontalDistance, 1, "Horizontal staircase step");
                helper.assertTrue(
                        verticalDrop == 0 || verticalDrop == 1,
                        "The staircase must descend by at most one block per horizontal step"
                );
            }

            helper.assertValueEqual(
                    path.stream().distinct().count(),
                    (long) path.size(),
                    "The deep staircase must not double back through an existing step"
            );
        }

        helper.succeed();
    }

    private static void earthLabyrinthPiecePlacesGeometry(GameTestHelper helper) {
        BlockPos testCenter = helper.absolutePos(new BlockPos(96, 96, 96));
        int pyramidBaseY = testCenter.getY();
        long layoutSeed = 0x4C41425952494E54L;
        EarthLabyrinthLayout layout = EarthLabyrinthPlanner.generate(layoutSeed);
        int width = layout.footprintWidthBlocks();
        int minX = testCenter.getX() - width / 2;
        int minZ = testCenter.getZ() - width / 2;
        long offsetJunctions = layout.cells()
                .stream()
                .filter(cell -> {
                    BlockPos actual = cellCenter(
                            layout,
                            cell,
                            testCenter,
                            pyramidBaseY,
                            layoutSeed
                    );
                    int regularX = minX + 2
                            + cell.x() * EarthLabyrinthLayout.CELL_PITCH_BLOCKS;
                    int regularZ = minZ + 2
                            + cell.z() * EarthLabyrinthLayout.CELL_PITCH_BLOCKS;
                    return actual.getX() != regularX || actual.getZ() != regularZ;
                })
                .count();
        long bentTunnels = layout.passages()
                .stream()
                .filter(passage -> !passage.isVertical())
                .map(passage -> EarthLabyrinthGenerator.tunnelPath(
                        layout,
                        passage,
                        testCenter.getX(),
                        testCenter.getZ(),
                        pyramidBaseY,
                        layoutSeed
                ))
                .filter(RetoldEarthLabyrinthGameTests::hasTunnelTurn)
                .count();
        helper.assertTrue(offsetJunctions > 0L, "Cave junctions must break the logical grid");
        helper.assertTrue(bentTunnels > 0L, "Cave tunnels must include natural-looking bends");
        BlockPos connection = new BlockPos(
                testCenter.getX(),
                pyramidBaseY + EarthLabyrinthDimensions.CONNECTION_LOCAL_Y,
                testCenter.getZ()
        );
        BoundingBox fullBounds = new BoundingBox(
                minX,
                EarthLabyrinthDimensions.floorY(pyramidBaseY, 1),
                minZ,
                minX + width - 1,
                connection.getY() + 3,
                minZ + width - 1
        );

        EarthLabyrinthGenerator.generate(
                helper.getLevel(),
                fullBounds,
                testCenter.getX(),
                testCenter.getZ(),
                pyramidBaseY,
                connection,
                Direction.EAST,
                layoutSeed
        );

        BlockPos entranceCenter = cellCenter(
                layout,
                layout.entrance(),
                testCenter,
                pyramidBaseY,
                layoutSeed
        );
        for (var room : EarthLabyrinthRooms.plan(layout, layoutSeed)) {
            BlockPos floor = cellCenter(layout, room.cell(), testCenter, pyramidBaseY, layoutSeed);
            int distance = room.kind() == EarthLabyrinthRooms.Kind.TREASURE ? 1 : 2;
            BlockPos container = floor.offset(room.backX() * distance, 1, room.backZ() * distance);
            helper.assertTrue(helper.getLevel().getBlockEntity(container)
                    instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity,
                    "Every selected room must contain its initialized loot container");
            helper.assertTrue(helper.getLevel().getBlockState(floor.above(2)).isAir(),
                    "Side-room contents must preserve the central walking route");
        }
        helper.assertTrue(
                isMazeStone(helper, entranceCenter)
                        || helper.getLevel().getBlockState(entranceCenter)
                        .is(Blocks.MOSSY_COBBLESTONE_STAIRS),
                "The entrance chamber must retain a walkable maze-owned floor"
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(entranceCenter.above(2)).is(Blocks.CAVE_AIR),
                "Every chamber must retain a three-block-high interior"
        );

        BlockPos guardianCenter = cellCenter(
                layout,
                layout.guardianChamber(),
                testCenter,
                pyramidBaseY,
                layoutSeed
        );
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(
                            guardianCenter.relative(direction, 15).above(5)
                    ).is(Blocks.CAVE_AIR),
                    "The central guardian arena must remain at least 31 blocks wide"
            );
        }
        helper.assertTrue(
                helper.getLevel().getBlockState(guardianCenter.above(10)).is(Blocks.CAVE_AIR),
                "The central guardian arena must provide ten blocks of headroom"
        );
        helper.assertTrue(
                isMazeStone(helper, guardianCenter.above(11)),
                "The huge guardian arena must retain a maze-owned ceiling"
        );

        EarthLabyrinthLayout.Passage horizontal = layout.passages()
                .stream()
                .filter(passage -> !passage.isVertical())
                .findFirst()
                .orElseThrow();
        List<BlockPos> tunnelPath = EarthLabyrinthGenerator.tunnelPath(
                layout,
                horizontal,
                testCenter.getX(),
                testCenter.getZ(),
                pyramidBaseY,
                layoutSeed
        );
        int tunnelIndex = tunnelPath.size() / 2;
        BlockPos tunnelCenter = tunnelPath.get(tunnelIndex);
        BlockPos doorway = tunnelCenter.above(2);
        helper.assertTrue(
                helper.getLevel().getBlockState(doorway).is(Blocks.CAVE_AIR),
                "Planned horizontal passages must carve a continuous cave tunnel"
        );
        boolean hasNaturalTunnelShell = layout.passages()
                .stream()
                .filter(passage -> !passage.isVertical())
                .flatMap(passage -> EarthLabyrinthGenerator.tunnelPath(
                        layout,
                        passage,
                        testCenter.getX(),
                        testCenter.getZ(),
                        pyramidBaseY,
                        layoutSeed
                ).stream())
                .anyMatch(center -> helper.getLevel().getBlockState(center.above(2))
                        .is(Blocks.CAVE_AIR)
                        && isMazeStone(helper, center.above(4)));
        helper.assertTrue(
                hasNaturalTunnelShell,
                "Carved tunnels must retain natural maze-owned stone ceilings"
        );

        long ladderCount = layout.passages()
                .stream()
                .filter(EarthLabyrinthLayout.Passage::isVertical)
                .filter(passage -> hasGeneratedLadder(
                        helper,
                        passage,
                        layout,
                        testCenter,
                        pyramidBaseY,
                        layoutSeed
                ))
                .count();
        helper.assertTrue(
                ladderCount >= 1L,
                "At least one planned vertical connection must remain physically climbable"
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(connection.above()).isAir(),
                "The hidden puzzle room floor opening must lead into the staircase"
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(connection).is(Blocks.LADDER),
                "The puzzle-room opening must enter a narrow ladder shaft"
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(
                        connection.below(EarthLabyrinthDimensions.ENTRANCE_SHAFT_DEPTH)
                ).is(Blocks.LADDER),
                "The ladder shaft must reach the safely lowered staircase start"
        );
        helper.assertTrue(
                isMazeStone(helper, connection.west()),
                "The entrance ladder must retain a supporting wall away from its east exit"
        );
        helper.succeed();
    }

    private static void earthGuardianFoundationSpawnsAndPersistsLifecycle(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        RetoldWorldData worldData = RetoldWorldData.get(level);
        RetoldWorldStage originalStage = worldData.getStage();
        BlockPos guardianPosition = helper.absolutePos(new BlockPos(4, 3, 4));
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                guardianPosition,
                new BoundingBox(
                        guardianPosition.getX() - 24,
                        guardianPosition.getY() - 8,
                        guardianPosition.getZ() - 24,
                        guardianPosition.getX() + 24,
                        guardianPosition.getY() + 72,
                        guardianPosition.getZ() + 24
                )
        );
        EarthGuardianEncounterData encounterData = EarthGuardianEncounterData.get(level);
        ServerPlayer player = null;
        EarthGuardian restored = null;

        try {
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
            worldData.setStage(RetoldWorldStage.STAGE_1);
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "A loaded labyrinth must create its dormant guardian"
            );
            EarthGuardian guardian = findStructureGuardians(level, source).getFirst();
            helper.assertTrue(guardian.isPersistenceRequired(), "The guardian must persist");
            helper.assertValueEqual(
                    guardian.lifecycle(),
                    EarthGuardianLifecycle.DORMANT,
                    "Initial guardian lifecycle"
            );
            helper.assertTrue(guardian.isNoAi(), "The Stage 1 statue must have no AI");
            helper.assertTrue(guardian.isInvulnerable(), "The Stage 1 statue must be invulnerable");
            helper.assertFalse(
                    guardian.isBossBarVisible(),
                    "The dormant statue must not expose a boss bar"
            );
            helper.assertFalse(
                    guardian.canUsePortal(false),
                    "A labyrinth guardian must not leave through a portal"
            );
            helper.assertFalse(
                    guardian.hurtServer(level, level.damageSources().generic(), 20.0F),
                    "The dormant statue must not take damage before awakening"
            );
            guardian.setPos(guardian.position().add(4.0D, 1.0D, 0.0D));
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "A repeated source scan must repair rather than duplicate the guardian"
            );
            helper.assertValueEqual(
                    guardian.position(),
                    net.minecraft.world.phys.Vec3.atBottomCenterOf(guardianPosition),
                    "A displaced dormant statue must still return to its chamber anchor"
            );
            helper.assertValueEqual(
                    findStructureGuardians(level, source).size(),
                    1,
                    "Guardian count after a repeated source scan"
            );
            helper.assertTrue(
                    SpawnEggItem.spawnsEntity(
                            new ItemStack(RetoldBlocks.EARTH_GUARDIAN_SPAWN_EGG.get()),
                            RetoldEntityTypes.EARTH_GUARDIAN.get()
                    ),
                    "The Earth Guardian spawn egg must map to the guardian entity"
            );

            player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            level.players().add(player);
            player.snapTo(
                    guardianPosition.getX() + 0.5D,
                    guardianPosition.getY(),
                    guardianPosition.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            guardian.tickGuardianState(level);
            helper.assertValueEqual(
                    guardian.lifecycle(),
                    EarthGuardianLifecycle.DORMANT,
                    "A chamber visit before Stage 2 must not wake the statue"
            );

            worldData.setStage(RetoldWorldStage.STAGE_2);
            guardian.tickGuardianState(level);
            helper.assertValueEqual(
                    guardian.lifecycle(),
                    EarthGuardianLifecycle.AWAKENING,
                    "A Stage 2 chamber visit must begin awakening"
            );
            helper.assertTrue(
                    guardian.isBossBarVisible(),
                    "The awakening guardian must reveal its boss bar"
            );
            helper.assertValueEqual(
                    encounterData.lifecycle(source.key()),
                    EarthGuardianLifecycle.AWAKENING,
                    "Awakening must update authoritative encounter state"
            );

            for (int tick = 0; tick < EarthGuardian.AWAKENING_DURATION_TICKS; tick++) {
                if (tick % 20 == 0) {
                    EarthGuardianSpawner.spawnIfNeeded(level, source);
                }
                guardian.tickGuardianState(level);
            }

            helper.assertValueEqual(
                    guardian.lifecycle(),
                    EarthGuardianLifecycle.ROAMING,
                    "The awakening timer must reach the future roaming state"
            );
            helper.assertTrue(
                    !guardian.isNoAi() && !guardian.isInvulnerable(),
                    "Roaming must enable investigation and make the awakened boss damageable"
            );
            helper.assertValueEqual(
                    encounterData.lifecycle(source.key()),
                    EarthGuardianLifecycle.ROAMING,
                    "Roaming must persist in authoritative encounter state"
            );

            net.minecraft.world.phys.Vec3 roamingPosition = guardian.position().add(10.0D, 2.0D, 6.0D);
            net.minecraft.world.phys.Vec3 roamingVelocity = new net.minecraft.world.phys.Vec3(0.1D, -0.2D, 0.15D);
            guardian.setPos(roamingPosition);
            guardian.setDeltaMovement(roamingVelocity);
            guardian.setYRot(75.0F);
            guardian.setHealth(200.0F);
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    guardianPosition,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    player,
                    null,
                    12.0F
            );
            CompoundTag beforeRepair = saveAdditionalState(level, guardian);

            for (int scan = 0; scan < 4; scan++) {
                helper.assertTrue(EarthGuardianSpawner.spawnIfNeeded(level, source), "Roaming source repair");
                helper.assertValueEqual(guardian.position(), roamingPosition, "Repair must not teleport a roaming guardian");
                helper.assertValueEqual(guardian.getDeltaMovement(), roamingVelocity, "Repair must retain roaming velocity");
                helper.assertValueEqual(guardian.getYRot(), 75.0F, "Repair must retain facing");
                helper.assertValueEqual(saveAdditionalState(level, guardian), beforeRepair, "Repair must retain entity state and clue");
                helper.assertValueEqual(findStructureGuardians(level, source).getFirst(), guardian, "Repair must retain the same guardian");
                guardian.tickGuardianState(level);
                helper.assertValueEqual(guardian.position(), roamingPosition, "Roaming lifecycle ticks must not restore the statue anchor");
            }

            CompoundTag saved = saveAdditionalState(level, guardian);
            restored = RetoldEntityTypes.EARTH_GUARDIAN.get().create(
                    level,
                    EntitySpawnReason.LOAD
            );
            helper.assertTrue(restored != null, "A restored guardian entity must be creatable");
            restored.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    saved
            ));
            helper.assertValueEqual(
                    restored.lifecycle(),
                    EarthGuardianLifecycle.ROAMING,
                    "Entity serialization must preserve the lifecycle"
            );
            helper.assertTrue(
                    restored.isStructureBound()
                            && restored.labyrinthKey() == source.key(),
                    "Entity serialization must preserve labyrinth ownership"
            );
            restored.setPos(roamingPosition);
            restored.configureForLabyrinth(level, source, EarthGuardianLifecycle.ROAMING);
            helper.assertValueEqual(restored.position(), roamingPosition, "Rebinding a loaded roaming guardian must retain its position");

            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DEFEATED);
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "A defeated encounter must be handled without respawning"
            );
            helper.assertTrue(
                    findStructureGuardians(level, source).isEmpty(),
                    "A defeated labyrinth must remain permanently cleared"
            );
            helper.succeed();
        } finally {
            if (player != null) {
                level.players().remove(player);
                player.discard();
            }

            if (restored != null) {
                restored.discard();
            }

            findStructureGuardians(level, source).forEach(Entity::discard);
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
            worldData.setStage(originalStage);
        }
    }

    private static void earthGuardianTracksWeightedVibrationsWithoutSight(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos guardianPosition = helper.absolutePos(new BlockPos(4, 3, 4));
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                guardianPosition,
                new BoundingBox(
                        guardianPosition.getX() - 32,
                        guardianPosition.getY() - 8,
                        guardianPosition.getZ() - 32,
                        guardianPosition.getX() + 32,
                        guardianPosition.getY() + 72,
                        guardianPosition.getZ() + 32
                )
        );
        EarthGuardianEncounterData encounterData = EarthGuardianEncounterData.get(level);
        ServerPlayer walker = null;
        ServerPlayer explosivePlayer = null;
        EarthGuardian restored = null;

        try {
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.ROAMING);
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "The vibration fixture must create its roaming guardian"
            );
            EarthGuardian guardian = findStructureGuardians(level, source).getFirst();
            helper.assertFalse(guardian.isNoAi(), "A roaming guardian must run investigation AI");
            helper.assertFalse(
                    guardian.isInvulnerable(),
                    "The awakened guardian must be damageable during environmental combat"
            );
            helper.assertTrue(guardian.getTarget() == null, "The blind guardian must start untargeted");

            walker = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            explosivePlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            BlockPos walkingClue = guardianPosition.offset(12, 0, 0);
            BlockPos explosionClue = guardianPosition.offset(0, 0, 10);
            walker.snapTo(
                    walkingClue.getX() + 0.5D,
                    walkingClue.getY(),
                    walkingClue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            explosivePlayer.snapTo(
                    explosionClue.getX() + 0.5D,
                    explosionClue.getY(),
                    explosionClue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );

            helper.assertTrue(
                    guardian.getVibrationUser().canReceiveVibration(
                            level,
                            walkingClue,
                            net.minecraft.world.level.gameevent.GameEvent.STEP,
                            net.minecraft.world.level.gameevent.GameEvent.Context.of(walker)
                    ),
                    "A Survival player's step inside the labyrinth must be a valid clue"
            );
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    walkingClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    walker,
                    null,
                    12.0F
            );
            helper.assertValueEqual(
                    guardian.vibrationTarget(),
                    walkingClue,
                    "The listener callback must remember a valid walking clue"
            );

            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    explosionClue,
                    net.minecraft.world.level.gameevent.GameEvent.EXPLODE,
                    explosivePlayer,
                    null,
                    10.0F
            );
            int explosionStrength = guardian.vibrationStrength();
            helper.assertValueEqual(
                    guardian.vibrationTarget(),
                    explosionClue,
                    "A stronger explosion must replace a quieter player's clue"
            );

            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    walkingClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    walker,
                    null,
                    1.0F
            );
            helper.assertValueEqual(
                    guardian.vibrationTarget(),
                    explosionClue,
                    "A weaker clue from another player must not replace the active source"
            );
            helper.assertValueEqual(
                    guardian.vibrationStrength(),
                    explosionStrength,
                    "Rejected vibration strength"
            );
            helper.assertTrue(
                    guardian.getTarget() == null,
                    "Remembering a vibration must not grant a sight-like combat target"
            );

            float healthBeforeDamage = guardian.getHealth();
            helper.assertTrue(
                    guardian.hurtServer(level, level.damageSources().playerAttack(walker), 1.0F),
                    "An awakened guardian must accept player damage"
            );
            helper.assertTrue(
                    guardian.getHealth() < healthBeforeDamage,
                    "Accepted player damage must reduce awakened guardian health"
            );
            helper.assertValueEqual(
                    guardian.vibrationTarget(),
                    walker.blockPosition(),
                    "Direct damage must provide the strongest location clue"
            );

            for (int tick = 0; tick < 5; tick++) {
                guardian.tick();
            }
            helper.assertTrue(
                    RetoldAiControl.isControlledBy(
                            guardian,
                            RetoldAiControlOwner.EARTH_GUARDIAN
                    ),
                    "Vibration investigation must own guardian movement"
            );

            CompoundTag saved = saveAdditionalState(level, guardian);
            restored = RetoldEntityTypes.EARTH_GUARDIAN.get().create(
                    level,
                    EntitySpawnReason.LOAD
            );
            helper.assertTrue(restored != null, "A vibration-aware guardian must restore");
            restored.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    saved
            ));
            helper.assertValueEqual(
                    restored.vibrationTarget(),
                    guardian.vibrationTarget(),
                    "Remembered vibration position after save/load"
            );
            helper.assertValueEqual(
                    restored.vibrationStrength(),
                    guardian.vibrationStrength(),
                    "Remembered vibration strength after save/load"
            );
            helper.succeed();
        } finally {
            if (walker != null) {
                level.players().remove(walker);
                walker.discard();
            }
            if (explosivePlayer != null) {
                level.players().remove(explosivePlayer);
                explosivePlayer.discard();
            }
            if (restored != null) {
                restored.discard();
            }
            findStructureGuardians(level, source).forEach(Entity::discard);
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
        }
    }

    private static void earthGuardianUsesEnvironmentalAttacks(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos guardianPosition = helper.absolutePos(new BlockPos(8, 3, 8));
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                guardianPosition,
                new BoundingBox(
                        guardianPosition.getX() - 24,
                        guardianPosition.getY() - 8,
                        guardianPosition.getZ() - 24,
                        guardianPosition.getX() + 24,
                        guardianPosition.getY() + 16,
                        guardianPosition.getZ() + 24
                )
        );
        EarthGuardianEncounterData encounterData = EarthGuardianEncounterData.get(level);
        ServerPlayer player = null;
        Zombie victim = null;
        EarthGuardian restored = null;
        RetoldWorldProtection.Registration protection = null;

        try {
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.ROAMING);
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "The combat fixture must create its roaming guardian"
            );
            EarthGuardian guardian = findStructureGuardians(level, source).getFirst();
            player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 20, 3, 8);

            BlockPos hazardClue = guardianPosition.east(12);
            BlockPos ceiling = hazardClue.above(6);
            level.setBlockAndUpdate(hazardClue.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(hazardClue, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ceiling.east(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ceiling.west(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ceiling.north(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ceiling.south(), Blocks.STONE.defaultBlockState());
            BlockPos protectedCeiling = ceiling.east();
            protection = RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath(
                            "retold",
                            "earth_guardian_environmental_attack_test"
                    ),
                    context -> context.type() != RetoldWorldMutationType.MOB_BREAK
                            || !context.pos().equals(protectedCeiling)
            );
            player.snapTo(
                    hazardClue.getX() + 0.5D,
                    hazardClue.getY(),
                    hazardClue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            victim.snapTo(
                    hazardClue.getX() + 0.5D,
                    hazardClue.getY(),
                    hazardClue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            float healthBeforeDirectAttempt = victim.getHealth();
            helper.assertFalse(
                    guardian.doHurtTarget(level, victim),
                    "Contact melee must not target unrelated mobs at a distant hazard location"
            );
            helper.assertValueEqual(
                    victim.getHealth(),
                    healthBeforeDirectAttempt,
                    "Rejecting direct attacks must leave the victim unharmed"
            );
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    hazardClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    player,
                    null,
                    12.0F
            );
            float healthBeforeCollapse = victim.getHealth();
            guardian.tickEnvironmentalCombat(level);
            helper.assertTrue(
                    guardian.isPreparingEnvironmentalAttack(),
                    "A remembered vibration beneath a ceiling must start an environmental attack"
            );
            helper.assertValueEqual(
                    victim.getHealth(),
                    healthBeforeCollapse,
                    "Starting a ceiling warning must not directly damage its victim"
            );
            helper.assertTrue(
                    guardian.getTarget() == null,
                    "Environmental attacks must not grant the blind guardian a sight target"
            );
            CompoundTag saved = saveAdditionalState(level, guardian);
            restored = RetoldEntityTypes.EARTH_GUARDIAN.get().create(
                    level,
                    EntitySpawnReason.LOAD
            );
            helper.assertTrue(restored != null, "A winding-up guardian must restore");
            restored.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    saved
            ));
            helper.assertTrue(
                    restored.isPreparingEnvironmentalAttack(),
                    "The pending environmental warning must survive save/load"
            );

            for (int tick = 0; tick < 29; tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertValueEqual(
                    level.getBlockState(ceiling).getBlock(),
                    Blocks.STONE,
                    "The ceiling must remain intact throughout the warning"
            );
            guardian.tickEnvironmentalCombat(level);
            helper.assertTrue(
                    level.getBlockState(ceiling).isAir(),
                    "The released collapse must remove a real ceiling block"
            );
            helper.assertTrue(
                    level.getBlockState(protectedCeiling).is(Blocks.STONE),
                    "Administrative world protection must be able to preserve ceiling blocks"
            );
            protection.close();
            protection = null;
            helper.assertFalse(
                    guardian.isPreparingEnvironmentalAttack(),
                    "The collapse must clear its pending attack after release"
            );
            helper.assertTrue(
                    !level.getEntitiesOfClass(
                            FallingBlockEntity.class,
                            new net.minecraft.world.phys.AABB(ceiling).inflate(2.0D)
                    ).isEmpty(),
                    "The ceiling block must become physical falling debris"
            );

            victim.setHealth(victim.getMaxHealth());
            victim.invulnerableTime = 0;
            victim.setRemainingFireTicks(0);
            victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            victim.snapTo(
                    hazardClue.getX() + 0.5D,
                    hazardClue.getY(),
                    hazardClue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            BlockPos lavaGround = hazardClue.below();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlockAndUpdate(lavaGround.offset(x, 0, z), Blocks.STONE.defaultBlockState());
                }
            }
            level.setBlockAndUpdate(lavaGround.north(), Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(lavaGround.south(), Blocks.CHEST.defaultBlockState());
            protection = RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath("retold", "earth_guardian_lava_test"),
                    context -> !(context.type() == RetoldWorldMutationType.MOB_BREAK
                            && context.pos().equals(lavaGround.east()))
                            && !(context.type() == RetoldWorldMutationType.MOB_PLACE
                            && context.pos().equals(lavaGround.west()))
            );
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    hazardClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    player,
                    null,
                    12.0F
            );
            for (int tick = 0; tick < 80 && !guardian.isPreparingEnvironmentalAttack(); tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertTrue(
                    guardian.isPreparingEnvironmentalAttack(),
                    "The alternating attack must begin a lava eruption after cooldown"
            );
            float healthBeforeEruption = victim.getHealth();
            for (int tick = 0; tick < 29; tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertValueEqual(
                    victim.getHealth(),
                    healthBeforeEruption,
                    "The lava warning must leave time to leave the marked ground"
            );
            guardian.tickEnvironmentalCombat(level);
            helper.assertValueEqual(
                    victim.getHealth(),
                    healthBeforeEruption,
                    "Releasing lava must not apply scripted damage before fluid contact"
            );
            helper.assertTrue(
                    victim.getRemainingFireTicks() == 0 && victim.getDeltaMovement().y == 0.0D,
                    "Ground conversion must not directly ignite or launch its victim"
            );
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos ground = lavaGround.offset(x, 0, z);
                    boolean protectedGround = ground.equals(lavaGround.east())
                            || ground.equals(lavaGround.west());
                    helper.assertTrue(
                            level.getBlockState(ground).is(protectedGround ? Blocks.STONE : Blocks.LAVA),
                            "Only protection-approved ground in the 3x3 patch must become lava"
                    );
                }
            }
            helper.assertTrue(
                    level.getFluidState(lavaGround).isSource()
                            && level.getBlockState(hazardClue).isAir()
                            && level.getBlockEntity(lavaGround.south()) == null,
                    "The attack must create real sources below the victim, including container replacement"
            );
            protection.close();
            protection = null;
            helper.assertTrue(
                    guardian.getTarget() == null,
                    "The eruption must still use vibration memory without assigning a target"
            );

            level.setBlockAndUpdate(protectedCeiling, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(lavaGround, Blocks.STONE.defaultBlockState());
            victim.setHealth(victim.getMaxHealth());
            victim.invulnerableTime = 0;
            victim.setRemainingFireTicks(0);
            victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    hazardClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    player,
                    null,
                    12.0F
            );
            for (int tick = 0; tick < 80 && !guardian.isPreparingEnvironmentalAttack(); tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertTrue(
                    guardian.isPreparingEnvironmentalAttack(),
                    "The attack after lava must begin a floor-collapse warning"
            );
            for (int tick = 0; tick < 30; tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertTrue(
                    level.getBlockState(lavaGround).isAir(),
                    "The third attack must open the floor instead of producing more lava"
            );

            victim.setHealth(victim.getMaxHealth());
            victim.invulnerableTime = 0;
            victim.setRemainingFireTicks(0);
            victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            level.setBlockAndUpdate(lavaGround, Blocks.STONE.defaultBlockState());
            BlockPos obstructedCeiling = hazardClue.above(6);
            BlockPos obstruction = hazardClue.above();
            level.setBlockAndUpdate(obstructedCeiling, Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(obstruction, Blocks.STONE.defaultBlockState());
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    hazardClue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    player,
                    null,
                    12.0F
            );
            for (int tick = 0; tick < 80 && !guardian.isPreparingEnvironmentalAttack(); tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertTrue(
                    guardian.isPreparingEnvironmentalAttack(),
                    "A blocked ceiling must still begin a fallback environmental warning"
            );
            for (int tick = 0; tick < 30; tick++) {
                guardian.tickEnvironmentalCombat(level);
            }
            helper.assertTrue(
                    level.getBlockState(obstructedCeiling).is(Blocks.STONE),
                    "Ceiling debris must not be selected when its fall path is blocked"
            );
            helper.assertTrue(
                    level.getBlockState(lavaGround).is(Blocks.LAVA),
                    "A blocked ceiling collapse must fall back to ground conversion"
            );
            boolean originalMobGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
            try {
                level.setBlockAndUpdate(lavaGround, Blocks.STONE.defaultBlockState());
                for (int tick = 0; tick < 80 && !guardian.isPreparingEnvironmentalAttack(); tick++) {
                    guardian.tickEnvironmentalCombat(level);
                }
                helper.assertTrue(guardian.isPreparingEnvironmentalAttack(), "A floor warning must be pending");
                // Administrative policy is rechecked at release, even if it changes during the warning.
                level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
                for (int tick = 0; tick < 30; tick++) {
                    guardian.tickEnvironmentalCombat(level);
                }
                helper.assertTrue(
                        level.getBlockState(lavaGround).is(Blocks.STONE),
                        "Disabling mob griefing during windup must prevent floor collapse"
                );
            } finally {
                level.getGameRules().set(GameRules.MOB_GRIEFING, originalMobGriefing, level.getServer());
            }
            helper.succeed();
        } finally {
            if (protection != null) {
                protection.close();
            }
            if (player != null) {
                player.discard();
            }
            if (victim != null) {
                victim.discard();
            }
            if (restored != null) {
                restored.discard();
            }
            level.getEntitiesOfClass(
                    FallingBlockEntity.class,
                    new net.minecraft.world.phys.AABB(guardianPosition).inflate(32.0D)
            ).forEach(Entity::discard);
            findStructureGuardians(level, source).forEach(Entity::discard);
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
        }
    }

    private static void earthGuardianCeilingDebrisHitsOnServerTicks(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos guardianStart = helper.absolutePos(new BlockPos(4, 3, 4));
        ChunkPos ticketCenter = new ChunkPos(
                guardianStart.getX() >> 4,
                guardianStart.getZ() >> 4
        );
        setForcedChunks(level, ticketCenter, true);
        EarthGuardian guardian = helper.spawn(
                RetoldEntityTypes.EARTH_GUARDIAN.get(),
                4.0D,
                3.0D,
                4.0D,
                EntitySpawnReason.COMMAND
        );
        Zombie victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 16, 3, 4);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos attackFloor = helper.absolutePos(new BlockPos(16, 3, 4));
        BlockPos attackCeiling = attackFloor.above(6);
        BlockPos movedFloor = helper.absolutePos(new BlockPos(16, 3, 10));
        BlockPos decoyCeiling = movedFloor.above(6);

        level.setBlockAndUpdate(attackFloor.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(attackFloor, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(attackCeiling, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(decoyCeiling, Blocks.STONE.defaultBlockState());
        player.snapTo(
                attackFloor.getX() + 0.5D,
                attackFloor.getY(),
                attackFloor.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        victim.snapTo(
                attackFloor.getX() + 0.5D,
                attackFloor.getY(),
                attackFloor.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        guardian.getVibrationUser().onReceiveVibration(
                level,
                attackFloor,
                net.minecraft.world.level.gameevent.GameEvent.STEP,
                player,
                null,
                12.0F
        );
        helper.assertValueEqual(
                guardian.lifecycle(),
                EarthGuardianLifecycle.ROAMING,
                "The real-tick fixture guardian lifecycle"
        );
        helper.assertValueEqual(
                guardian.vibrationTarget(),
                attackFloor,
                "The real-tick fixture vibration target"
        );
        float startingHealth = victim.getHealth();

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.isPreparingEnvironmentalAttack(),
                        "A real guardian tick must begin the environmental warning"
                                + " (lifecycle=" + guardian.lifecycle()
                                + ", removed=" + guardian.isRemoved()
                                + ", noAi=" + guardian.isNoAi()
                                + ", tickCount=" + guardian.tickCount
                                + ", position=" + guardian.position()
                                + ", vibration=" + guardian.vibrationTarget()
                                + ", floor=" + level.getBlockState(attackFloor)
                                + ", support=" + level.getBlockState(attackFloor.below())
                                + ", ceiling=" + level.getBlockState(attackCeiling) + ")"
                ))
                .thenExecute(() -> player.snapTo(
                        movedFloor.getX() + 0.5D,
                        movedFloor.getY(),
                        movedFloor.getZ() + 0.5D,
                        0.0F,
                        0.0F
                ))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(
                            level.getBlockState(attackCeiling).is(Blocks.STONE),
                            "The real warning interval must not release ceiling debris early"
                    );
                    helper.assertValueEqual(
                            victim.getHealth(),
                            startingHealth,
                            "The warning interval must remain non-damaging on real ticks"
                    );
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        level.getBlockState(attackCeiling).isAir(),
                        "The locked original ceiling must release after the warning"
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        victim.getHealth() < startingHealth
                                && victim.getLastDamageSource() != null
                                && victim.getLastDamageSource().is(DamageTypes.FALLING_BLOCK),
                        "Physical falling debris must hit with the falling-block damage source"
                ))
                .thenExecute(() -> helper.assertTrue(
                        level.getBlockState(decoyCeiling).is(Blocks.STONE),
                        "Moving after lock-on must not redirect collapse to the player's new position"
                ))
                .thenExecute(() -> {
                    player.discard();
                    victim.discard();
                    guardian.discard();
                    setForcedChunks(level, ticketCenter, false);
                })
                .thenSucceed();
    }

    private static void earthGuardianLavaGroundBurnsOnServerTicks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setTime(18_000L);
        EarthGuardian guardian = helper.spawn(
                RetoldEntityTypes.EARTH_GUARDIAN.get(), 4.0D, 3.0D, 4.0D, EntitySpawnReason.COMMAND
        );
        ChunkPos ticketCenter = guardian.chunkPosition();
        setForcedChunks(level, ticketCenter, true);
        BlockPos target = helper.absolutePos(new BlockPos(16, 3, 4));
        BlockPos ground = target.below();
        BlockPos escapedGround = ground.south(6);
        // A stone basin contains fluid spread so the test measures contact damage at the locked floor.
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(ground.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(ground.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        level.setBlockAndUpdate(escapedGround, Blocks.STONE.defaultBlockState());
        Zombie victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 16, 3, 4);
        Zombie escapedVictim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 16, 3, 10);
        victim.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
        escapedVictim.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(escapedGround.above()));
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
        guardian.getVibrationUser().onReceiveVibration(
                level, target, net.minecraft.world.level.gameevent.GameEvent.STEP, player, null, 12.0F
        );
        float startingHealth = victim.getHealth();
        float escapedHealth = escapedVictim.getHealth();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.isPreparingEnvironmentalAttack(), "The open-sky clue must begin a lava warning"
                ))
                .thenExecute(() -> player.setPos(
                        net.minecraft.world.phys.Vec3.atBottomCenterOf(escapedGround.above())
                ))
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(ground).is(Blocks.STONE), "Warning must leave ground intact");
                    helper.assertValueEqual(victim.getHealth(), startingHealth, "Warning must remain harmless");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        level.getBlockState(ground).is(Blocks.LAVA), "The original supporting block must become lava"
                ))
                .thenWaitUntil(() -> helper.assertTrue(
                        victim.getHealth() < startingHealth
                                && victim.getLastDamageSource() != null
                                && victim.getLastDamageSource().is(DamageTypes.LAVA)
                                && victim.getY() < target.getY(),
                        "The victim must sink into the changed floor and take vanilla lava contact damage"
                ))
                .thenExecute(() -> {
                    helper.assertTrue(level.getFluidState(ground).isSource(), "The source must persist after release");
                    helper.assertTrue(
                            level.getBlockState(escapedGround).is(Blocks.STONE),
                            "Moving after the warning must not redirect ground conversion"
                    );
                    helper.assertValueEqual(escapedVictim.getHealth(), escapedHealth, "Leaving the patch must avoid damage");
                    player.discard();
                    victim.discard();
                    escapedVictim.discard();
                    guardian.discard();
                    setForcedChunks(level, ticketCenter, false);
                })
                .thenSucceed();
    }

    private static void earthGuardianFloorCollapseCausesRealFall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setTime(18_000L);
        EarthGuardian guardian = helper.spawn(
                RetoldEntityTypes.EARTH_GUARDIAN.get(), 4.0D, 3.0D, 4.0D, EntitySpawnReason.COMMAND
        );
        ChunkPos ticketCenter = guardian.chunkPosition();
        setForcedChunks(level, ticketCenter, true);
        BlockPos target = helper.absolutePos(new BlockPos(16, 3, 4));
        BlockPos ground = target.below();
        BlockPos escapedGround = ground.south(6);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int depth = 1; depth <= 5; depth++) {
                    level.setBlockAndUpdate(target.offset(x, -depth, z), Blocks.STONE.defaultBlockState());
                }
            }
        }
        level.setBlockAndUpdate(guardian.blockPosition().below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(escapedGround, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(ground, Blocks.BEDROCK.defaultBlockState());
        level.setBlockAndUpdate(ground.south(), Blocks.CHEST.defaultBlockState());
        Zombie victim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 16, 3, 4);
        victim.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));
        Zombie escapedVictim = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, 16, 3, 10);
        escapedVictim.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(escapedGround.above()));
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(target));

        CompoundTag saved = saveAdditionalState(level, guardian);
        saved.remove("retold_next_environmental_attack");
        saved.putBoolean("retold_prefer_lava_attack", true);
        guardian.readAdditionalSaveData(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved
        ));
        helper.assertValueEqual(
                saveAdditionalState(level, guardian).getIntOr("retold_next_environmental_attack", -1),
                1,
                "Legacy lava preference must migrate without changing the next attack"
        );
        saved.putInt("retold_next_environmental_attack", 2);
        guardian.readAdditionalSaveData(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), saved
        ));
        guardian.getVibrationUser().onReceiveVibration(
                level, target, net.minecraft.world.level.gameevent.GameEvent.STEP, player, null, 12.0F
        );
        guardian.tickEnvironmentalCombat(level);
        helper.assertTrue(guardian.isPreparingEnvironmentalAttack(), "Floor collapse must start with a warning");
        CompoundTag pending = saveAdditionalState(level, guardian);
        helper.assertValueEqual(pending.getIntOr("retold_environmental_attack_type", -1), 2, "Saved floor hazard");
        helper.assertValueEqual(pending.getIntOr("retold_next_environmental_attack", -1), 0, "Rotation returns to ceiling");
        guardian.readAdditionalSaveData(TagValueInput.create(
                ProblemReporter.DISCARDING, level.registryAccess(), pending
        ));
        // Register after the warning to verify that protection is checked at the moment of removal.
        RetoldWorldProtection.Registration protection = RetoldWorldProtection.register(
                Identifier.fromNamespaceAndPath("retold", "earth_guardian_floor_test"),
                context -> context.type() != RetoldWorldMutationType.MOB_BREAK
                        || !context.pos().equals(ground.north())
        );
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(escapedGround.above()));
        float startingHealth = victim.getHealth();
        float escapedHealth = escapedVictim.getHealth();
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(ground).is(Blocks.BEDROCK), "Warning leaves support intact");
                    helper.assertValueEqual(victim.getHealth(), startingHealth, "Warning must remain harmless");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        level.getBlockState(ground).isAir(), "The locked floor must open after reload"
                ))
                .thenExecute(() -> {
                    for (int depth = 1; depth <= 4; depth++) {
                        helper.assertTrue(level.getBlockState(target.below(depth)).isAir(), "Pit must reach four blocks deep");
                        helper.assertTrue(
                                level.getBlockState(target.north().below(depth)).is(Blocks.STONE),
                                "Protected support and its underlying column must remain intact"
                        );
                    }
                    helper.assertTrue(level.getBlockState(target.below(5)).is(Blocks.STONE), "Pit depth must be bounded");
                    helper.assertTrue(level.getBlockState(ground.east(2)).is(Blocks.STONE), "Pit width must be bounded");
                    helper.assertTrue(level.getBlockEntity(ground.south()) == null, "Containers are also valid floor material");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        victim.getY() <= target.getY() - 3.9D
                                && victim.getHealth() < startingHealth
                                && victim.getLastDamageSource() != null
                                && victim.getLastDamageSource().is(DamageTypes.FALL),
                        "The opened pit must cause real falling and vanilla landing damage"
                ))
                .thenExecute(() -> {
                    helper.assertValueEqual(escapedVictim.getHealth(), escapedHealth, "Safe ground must avoid damage");
                    helper.assertTrue(level.getBlockState(escapedGround).is(Blocks.STONE), "The pit must not follow the player");
                    protection.close();
                    player.discard();
                    victim.discard();
                    escapedVictim.discard();
                    guardian.discard();
                    setForcedChunks(level, ticketCenter, false);
                })
                .thenSucceed();
    }

    private static void earthGuardianPhysicallyInvestigatesVibrations(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (int x = 0; x <= 38; x++) {
            for (int z = 1; z <= 7; z++) {
                level.setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.STONE.defaultBlockState());
            }
        }
        EarthGuardian guardian = helper.spawn(
                RetoldEntityTypes.EARTH_GUARDIAN.get(), 4.0D, 3.0D, 4.0D, EntitySpawnReason.COMMAND
        );
        BlockPos start = guardian.blockPosition();
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                start, new BoundingBox(start.getX() - 8, start.getY() - 4, start.getZ() - 8,
                start.getX() + 38, start.getY() + 8, start.getZ() + 8)
        );
        guardian.configureForLabyrinth(level, source, EarthGuardianLifecycle.ROAMING);
        BlockPos middle = helper.absolutePos(new BlockPos(19, 3, 4));
        ChunkPos ticketCenter = new ChunkPos(middle.getX() >> 4, middle.getZ() >> 4);
        ChunkPos guardianChunk = guardian.chunkPosition();
        setForcedChunks(level, ticketCenter, true);
        setForcedChunks(level, guardianChunk, true);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos clue = helper.absolutePos(new BlockPos(34, 3, 4));
        player.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(clue));
        guardian.getVibrationUser().onReceiveVibration(
                level, clue, net.minecraft.world.level.gameevent.GameEvent.STEP, player, null, 30.0F
        );
        double startingX = guardian.getX();
        int[] repairs = {0};
        helper.onEachTick(() -> {
            if (guardian.isAlive() && guardian.tickCount > 0 && guardian.tickCount % 40 == 0) {
                net.minecraft.world.phys.Vec3 position = guardian.position();
                var path = guardian.getNavigation().getPath();
                var owner = RetoldAiControl.getOwner(guardian);
                EarthGuardianSpawner.spawnIfNeeded(level, source);
                helper.assertValueEqual(guardian.position(), position, "Periodic source repair must not interrupt physical pursuit");
                helper.assertTrue(guardian.getNavigation().getPath() == path, "Repair must retain the current navigation path");
                helper.assertValueEqual(RetoldAiControl.getOwner(guardian), owner, "Repair must retain movement ownership");
                repairs[0]++;
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(repairs[0] > 0, "A real-tick pursuit must cross a periodic source repair"))
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.getX() > startingX + 2.0D,
                        "The guardian must physically approach a distant remembered clue"
                                + "; position=" + guardian.position()
                                + ", ticks=" + guardian.tickCount
                                + ", path=" + guardian.getNavigation().getPath()
                                + ", owner=" + RetoldAiControl.getOwner(guardian)
                ))
                .thenExecute(() -> {
                    helper.assertTrue(
                            RetoldAiControl.isControlledBy(guardian, RetoldAiControlOwner.EARTH_GUARDIAN),
                            "Investigation must retain its movement ownership"
                    );
                    helper.assertTrue(guardian.getTarget() == null, "Physical pursuit must not grant a sight target");
                    player.discard();
                    guardian.discard();
                    EarthGuardianEncounterData.get(level).setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
                    setForcedChunks(level, ticketCenter, false);
                    setForcedChunks(level, guardianChunk, false);
                })
                .thenSucceed();
    }

    private static void earthGuardianPhysicallyPatrolsMaze(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos fixtureCenter = helper.absolutePos(new BlockPos(12, 3, 12));
        ChunkPos ticketCenter = new ChunkPos(fixtureCenter.getX() >> 4, fixtureCenter.getZ() >> 4);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticketCenter, 2);
        for (int x = 0; x <= 24; x++) {
            for (int z = 0; z <= 24; z++) {
                level.setBlockAndUpdate(helper.absolutePos(new BlockPos(x, 2, z)), Blocks.STONE.defaultBlockState());
            }
        }
        EarthGuardian guardian = helper.spawn(
                RetoldEntityTypes.EARTH_GUARDIAN.get(), 12.0D, 3.0D, 12.0D, EntitySpawnReason.COMMAND
        );
        BlockPos start = guardian.blockPosition();
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                start, new BoundingBox(start.getX() - 7, start.getY(), start.getZ() - 7,
                start.getX() + 7, start.getY() + 5, start.getZ() + 7)
        );
        guardian.configureForLabyrinth(level, source, EarthGuardianLifecycle.ROAMING);
        guardian.getRandom().setSeed(11L);
        ServerPlayer observer = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        observer.setPos(net.minecraft.world.phys.Vec3.atCenterOf(start.above(8)));
        level.getChunkAt(guardian.blockPosition());
        net.minecraft.world.phys.Vec3 startPosition = guardian.position();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.distanceToSqr(startPosition) > 0.25D,
                        "An idle guardian must physically follow its selected maze patrol path"
                                + "; path=" + guardian.getNavigation().getPath()
                                + ", ticks=" + guardian.tickCount
                                + ", lifecycle=" + guardian.lifecycle()
                                + ", noAi=" + guardian.isNoAi()
                                + ", position=" + guardian.position()
                                + ", onGround=" + guardian.onGround()
                                + ", support=" + level.getBlockState(guardian.blockPosition().below())
                                + ", clue=" + guardian.vibrationTarget()
                                + ", owner=" + RetoldAiControl.getOwner(guardian)
                ))
                .thenExecute(() -> {
                    helper.assertTrue(
                            RetoldAiControl.isControlledBy(guardian, RetoldAiControlOwner.EARTH_GUARDIAN),
                            "Patrol movement must retain guardian ownership"
                    );
                    helper.assertTrue(
                            guardian.getTarget() == null && guardian.vibrationTarget() == null,
                            "Idle patrol must not invent an enemy or a vibration"
                    );
                    observer.discard();
                    guardian.discard();
                    EarthGuardianEncounterData.get(level).setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
                    level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticketCenter, 2);
                })
                .thenSucceed();
    }

    private static void setForcedChunks(
            ServerLevel level,
            ChunkPos center,
            boolean forced
    ) {
        for (int chunkX = center.x() - 1; chunkX <= center.x() + 1; chunkX++) {
            for (int chunkZ = center.z() - 1; chunkZ <= center.z() + 1; chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }

    private static void earthGuardianRelocatesAnyLoadedBlock(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos guardianPosition = helper.absolutePos(new BlockPos(8, 3, 8));
        EarthLabyrinthSource source = new EarthLabyrinthSource(
                guardianPosition,
                new BoundingBox(
                        guardianPosition.getX() - 1,
                        guardianPosition.getY() - 1,
                        guardianPosition.getZ() - 1,
                        guardianPosition.getX() + 1,
                        guardianPosition.getY() + 3,
                        guardianPosition.getZ() + 1
                )
        );
        EarthGuardianEncounterData encounterData = EarthGuardianEncounterData.get(level);
        ServerPlayer vibrationPlayer = null;
        EarthGuardian restored = null;
        RetoldWorldProtection.Registration registration = null;
        BlockPos wall = source.guardianPosition().east(2);
        BlockPos relocatedContainer = source.guardianPosition().west(3);
        BlockPos container = source.guardianPosition().south(2);
        BlockPos relocatedWall = source.guardianPosition().north(3);

        try {
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.ROAMING);
            helper.assertTrue(
                    EarthGuardianSpawner.spawnIfNeeded(level, source),
                    "The terrain fixture must create its roaming guardian"
            );
            EarthGuardian guardian = findStructureGuardians(level, source).getFirst();
            level.setBlockAndUpdate(wall, Blocks.BEDROCK.defaultBlockState());
            level.setBlockAndUpdate(relocatedContainer, Blocks.CAVE_AIR.defaultBlockState());
            level.setBlockAndUpdate(container, Blocks.CHEST.defaultBlockState());
            level.setBlockAndUpdate(relocatedWall, Blocks.CAVE_AIR.defaultBlockState());

            helper.assertTrue(
                    level.getBlockEntity(container) != null,
                    "The unrestricted fixture must begin with a real container block entity"
            );

            registration = RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath(
                            "retold",
                            "earth_guardian_terrain_test"
                    ),
                    context -> context.type() != RetoldWorldMutationType.MOB_BREAK
                            || !context.pos().equals(container)
            );
            helper.assertFalse(
                    guardian.tryExcavateBlock(level, container),
                    "Administrative world protection must still be able to deny excavation"
            );
            registration.close();
            registration = null;

            helper.assertTrue(
                    guardian.tryExcavateBlock(level, container),
                    "A container outside the labyrinth must be valid terrain material"
            );
            helper.assertTrue(
                    level.getBlockState(container).isAir()
                            && level.getBlockEntity(container) == null,
                    "Excavating a container must remove its block and block entity"
            );
            helper.assertTrue(
                    guardian.tryPlaceReservedBlock(level, relocatedContainer),
                    "The stored container state must be placeable outside the labyrinth"
            );
            helper.assertTrue(
                    level.getBlockState(relocatedContainer).is(Blocks.CHEST),
                    "Relocation must preserve an unrestricted container block state"
            );
            vibrationPlayer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            BlockPos clue = source.guardianPosition().east(10);
            vibrationPlayer.snapTo(
                    clue.getX() + 0.5D,
                    clue.getY(),
                    clue.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
            guardian.getVibrationUser().onReceiveVibration(
                    level,
                    clue,
                    net.minecraft.world.level.gameevent.GameEvent.STEP,
                    vibrationPlayer,
                    null,
                    10.0F
            );
            guardian.tickTerrainControl(level);
            helper.assertTrue(
                    level.getBlockState(wall).isAir(),
                    "Following an outside vibration must excavate even Bedrock outside the maze"
            );
            helper.assertValueEqual(
                    guardian.terrainReserveSize(),
                    1,
                    "Excavated reserve size"
            );

            CompoundTag saved = saveAdditionalState(level, guardian);
            restored = RetoldEntityTypes.EARTH_GUARDIAN.get().create(
                    level,
                    EntitySpawnReason.LOAD
            );
            helper.assertTrue(restored != null, "A terrain-shaping guardian must restore");
            restored.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    level.registryAccess(),
                    saved
            ));
            helper.assertValueEqual(
                    restored.terrainReserveSize(),
                    1,
                    "The conserved terrain reserve must survive save/load"
            );

            registration = RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath(
                            "retold",
                            "earth_guardian_terrain_test"
                    ),
                    context -> context.type() != RetoldWorldMutationType.MOB_PLACE
                            || !context.pos().equals(relocatedWall)
            );
            helper.assertFalse(
                    restored.tryPlaceReservedBlock(level, relocatedWall),
                    "Administrative world protection must still be able to deny placement"
            );
            registration.close();
            registration = null;

            helper.assertTrue(
                    restored.tryPlaceReservedBlock(level, relocatedWall),
                    "The guardian must relocate stored Bedrock beyond the labyrinth"
            );
            helper.assertTrue(
                    level.getBlockState(relocatedWall).is(Blocks.BEDROCK),
                    "Relocation must preserve the exact excavated material"
            );
            helper.assertValueEqual(
                    restored.terrainReserveSize(),
                    0,
                    "Placed material must be consumed rather than duplicated"
            );
            helper.succeed();
        } finally {
            if (registration != null) {
                registration.close();
            }
            if (restored != null) {
                restored.discard();
            }
            if (vibrationPlayer != null) {
                vibrationPlayer.discard();
            }
            findStructureGuardians(level, source).forEach(Entity::discard);
            encounterData.setLifecycle(source.key(), EarthGuardianLifecycle.DORMANT);
            level.setBlockAndUpdate(wall, Blocks.CAVE_AIR.defaultBlockState());
            level.setBlockAndUpdate(relocatedContainer, Blocks.CAVE_AIR.defaultBlockState());
            level.setBlockAndUpdate(container, Blocks.CAVE_AIR.defaultBlockState());
            level.setBlockAndUpdate(relocatedWall, Blocks.CAVE_AIR.defaultBlockState());
        }
    }

    private static List<EarthGuardian> findStructureGuardians(
            ServerLevel level,
            EarthLabyrinthSource source
    ) {
        return level.getEntities(
                (Entity) null,
                source.duplicateSearchBounds(),
                entity -> entity instanceof EarthGuardian guardian
                        && guardian.isStructureBound()
                        && guardian.labyrinthKey() == source.key()
        ).stream().map(EarthGuardian.class::cast).toList();
    }

    private static CompoundTag saveAdditionalState(
            ServerLevel level,
            EarthGuardian guardian
    ) {
        TagValueOutput output = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        guardian.addAdditionalSaveData(output);
        return output.buildResult();
    }

    private static BlockPos cellCenter(
            EarthLabyrinthLayout layout,
            EarthLabyrinthLayout.Cell cell,
            BlockPos labyrinthCenter,
            int pyramidBaseY,
            long layoutSeed
    ) {
        return EarthLabyrinthGenerator.cellCenter(
                layout,
                cell,
                labyrinthCenter.getX(),
                labyrinthCenter.getZ(),
                pyramidBaseY,
                layoutSeed
        );
    }

    private static boolean hasTunnelTurn(List<BlockPos> path) {
        for (int index = 1; index + 1 < path.size(); index++) {
            BlockPos previous = path.get(index - 1);
            BlockPos current = path.get(index);
            BlockPos next = path.get(index + 1);
            int firstDeltaX = current.getX() - previous.getX();
            int firstDeltaZ = current.getZ() - previous.getZ();
            int secondDeltaX = next.getX() - current.getX();
            int secondDeltaZ = next.getZ() - current.getZ();

            if (firstDeltaX != secondDeltaX || firstDeltaZ != secondDeltaZ) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasGeneratedLadder(
            GameTestHelper helper,
            EarthLabyrinthLayout.Passage passage,
            EarthLabyrinthLayout layout,
            BlockPos labyrinthCenter,
            int pyramidBaseY,
            long layoutSeed
    ) {
        EarthLabyrinthLayout.Cell lowerCell = passage.first().level() == 1
                ? passage.first()
                : passage.second();
        BlockPos lower = cellCenter(
                layout,
                lowerCell,
                labyrinthCenter,
                pyramidBaseY,
                layoutSeed
        );
        return helper.getLevel().getBlockState(lower.south().above()).is(Blocks.LADDER);
    }

    private static boolean isMazeStone(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getBlockState(pos).is(Blocks.STONE)
                || helper.getLevel().getBlockState(pos).is(Blocks.DEEPSLATE)
                || helper.getLevel().getBlockState(pos).is(Blocks.ANDESITE)
                || helper.getLevel().getBlockState(pos).is(Blocks.TUFF)
                || helper.getLevel().getBlockState(pos).is(Blocks.COBBLESTONE)
                || helper.getLevel().getBlockState(pos).is(Blocks.COBBLED_DEEPSLATE)
                || helper.getLevel().getBlockState(pos).is(Blocks.MOSSY_COBBLESTONE);
    }

    private static PiecesContainer roundTrip(
            StructurePieceSerializationContext context,
            PiecesContainer pieces
    ) {
        ListTag saved = (ListTag) pieces.save(context);
        return PiecesContainer.load(saved, context);
    }

    private static void register(
            RegisterGameTestsEvent event,
            TestData<Holder<TestEnvironmentDefinition<?>>> testData,
            String name,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath("retold", name),
                new InlineGameTest(testData, test)
        );
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
