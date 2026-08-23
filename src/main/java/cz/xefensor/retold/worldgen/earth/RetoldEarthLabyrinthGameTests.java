package cz.xefensor.retold.worldgen.earth;

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
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.JungleTemplePiece;
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
