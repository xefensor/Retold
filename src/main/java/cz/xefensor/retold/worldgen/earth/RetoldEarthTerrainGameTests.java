package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.api.world.RetoldWorldMutationType;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

final class RetoldEarthTerrainGameTests {
    private RetoldEarthTerrainGameTests() {
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        var data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 40, 0, true);
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_relocates_fluids_without_stalling"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        relocateFluids(helper);
                    }
                });
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_batches_excavation_with_protection"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        batchExcavation(helper);
                    }
                });
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_builds_conserved_trailing_walls"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        trailingWalls(helper);
                    }
                });
    }

    private static BlockPos prepareFloor(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
        for (int x = -8; x <= 16; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -1; y <= 5; y++) {
                    helper.getLevel().setBlock(origin.offset(x, y, z), y == -1
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        return origin;
    }

    private static void relocateFluids(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = prepareFloor(helper);
        var guardian = guardian(helper, origin);
        BlockPos source = origin.east(6);
        BlockPos destination = origin.east(10);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        try {
            level.setBlock(source, Blocks.LAVA.defaultBlockState(), 2);
            var protection = RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath("retold", "earth_fluid_denial"),
                    context -> !context.pos().equals(source));
            try {
                helper.assertTrue(!guardian.tryExcavateBlock(level, source), "Protected lava must remain untouched");
            } finally {
                protection.close();
            }
            level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
            helper.assertTrue(!guardian.tryExcavateBlock(level, source), "Griefing denial must also protect fluids");
            helper.assertValueEqual(guardian.terrainReserveSize(), 0, "Denied fluids must grant no material");
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            var states = java.util.List.of(
                    Blocks.LAVA.defaultBlockState(),
                    Blocks.WATER.defaultBlockState(),
                    Blocks.LAVA.defaultBlockState().setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, 5),
                    Blocks.OAK_SLAB.defaultBlockState().setValue(
                            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true));
            for (var state : states) {
                level.setBlock(source, state, 2);
                level.setBlock(destination, Blocks.AIR.defaultBlockState(), 2);
                helper.assertTrue(guardian.tryExcavateBlock(level, source), "Fluid-bearing state must actually be excavated: " + state);
                helper.assertTrue(level.getBlockState(source).isAir() && level.getFluidState(source).isEmpty(),
                        "Excavation must not leave a source/flowing/waterlogged fluid behind");
                helper.assertValueEqual(guardian.terrainReserveSize(), 1, "Exactly one complete state must enter the reserve");
                helper.assertTrue(!guardian.tryExcavateBlock(level, source), "The emptied source must not duplicate material");
                helper.assertTrue(guardian.tryPlaceReservedBlock(level, destination), "Stored fluid-bearing state must remain relocatable");
                helper.assertTrue(level.getBlockState(destination).equals(state), "Relocation must preserve the exact fluid-bearing state");
                helper.assertValueEqual(guardian.terrainReserveSize(), 0, "Successful placement must spend the state");
            }
            helper.succeed();
        } finally {
            guardian.discard();
            level.setBlock(source, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(destination, Blocks.AIR.defaultBlockState(), 2);
            level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
        }
    }

    private static EarthGuardian guardian(GameTestHelper helper, BlockPos origin) {
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        guardian.setNoAi(true);
        ServerPlayer source = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        guardian.getVibrationUser().onReceiveVibration(helper.getLevel(), origin.east(14),
                GameEvent.STEP, source, null, 14.0F);
        source.discard();
        return guardian;
    }

    private static void batchExcavation(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = prepareFloor(helper);
        var guardian = guardian(helper, origin);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        for (int x = 2; x <= 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(origin.offset(x, y, z), x == 3
                            ? Blocks.BEDROCK.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        try {
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 12, "One pulse must excavate a complete 3-by-4 slice, not one block");
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    helper.assertTrue(level.getBlockState(origin.offset(2, y, z)).isAir(), "The first full slice must be open");
                }
            }
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 12, "Repeated same-tick calls must not exceed the pulse budget");
            guardian.tickCount += 10;
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 24, "The next half-second pulse must also excavate Bedrock");
            var protection = RetoldWorldProtection.register(Identifier.fromNamespaceAndPath("retold", "earth_batch_stop"),
                    context -> context.type() != RetoldWorldMutationType.MOB_BREAK || guardian.terrainReserveSize() < 28);
            try {
                guardian.tickCount += 10;
                guardian.tickTerrainControl(level);
                helper.assertValueEqual(guardian.terrainReserveSize(), 28, "A mid-pulse denial must stop excavation without extra material");
            } finally {
                protection.close();
            }
            level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
            guardian.tickCount += 10;
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 28, "Griefing denial must stop the complete batch");
            helper.succeed();
        } finally {
            guardian.discard();
            level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
        }
    }

    private static void gather(GameTestHelper helper, EarthGuardian guardian, BlockPos origin, int count) {
        for (int index = 0; index < count; index++) {
            BlockPos donor = origin.offset(10 + index % 6, 4, index / 6 - 2);
            helper.getLevel().setBlock(donor, Blocks.STONE.defaultBlockState(), 2);
            helper.assertTrue(guardian.tryExcavateBlock(helper.getLevel(), donor), "Fixture material must be physically excavated");
        }
    }

    private static void trailingWalls(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = prepareFloor(helper);
        var guardian = guardian(helper, origin);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        var blocker = helper.spawn(EntityTypes.ZOMBIE, 5.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        blocker.setNoAi(true);
        blocker.setPos(blocker.position().add(0, 0, 3));
        BlockPos wall = origin.west(3);
        try {
            gather(helper, guardian, origin, 24);
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 15, "The production pulse must build a nine-block wall from stored material");
            for (int y = 0; y < 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos pos = wall.offset(0, y, z);
                    helper.assertTrue(level.getBlockState(pos).is(Blocks.STONE), "The trailing wall must be a coherent 3-by-3 plane");
                }
            }
            gather(helper, guardian, origin, 12);
            guardian.tickCount += 10;
            guardian.tickTerrainControl(level);
            helper.assertValueEqual(guardian.terrainReserveSize(), 27, "Wall creation must respect its separate two-second cadence");
            for (int y = 0; y < 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(wall.offset(0, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
            helper.assertValueEqual(EarthGuardianWalls.buildBehind(level, guardian, Direction.EAST), 0, "Griefing denial must prevent wall building");
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            BlockPos protectedCell = wall.north().above();
            var protection = RetoldWorldProtection.register(Identifier.fromNamespaceAndPath("retold", "earth_wall_cell"),
                    context -> !context.pos().equals(protectedCell));
            blocker.setPos(net.minecraft.world.phys.Vec3.atBottomCenterOf(wall));
            try {
                int placed = EarthGuardianWalls.buildBehind(level, guardian, Direction.EAST);
                helper.assertValueEqual(placed, 6, "The wall must skip the protected cell and two occupied cells");
                helper.assertValueEqual(guardian.terrainReserveSize(), 27 - placed, "Only successfully placed wall cells may consume material");
                helper.assertTrue(level.getBlockState(protectedCell).isAir(), "The protected wall cell must stay empty");
                helper.assertTrue(level.getBlockState(wall).isAir() && level.getBlockState(wall.above()).isAir(), "Walls must not be placed inside entities");
            } finally {
                protection.close();
            }
            helper.succeed();
        } finally {
            blocker.discard();
            guardian.discard();
            level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
        }
    }
}
