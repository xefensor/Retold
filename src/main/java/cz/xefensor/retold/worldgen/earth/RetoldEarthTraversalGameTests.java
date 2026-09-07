package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
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

final class RetoldEarthTraversalGameTests {
    private RetoldEarthTraversalGameTests() {
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        var pursuitData = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 2000, 0, true);
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_follows_real_steps_into_tunnel"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, pursuitData) {
                    @Override
                    public void run(GameTestHelper helper) {
                        followRealSteps(helper);
                    }
                });
        var policyData = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 40, 0, true);
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_ramp_conserves_and_respects_protection"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, policyData) {
                    @Override
                    public void run(GameTestHelper helper) {
                        routePolicy(helper);
                    }
                });
        for (int rise : new int[]{0, 14, -14}) {
            String name = rise == 0 ? "low_tunnel" : rise > 0 ? "ascending_ramp" : "descending_ramp";
            // A solid-bank ramp includes hundreds of batched edits and conserved backfill.
            var data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 24000, 0, true);
            event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_carves_" + name),
                    new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                        @Override
                        public void run(GameTestHelper helper) {
                            traverse(helper, rise);
                        }
                    });
        }
    }

    private static void followRealSteps(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
        ChunkPos ticket = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticket, 4);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        for (int x = -4; x <= 22; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -1; y <= 7; y++) {
                    boolean solid = y < 0 || (x >= 3 && (Math.abs(z) >= 2 || y >= 3));
                    level.setBlock(origin.offset(x, y, z), solid
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atBottomCenterOf(origin.east(14)));
        // VibrationInfo retains the live source reference. Do not register this clientless mock
        // for vanilla entity tracking, which requires a real player network connection.
        // Keep combat enabled but protect the remote player pad, isolating route construction
        // from destruction of the fixture's destination. No direct vibration callback is used.
        var protection = cz.xefensor.retold.api.world.RetoldWorldProtection.register(
                Identifier.fromNamespaceAndPath("retold", "earth_real_steps_pad"),
                context -> context.pos().getX() < origin.getX() + 12);
        int[] heard = {0};
        helper.onEachTick(() -> {
            if (!guardian.isAlive()) {
                return;
            }
            if (guardian.tickCount % 20 == 0) {
                player.setPos(Vec3.atBottomCenterOf(origin.offset(14 + (guardian.tickCount / 20) % 2, 0, 0)));
                level.gameEvent(GameEvent.STEP, player.position(), GameEvent.Context.of(player));
            }
            if (guardian.vibrationTarget() != null) {
                heard[0]++;
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        guardian.getX() > origin.getX() + 8,
                        "Real moving footsteps must draw the full-sized guardian into its carved tunnel"
                                + "; pos=" + guardian.position() + ", heard=" + heard[0]
                                + ", clue=" + guardian.vibrationTarget() + ", path=" + guardian.getNavigation().getPath()
                                + ", reserve=" + guardian.terrainReserveSize() + ", route=" + guardian.routeDescription()))
                .thenExecute(() -> {
                    helper.assertTrue(heard[0] > 0, "Real event delivery must reach the listener");
                    helper.assertTrue(level.getBlockState(origin.offset(5, 3, 0)).isAir(), "The tunnel ceiling must be physically widened");
                    helper.assertTrue(guardian.getTarget() == null, "Hearing must not grant a sight target");
                    protection.close();
                    player.discard();
                    guardian.discard();
                    level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
                    level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticket, 4);
                })
                .thenSucceed();
    }

    private static void routePolicy(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos feet = helper.absolutePos(new BlockPos(8, 4, 8));
        for (int x = -3; x <= 5; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = -1; y <= 5; y++) {
                    level.setBlock(feet.offset(x, y, z), y == -1 ? Blocks.STONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        guardian.setNoAi(true);
        var route = new EarthGuardianRoute(guardian);
        BlockPos roof = feet.above(3);
        BlockPos support = feet.east(2).north();
        BlockPos clue = feet.east(12).above(14);
        level.setBlock(roof, Blocks.STONE.defaultBlockState(), 2);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        var denyBreak = cz.xefensor.retold.api.world.RetoldWorldProtection.register(
                Identifier.fromNamespaceAndPath("retold", "earth_route_break"),
                context -> !context.pos().equals(roof));
        try {
            route.tick(level, clue);
            helper.assertTrue(level.getBlockState(roof).is(Blocks.STONE) && guardian.terrainReserveSize() == 0,
                    "Protected headroom must not be excavated or grant reserve material");
        } finally {
            denyBreak.close();
        }
        try {
            route.tick(level, clue);
            helper.assertTrue(level.getBlockState(roof).isAir() && guardian.terrainReserveSize() == 1,
                    "One ceiling removal must produce exactly one reserve state");
            level.getGameRules().set(GameRules.MOB_GRIEFING, false, level.getServer());
            route.tick(level, clue);
            helper.assertTrue(level.getBlockState(support).isAir() && guardian.terrainReserveSize() == 1,
                    "Disabling griefing during construction must prevent support placement");
            level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
            var denyPlace = cz.xefensor.retold.api.world.RetoldWorldProtection.register(
                    Identifier.fromNamespaceAndPath("retold", "earth_route_place"),
                    context -> !context.pos().equals(support));
            try {
                route.tick(level, clue);
                helper.assertTrue(level.getBlockState(support).isAir() && guardian.terrainReserveSize() == 1,
                        "Denied placement must not consume a reserved block");
            } finally {
                denyPlace.close();
            }
            route.tick(level, clue);
            helper.assertTrue(level.getBlockState(support).is(Blocks.STONE) && guardian.terrainReserveSize() == 0,
                    "A ramp step must spend the exact excavated stone state");
            var output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
            route.save(output);
            var restored = new EarthGuardianRoute(guardian);
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
            restored.tick(level, feet.west(12).above(14));
            var reSaved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
            restored.save(reSaved);
            helper.assertValueEqual(reSaved.buildResult().getIntOr("retold_ramp_direction", -1),
                    net.minecraft.core.Direction.EAST.get2DDataValue(),
                    "Reload must retain the uphill heading instead of reversing onto the preceding steps");
            for (int index = 0; index < 140; index++) {
                BlockPos donor = feet.offset(index % 14, 0, 10 + index / 14);
                level.setBlock(donor, Blocks.STONE.defaultBlockState(), 2);
                guardian.tryExcavateBlock(level, donor);
            }
            helper.assertValueEqual(guardian.terrainReserveSize(), 128, "The expanded reserve must remain bounded");
            var entityOutput = net.minecraft.world.level.storage.TagValueOutput.createWithContext(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
            guardian.addAdditionalSaveData(entityOutput);
            guardian.readAdditionalSaveData(net.minecraft.world.level.storage.TagValueInput.create(
                    net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), entityOutput.buildResult()));
            helper.assertValueEqual(guardian.terrainReserveSize(), 128, "Reload must preserve the complete reserve");
            helper.succeed();
        } finally {
            guardian.discard();
            level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
        }
    }

    private static void traverse(GameTestHelper helper, int rise) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 22, 8));
        ChunkPos ticket = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
        // Entity ticking covers fewer rings than mere chunk loading; the complete ramp can
        // extend over forty blocks from its start, with varying chunk alignment between runs.
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticket, 8);
        boolean previousGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        for (int x = -4; x <= 54; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -16; y <= 19; y++) {
                    boolean solid = y < 0 || (x >= 2 && y < rise)
                            || (rise == 0 && x >= 2 && x <= 6 && y == 3);
                    if (rise < 0 && x >= 22 && y >= rise && y < rise + 4) {
                        solid = false;
                    }
                    level.setBlock(origin.offset(x, y, z), solid
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 22.0D, 8.5D, EntitySpawnReason.COMMAND);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos clue = origin.offset(rise == 0 ? 30 : 24, rise, rise == 0 ? 0 : 6);
        // Isolate walking from combat: the guarded attack pad is beside, not on, the ramp route.
        var protection = cz.xefensor.retold.api.world.RetoldWorldProtection.register(
                Identifier.fromNamespaceAndPath("retold", "earth_traversal_attack_pad"),
                context -> rise == 0 || Math.abs(context.pos().getX() - clue.getX()) > 1
                        || Math.abs(context.pos().getZ() - clue.getZ()) > 1);
        player.setPos(Vec3.atBottomCenterOf(clue));
        helper.onEachTick(() -> {
            if (guardian.isAlive()) {
                guardian.getVibrationUser().onReceiveVibration(level, clue, GameEvent.BLOCK_DESTROY, player, null, 28.0F);
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        rise == 0 ? guardian.getX() > origin.getX() + 8
                                : Math.abs(guardian.getY() - clue.getY()) < 0.4D,
                        "Guardian must walk its excavated route: rise=" + rise + ", pos=" + guardian.position()
                                + ", ticks=" + guardian.tickCount + ", reserve=" + guardian.terrainReserveSize()
                                + ", health=" + guardian.getHealth() + ", removed=" + guardian.isRemoved()
                                + ", removal=" + guardian.getRemovalReason()
                                + ", damage=" + guardian.getLastDamageSource()
                                + ", path=" + guardian.getNavigation().getPath()
                                + ", route=" + guardian.routeDescription()))
                .thenExecute(() -> {
                    helper.assertTrue(guardian.getTarget() == null, "Terrain pursuit must not grant sight targeting");
                    if (rise == 0) {
                        helper.assertTrue(level.getBlockState(origin.offset(3, 3, 0)).isAir(),
                                "The low ceiling must actually be excavated for the 3.2-block guardian");
                    }
                    guardian.discard();
                    player.discard();
                    protection.close();
                    level.getGameRules().set(GameRules.MOB_GRIEFING, previousGriefing, level.getServer());
                    level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticket, 8);
                })
                .thenSucceed();
    }
}
