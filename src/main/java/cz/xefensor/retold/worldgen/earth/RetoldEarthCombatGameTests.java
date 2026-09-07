package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.registry.RetoldEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

final class RetoldEarthCombatGameTests {
    private RetoldEarthCombatGameTests() {
    }

    static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        var data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 200, 0, true);
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_melee_respects_contact_and_cooldown"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        contactPolicy(helper);
                    }
                });
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_guardian_pursues_during_hazards_and_melees"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        combinedCombat(helper);
                    }
                });
    }

    private static ServerPlayer player(GameTestHelper helper, GameType mode, BlockPos pos) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(mode);
        // Damage feedback needs a listener, but this fixture does not connect a real client.
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(),
                new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.connection.markClientLoaded();
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0D);
        player.setHealth(200.0F);
        player.setPos(Vec3.atBottomCenterOf(pos));
        return player;
    }

    private static void clue(EarthGuardian guardian, ServerPlayer player) {
        guardian.getVibrationUser().onReceiveVibration((net.minecraft.server.level.ServerLevel) guardian.level(),
                player.blockPosition(), GameEvent.STEP, player, null, 1.0F);
    }

    private static void contactPolicy(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        ServerPlayer survivor = player(helper, GameType.SURVIVAL, origin.east(2));
        ServerPlayer stranger = player(helper, GameType.SURVIVAL, origin.east(2));
        ServerPlayer creative = player(helper, GameType.CREATIVE, origin.east(2));
        ServerPlayer spectator = player(helper, GameType.SPECTATOR, origin.east(2));
        try {
            survivor.setPos(guardian.position().add(1.7D, 0.0D, 0.0D));
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "An unheard player must not grant melee targeting");
            clue(guardian, survivor);
            helper.assertFalse(guardian.doHurtTarget(level, stranger), "Melee must remain tied to the heard player");
            helper.assertFalse(guardian.doHurtTarget(level, creative), "Creative players must remain excluded");
            helper.assertFalse(guardian.doHurtTarget(level, spectator), "Spectators must remain excluded");
            survivor.setPos(guardian.position().add(8.0D, 0.0D, 0.0D));
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "Melee must not hit a distant heard player");
            survivor.setPos(guardian.position().add(1.7D, 0.0D, 0.0D));
            for (int y = 0; y <= 4; y++) {
                level.setBlock(origin.east().above(y), Blocks.STONE.defaultBlockState(), 2);
            }
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "A fresh wall must stop a contact strike");
            for (int y = 0; y <= 4; y++) {
                level.setBlock(origin.east().above(y), Blocks.AIR.defaultBlockState(), 2);
            }
            helper.assertTrue(guardian.isWithinMeleeAttackRange(survivor), "Fixture player must be within vanilla melee reach");
            helper.assertTrue(guardian.hasLineOfSight(survivor), "Fixture wall must be completely removed");
            helper.assertTrue(guardian.doHurtTarget(level, survivor), "An unobstructed heard player in reach must take a golem strike"
                    + "; player invulnerable=" + survivor.isInvulnerableTo(level, guardian.damageSources().mobAttack(guardian))
                    + ", source=" + guardian.vibrationSourcePlayer() + ", player=" + survivor.getUUID());
            helper.assertTrue(survivor.getHealth() < 200.0F, "Melee must deal real damage");
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "Repeated calls must respect the cooldown");
            var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            guardian.addAdditionalSaveData(saved);
            helper.assertValueEqual(saved.buildResult().getIntOr("retold_melee_cooldown", -1), 30, "Melee cooldown must persist");
            guardian.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved.buildResult()));
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "Reload must not allow an immediate second strike");
            helper.assertTrue(guardian.getTarget() == null, "Contact combat must not assign a live sight target");
            var dormant = saved.buildResult();
            dormant.putInt("retold_melee_cooldown", 0);
            dormant.putInt("retold_lifecycle", EarthGuardianLifecycle.DORMANT.ordinal());
            guardian.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), dormant));
            helper.assertFalse(guardian.doHurtTarget(level, survivor), "A dormant guardian must not attack even with a saved clue");
            helper.succeed();
        } finally {
            guardian.discard();
            survivor.discard();
            stranger.discard();
            creative.discard();
            spectator.discard();
        }
    }

    private static void combinedCombat(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 4, 8));
        ChunkPos ticket = new ChunkPos(origin.getX() >> 4, origin.getZ() >> 4);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticket, 4);
        boolean oldGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
        level.getGameRules().set(GameRules.MOB_GRIEFING, true, level.getServer());
        for (int x = -3; x <= 18; x++) {
            for (int z = -4; z <= 4; z++) {
                for (int y = -1; y <= 6; y++) {
                    level.setBlock(origin.offset(x, y, z), y == -1
                            ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        var guardian = helper.spawn(RetoldEntityTypes.EARTH_GUARDIAN.get(),
                8.5D, 4.0D, 8.5D, EntitySpawnReason.COMMAND);
        BlockPos destination = origin.east(10);
        BlockPos ceiling = destination.above(4);
        level.setBlock(ceiling, Blocks.STONE.defaultBlockState(), 2);
        ServerPlayer survivor = player(helper, GameType.SURVIVAL, destination);
        level.addFreshEntity(survivor);
        guardian.setNoAi(true);
        int[] warningStartTick = {0};
        double[] initialX = {guardian.getX()};
        helper.startSequence()
                // Chunk preparation can advance GameTest time without ticking the entity.
                .thenWaitUntil(() -> helper.assertTrue(guardian.tickCount >= 40, "Wait for loaded entity-ticking terrain"))
                .thenExecute(() -> guardian.setNoAi(false))
                .thenWaitUntil(() -> helper.assertTrue(guardian.getNavigation().createPath(destination, 0) != null,
                        "Fixture destination must have a navigable floor; pos=" + guardian.position()
                                + ", floor=" + level.getBlockState(guardian.blockPosition().below())
                                + ", onGround=" + guardian.onGround()))
                .thenExecute(() -> {
                    clue(guardian, survivor);
                    guardian.tickEnvironmentalCombat(level);
                    helper.assertTrue(guardian.isPreparingEnvironmentalAttack(), "The combined encounter must start a real hazard warning");
                    warningStartTick[0] = guardian.tickCount;
                    initialX[0] = guardian.getX();
                })
                .thenWaitUntil(() -> helper.assertTrue(guardian.tickCount >= warningStartTick[0] + 20, "Wait for actual warning ticks"))
                .thenExecute(() -> {
                    helper.assertTrue(guardian.isPreparingEnvironmentalAttack(), "The hazard must still be warning");
                    helper.assertTrue(guardian.getX() > initialX[0] + 1.0D, "The guardian must advance during its hazard warning"
                            + "; x=" + guardian.getX() + ", start=" + initialX[0] + ", ticks=" + guardian.tickCount
                            + ", path=" + guardian.getNavigation().getPath() + ", clue=" + guardian.vibrationTarget()
                            + ", pos=" + guardian.position() + ", noAi=" + guardian.isNoAi()
                            + ", route=" + guardian.routeDescription()
                            + ", owner=" + cz.xefensor.retold.behavior.control.RetoldAiControl.getOwner(guardian));
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        survivor.getLastDamageSource() != null && survivor.getLastDamageSource().is(DamageTypes.MOB_ATTACK)
                                && survivor.getLastDamageSource().getEntity() == guardian,
                        "Pursuit must close into an automatic melee strike; pos=" + guardian.position()
                                + ", clue=" + guardian.vibrationTarget() + ", health=" + survivor.getHealth()))
                .thenExecute(() -> {
                    helper.assertTrue(level.getBlockState(ceiling).isAir(), "The environmental ceiling attack must also release");
                    helper.assertTrue(guardian.getTarget() == null, "Combined attacks must not grant sight tracking");
                    level.players().remove(survivor);
                    survivor.discard();
                    guardian.discard();
                    level.getGameRules().set(GameRules.MOB_GRIEFING, oldGriefing, level.getServer());
                    level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticket, 4);
                })
                .thenSucceed();
    }
}
