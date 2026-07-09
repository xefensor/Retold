package cz.xefensor.retold.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RetoldGuardianMiningPressure {
    private static final double GUARDIAN_BEAM_EFFECTIVE_RANGE = 18.0D;
    private static final double GUARDIAN_BEAM_EFFECTIVE_RANGE_SQR =
            GUARDIAN_BEAM_EFFECTIVE_RANGE * GUARDIAN_BEAM_EFFECTIVE_RANGE;
    private static final int GUARDIAN_BEAM_DAMAGE_COOLDOWN_TICKS = 60;

    private static final int MONUMENT_PRESSURE_GRACE_TICKS = 20;
    private static final int MONUMENT_PRESSURE_DECAY_TICKS = 10 * 20;
    private static final int MAX_MONUMENT_PRESSURE_LEVEL = 3;
    private static final int MONUMENT_PRESSURE_WARNING_COOLDOWN_TICKS = 16;
    private static final int MONUMENT_PRESSURE_WARNING_SPLASH_PARTICLES = 18;
    private static final int MONUMENT_PRESSURE_WARNING_BUBBLE_PARTICLES = 14;
    private static final int MONUMENT_PRESSURE_WARNING_ENCHANT_PARTICLES = 10;

    private static final int MINING_BLOCKED_FEEDBACK_COOLDOWN_TICKS = 8;
    private static final int MINING_BLOCKED_SPLASH_PARTICLES = 16;
    private static final int MINING_BLOCKED_ENCHANT_PARTICLES = 12;

    private static final Map<UUID, Long> LAST_MINING_BLOCKED_FEEDBACK_TICK_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> LAST_GUARDIAN_BEAM_DAMAGE_TICK_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Long> LAST_MONUMENT_PRESSURE_WARNING_TICK_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, MonumentPressure> MONUMENT_PRESSURE_BY_PLAYER = new HashMap<>();

    private RetoldGuardianMiningPressure() {
    }

    static void onBreakBlock(BreakBlockEvent event) {
        if (event.isCanceled()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            if (event.getLevel() instanceof Level clientLevel
                    && RetoldOceanMonumentSupport.isProtectedBlock(event.getState())) {
                noteLocalMonumentPressureGrace(clientLevel, event.getPlayer());
            }

            return;
        }

        if (!RetoldOceanMonumentSupport.isProtectedBlock(event.getState())) {
            return;
        }

        if (!RetoldOceanMonumentSupport.isValidMonumentAt(level, event.getPos())) {
            return;
        }

        triggerMonumentPressure(level, event.getPos(), event.getPlayer(), true);
    }

    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.isCanceled() || event.getPosition().isEmpty()) {
            return;
        }

        BlockPos pos = event.getPosition().get();
        Level level = event.getEntity().level();

        warnMonumentMiningAttempt(level, pos, event.getEntity(), event.getState());

        if (!isPlayerPressuredByGuardianBeam(level, event.getEntity())
                || isInMonumentPressureGrace(level, event.getEntity())) {
            return;
        }

        event.setNewSpeed(0.0F);
        event.setCanceled(true);
        interruptMiningWithGuardianBeam(level, pos, event.getEntity());
    }

    static void onGuardianTick(Guardian guardian) {
        RetoldGuardianAlertController.onGuardianTick(guardian);
    }

    static void handleGuardianDamage(LivingIncomingDamageEvent event, Player player, Guardian guardian) {
        if (guardian.distanceToSqr(player) > GUARDIAN_BEAM_EFFECTIVE_RANGE_SQR) {
            event.setCanceled(true);
            return;
        }

        long gameTime = player.level().getGameTime();
        Long lastDamageTick = LAST_GUARDIAN_BEAM_DAMAGE_TICK_BY_PLAYER.get(player.getUUID());

        if (lastDamageTick != null && gameTime - lastDamageTick < GUARDIAN_BEAM_DAMAGE_COOLDOWN_TICKS) {
            event.setCanceled(true);
            return;
        }

        LAST_GUARDIAN_BEAM_DAMAGE_TICK_BY_PLAYER.put(player.getUUID(), gameTime);
    }

    static void removeGuardianAlert(UUID guardianId) {
        RetoldGuardianAlertController.removeAlert(guardianId);
    }

    private static void warnMonumentMiningAttempt(Level level, BlockPos pos, Player player, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!RetoldOceanMonumentSupport.isProtectedBlock(state)) {
            return;
        }

        if (!canPlayMonumentPressureWarning(serverLevel, player)) {
            return;
        }

        if (!RetoldOceanMonumentSupport.isValidMonumentAt(serverLevel, pos)) {
            return;
        }

        markMonumentPressureWarningPlayed(serverLevel, player);
        triggerMonumentPressure(serverLevel, pos, player, true);
    }

    private static void triggerMonumentPressure(ServerLevel level, BlockPos pos, Player player, boolean playWarning) {
        MonumentPressure pressure = noteMonumentPressure(level, player);
        RetoldGuardianAlertController.alertNearby(level, pos, player, pressure.level());

        if (playWarning) {
            playMonumentPressureWarning(level, pos, player, pressure);
        }
    }

    private static MonumentPressure noteMonumentPressure(ServerLevel level, Player player) {
        long gameTime = level.getGameTime();
        MonumentPressure previousPressure = MONUMENT_PRESSURE_BY_PLAYER.get(player.getUUID());
        boolean stillPressured = previousPressure != null && gameTime <= previousPressure.expiresAt();
        int nextLevel = stillPressured ? Math.min(previousPressure.level() + 1, MAX_MONUMENT_PRESSURE_LEVEL) : 1;
        long graceUntil = stillPressured ? previousPressure.graceUntil() : gameTime + MONUMENT_PRESSURE_GRACE_TICKS;

        MonumentPressure pressure = new MonumentPressure(
                nextLevel,
                graceUntil,
                gameTime + MONUMENT_PRESSURE_DECAY_TICKS
        );
        MONUMENT_PRESSURE_BY_PLAYER.put(player.getUUID(), pressure);
        return pressure;
    }

    private static void noteLocalMonumentPressureGrace(Level level, Player player) {
        long gameTime = level.getGameTime();
        MonumentPressure previousPressure = MONUMENT_PRESSURE_BY_PLAYER.get(player.getUUID());

        if (previousPressure != null && gameTime <= previousPressure.expiresAt()) {
            return;
        }

        MONUMENT_PRESSURE_BY_PLAYER.put(
                player.getUUID(),
                new MonumentPressure(1, gameTime + MONUMENT_PRESSURE_GRACE_TICKS, gameTime + MONUMENT_PRESSURE_DECAY_TICKS)
        );
    }

    private static boolean isInMonumentPressureGrace(Level level, Player player) {
        long gameTime = level.getGameTime();
        MonumentPressure pressure = MONUMENT_PRESSURE_BY_PLAYER.get(player.getUUID());

        if (pressure == null) {
            return false;
        }

        if (gameTime > pressure.expiresAt()) {
            MONUMENT_PRESSURE_BY_PLAYER.remove(player.getUUID());
            return false;
        }

        return gameTime <= pressure.graceUntil();
    }

    private static boolean canPlayMonumentPressureWarning(Level level, Player player) {
        long gameTime = level.getGameTime();
        Long lastWarningTick = LAST_MONUMENT_PRESSURE_WARNING_TICK_BY_PLAYER.get(player.getUUID());

        return lastWarningTick == null
                || gameTime - lastWarningTick >= MONUMENT_PRESSURE_WARNING_COOLDOWN_TICKS;
    }

    private static void markMonumentPressureWarningPlayed(Level level, Player player) {
        LAST_MONUMENT_PRESSURE_WARNING_TICK_BY_PLAYER.put(player.getUUID(), level.getGameTime());
    }

    private static boolean isPlayerPressuredByGuardianBeam(Level level, Player player) {
        AABB pressureBounds = AABB.ofSize(
                player.position(),
                GUARDIAN_BEAM_EFFECTIVE_RANGE * 2.0D,
                GUARDIAN_BEAM_EFFECTIVE_RANGE * 2.0D,
                GUARDIAN_BEAM_EFFECTIVE_RANGE * 2.0D
        );
        List<Guardian> guardians = level.getEntitiesOfClass(
                Guardian.class,
                pressureBounds,
                guardian -> guardian.isAlive()
                        && isGuardianPressuringPlayer(guardian, player)
                        && guardian.hasLineOfSight(player)
        );

        return !guardians.isEmpty();
    }

    private static boolean isGuardianPressuringPlayer(Guardian guardian, Player player) {
        return guardian.getTarget() == player
                || guardian.hasActiveAttackTarget() && guardian.getActiveAttackTarget() == player;
    }

    private static void interruptMiningWithGuardianBeam(Level level, BlockPos pos, Player player) {
        player.resetAttackStrengthTicker();

        if (!shouldPlayMiningBlockedFeedback(level, player)) {
            return;
        }

        player.swing(InteractionHand.MAIN_HAND);

        if (level instanceof ServerLevel serverLevel) {
            playMiningBlockedFeedback(serverLevel, pos);
        }
    }

    private static boolean shouldPlayMiningBlockedFeedback(Level level, Player player) {
        long gameTime = level.getGameTime();
        Long lastFeedbackTick = LAST_MINING_BLOCKED_FEEDBACK_TICK_BY_PLAYER.get(player.getUUID());

        if (lastFeedbackTick != null && gameTime - lastFeedbackTick < MINING_BLOCKED_FEEDBACK_COOLDOWN_TICKS) {
            return false;
        }

        LAST_MINING_BLOCKED_FEEDBACK_TICK_BY_PLAYER.put(player.getUUID(), gameTime);
        return true;
    }

    private static void playMiningBlockedFeedback(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.HOSTILE, 0.7F, 1.6F);
        level.playSound(null, pos, SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 0.55F, 1.45F);

        level.sendParticles(
                ParticleTypes.SPLASH,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                MINING_BLOCKED_SPLASH_PARTICLES,
                0.25D,
                0.25D,
                0.25D,
                0.04D
        );
        level.sendParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                MINING_BLOCKED_ENCHANT_PARTICLES,
                0.35D,
                0.35D,
                0.35D,
                0.18D
        );
    }

    private static void playMonumentPressureWarning(
            ServerLevel level,
            BlockPos pos,
            Player player,
            MonumentPressure pressure
    ) {
        float volume = 0.55F + pressure.level() * 0.12F;
        float pitch = 1.25F - pressure.level() * 0.1F;
        int splashParticles = MONUMENT_PRESSURE_WARNING_SPLASH_PARTICLES * pressure.level();
        int bubbleParticles = MONUMENT_PRESSURE_WARNING_BUBBLE_PARTICLES * pressure.level();
        int enchantParticles = MONUMENT_PRESSURE_WARNING_ENCHANT_PARTICLES * pressure.level();

        level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.HOSTILE, volume, pitch);
        level.playSound(null, player.blockPosition(), SoundEvents.GUARDIAN_AMBIENT, SoundSource.HOSTILE, volume, pitch);

        Vec3 playerCenter = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(
                ParticleTypes.SPLASH,
                playerCenter.x,
                playerCenter.y,
                playerCenter.z,
                splashParticles,
                0.55D,
                0.35D,
                0.55D,
                0.08D
        );
        level.sendParticles(
                ParticleTypes.BUBBLE,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                bubbleParticles,
                0.35D,
                0.35D,
                0.35D,
                0.04D
        );
        level.sendParticles(
                ParticleTypes.ENCHANT,
                playerCenter.x,
                playerCenter.y,
                playerCenter.z,
                enchantParticles,
                0.75D,
                0.45D,
                0.75D,
                0.12D
        );
    }

    private record MonumentPressure(int level, long graceUntil, long expiresAt) {
    }
}
