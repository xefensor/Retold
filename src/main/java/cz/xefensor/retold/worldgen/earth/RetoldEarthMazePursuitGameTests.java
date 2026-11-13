package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Integration against generated chamber/tunnel geometry, not a hand-built straight corridor. */
final class RetoldEarthMazePursuitGameTests {
    private RetoldEarthMazePursuitGameTests() {
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        var data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 2400, 0, true);
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_pursues_into_generated_maze"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        pursue(helper);
                    }
                });
    }

    private static void pursue(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(96, 96, 96));
        long seed = 0x4C41425952494E54L;
        var piece = new EarthLabyrinthPiece(center.getX(), center.getZ(), center.getY(),
                center, Direction.EAST, seed);
        var source = piece.guardianSource();
        BlockPos feet = source.guardianPosition();
        var ticket = ChunkPos.containing(feet);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticket, 8);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        // Supply the underground host stone that a void GameTest world otherwise lacks.
        var bounds = source.structureBounds();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                for (int y = feet.getY() - 6; y <= feet.getY() + 12; y++) {
                    level.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
            }
        }
        EarthLabyrinthGenerator.generate(level, bounds, center.getX(), center.getZ(), center.getY(),
                center, Direction.EAST, seed);
        List<BlockPos> trail = playerTrail(level, feet);
        helper.assertTrue(!trail.isEmpty(), "Generated lower floor must have a player route out of the arena");
        EarthGuardianEncounterData.get(level).setLifecycle(source.key(), EarthGuardianLifecycle.ROAMING);
        var guardian = RetoldEntityTypes.EARTH_GUARDIAN.get().create(level, EntitySpawnReason.STRUCTURE);
        helper.assertTrue(guardian != null, "Guardian must be registered");
        guardian.configureForLabyrinth(level, source, EarthGuardianLifecycle.ROAMING);
        level.addFreshEntity(guardian);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        int[] progress = {Math.min(8, trail.size() - 1)};
        int[] heard = {0};
        boolean[] erupted = {false};
        cleanupAfterTest(helper, () -> {
            guardian.discard();
            player.discard();
            level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
            level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticket, 8);
        });
        helper.onEachTick(() -> {
            if (!guardian.isAlive()) {
                return;
            }
            if (guardian.tickCount % 10 == 0) {
                erupted[0] |= trail.stream().anyMatch(pos -> level.getBlockState(pos.below()).is(Blocks.LAVA));
                player.setPos(Vec3.atBottomCenterOf(trail.get(progress[0])));
                level.gameEvent(GameEvent.STEP, player.position(), GameEvent.Context.of(player));
                if (guardian.tickCount > 40 && progress[0] < trail.size() - 1) {
                    progress[0]++;
                }
            }
            if (guardian.tickCount % 40 == 0) {
                EarthGuardianSpawner.spawnIfNeeded(level, source);
            }
            if (guardian.vibrationTarget() != null) {
                heard[0]++;
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.position().distanceToSqr(Vec3.atBottomCenterOf(trail.getLast())) < 25.0D
                                && guardian.position().distanceToSqr(Vec3.atBottomCenterOf(feet)) > 324.0D,
                        "Guardian must leave its generated arena and approach the heard player in the maze"
                                + "; pos=" + guardian.position() + ", origin=" + feet + ", end=" + trail.getLast()
                                + ", ticks=" + guardian.tickCount + ", heard=" + heard[0]
                                + ", clue=" + guardian.vibrationTarget() + ", health=" + guardian.getHealth()
                                + ", removed=" + guardian.getRemovalReason() + ", reserve=" + guardian.terrainReserveSize()
                                + ", path=" + guardian.getNavigation().getPath() + ", route=" + guardian.routeDescription()))
                .thenExecute(() -> {
                    helper.assertTrue(heard[0] > 0 && guardian.getTarget() == null,
                            "Pursuit must use actual dispatched footsteps without a sight target");
                    helper.assertTrue(erupted[0], "Pursuit must survive a real lava eruption on the player's trail");
                    cz.xefensor.retold.Retold.LOGGER.info("EARTH_MAZE_PURSUIT ticks={}, origin={}, reached={}, lavaSeen={}",
                            guardian.tickCount, feet, guardian.position(), erupted[0]);
                })
                .thenSucceed();
    }

    private static void cleanupAfterTest(GameTestHelper helper, Runnable cleanup) {
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {
            }

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
            }
        });
    }

    private static List<BlockPos> playerTrail(ServerLevel level, BlockPos start) {
        var queue = new ArrayDeque<BlockPos>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        queue.add(start);
        previous.put(start, start);
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (current.distSqr(start) >= 24 * 24) {
                List<BlockPos> trail = new ArrayList<>();
                for (BlockPos pos = current; !pos.equals(start); pos = previous.get(pos)) {
                    trail.add(pos);
                }
                trail.add(start);
                Collections.reverse(trail);
                return List.copyOf(trail);
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = current.relative(direction);
                if (!previous.containsKey(next) && next.distSqr(start) <= 26 * 26
                        && level.getBlockState(next).isAir() && level.getBlockState(next.above()).isAir()
                        && !level.getBlockState(next.below()).getCollisionShape(level, next.below()).isEmpty()) {
                    previous.put(next, current);
                    queue.addLast(next);
                }
            }
        }
        return List.of();
    }
}
