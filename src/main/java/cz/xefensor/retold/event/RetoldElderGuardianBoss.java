package cz.xefensor.retold.event;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RetoldElderGuardianBoss {
    private static final double MONUMENT_BOUNDS_INFLATE = 1.0D;

    private static final int DRYING_DAMAGE_INTERVAL_TICKS = 40;
    private static final float DRYING_DAMAGE = 2.0F;

    private static final int BLOCK_FEEDBACK_COOLDOWN_TICKS = 10;
    private static final int BLOCKED_HIT_MINING_FATIGUE_TICKS = 20 * 6;
    private static final int BLOCKED_HIT_MINING_FATIGUE_AMPLIFIER = 2;
    private static final int PROJECTILE_BLOCK_PARTICLES = 24;
    private static final int MELEE_BLOCK_PARTICLES = 28;
    private static final double PROJECTILE_BLOCK_SPREAD = 0.3D;
    private static final double MELEE_BLOCK_SPREAD = 0.4D;

    private static final double MELEE_KNOCKBACK_STRENGTH = 0.85D;
    private static final double MELEE_KNOCKBACK_Y = 0.22D;

    private static final double PROJECTILE_MIN_SPEED_SQR = 0.01D;
    private static final double PROJECTILE_STILL_BOUNCE_STRENGTH = 0.9D;
    private static final double PROJECTILE_STILL_BOUNCE_Y = 0.12D;
    private static final double PROJECTILE_REVERSE_SCALE = -0.75D;
    private static final double PROJECTILE_AWAY_SCALE = 0.45D;
    private static final double PROJECTILE_BOUNCE_Y = 0.12D;
    private static final double PROJECTILE_REPOSITION_WIDTH_SCALE = 0.7D;

    private static final Map<UUID, Long> LAST_BLOCK_FEEDBACK_TICK_BY_GUARDIAN = new HashMap<>();

    private RetoldElderGuardianBoss() {
    }

    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!(event.getEntity() instanceof ElderGuardian elderGuardian)) {
            return;
        }

        handleMonumentGuardianJoin(event, level, elderGuardian);
    }

    static void onElderGuardianTick(ElderGuardian elderGuardian) {
        if (!(elderGuardian.level() instanceof ServerLevel level)) {
            return;
        }

        if (!elderGuardian.isAlive() || elderGuardian.isInWater()) {
            return;
        }

        if (elderGuardian.tickCount % DRYING_DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }

        elderGuardian.hurtServer(level, elderGuardian.damageSources().dryOut(), DRYING_DAMAGE);
        playDryingFeedback(level, elderGuardian);
    }

    static void onLivingDrops(LivingDropsEvent event, ElderGuardian elderGuardian) {
        LAST_BLOCK_FEEDBACK_TICK_BY_GUARDIAN.remove(elderGuardian.getUUID());
        RetoldGuardianMiningPressure.removeGuardianAlert(elderGuardian.getUUID());
        addGuaranteedWaterElementDrop(event, elderGuardian);
    }

    static void onIncomingDamage(LivingIncomingDamageEvent event, ElderGuardian elderGuardian) {
        if (!elderGuardian.isInWater()) {
            return;
        }

        event.setCanceled(true);
        handleBlockedWaterHit(elderGuardian, event.getSource().getEntity(), event.getSource().getDirectEntity());
    }

    public static void onInvulnerableHitAttempt(
            ElderGuardian elderGuardian,
            Entity attacker,
            Entity directEntity
    ) {
        if (elderGuardian == null || !elderGuardian.isInWater()) {
            return;
        }

        handleBlockedWaterHit(
                elderGuardian,
                attacker,
                directEntity
        );
    }

    private static void handleMonumentGuardianJoin(
            EntityJoinLevelEvent event,
            ServerLevel level,
            ElderGuardian elderGuardian
    ) {
        StructureStart monumentStart = RetoldOceanMonumentSupport.findAt(level, elderGuardian);

        if (monumentStart == null || !monumentStart.isValid()) {
            return;
        }

        elderGuardian.setPersistenceRequired();

        if (event.loadedFromDisk()) {
            return;
        }

        AABB monumentBounds = AABB.of(monumentStart.getBoundingBox()).inflate(MONUMENT_BOUNDS_INFLATE);
        List<Entity> existingGuardians = level.getEntities(
                elderGuardian,
                monumentBounds,
                entity -> entity instanceof ElderGuardian
        );

        if (existingGuardians.isEmpty()) {
            return;
        }

        event.setCanceled(true);
    }

    private static void addGuaranteedWaterElementDrop(LivingDropsEvent event, ElderGuardian elderGuardian) {
        if (!(elderGuardian.level() instanceof ServerLevel level)) {
            return;
        }

        boolean alreadyDroppingWaterElement = event.getDrops().stream()
                .anyMatch(drop -> drop.getItem().is(RetoldBlocks.WATER_ELEMENT));

        if (alreadyDroppingWaterElement) {
            return;
        }

        ItemEntity drop = new ItemEntity(
                level,
                elderGuardian.getX(),
                elderGuardian.getY(),
                elderGuardian.getZ(),
                RetoldBlocks.WATER_ELEMENT.toStack()
        );

        drop.setDefaultPickUpDelay();
        event.getDrops().add(drop);
    }

    private static void handleBlockedWaterHit(ElderGuardian elderGuardian, Entity attacker, Entity directEntity) {
        if (!(elderGuardian.level() instanceof ServerLevel level)) {
            return;
        }

        boolean playFeedback = shouldPlayBlockedHitFeedback(level, elderGuardian);
        applyBlockedHitCurse(attacker);

        if (directEntity instanceof Projectile projectile) {
            if (playFeedback) {
                playBlockedHitSounds(level, elderGuardian);
                playWaterBurst(level, projectile.position(), PROJECTILE_BLOCK_PARTICLES, PROJECTILE_BLOCK_SPREAD);
            }

            bounceProjectileAwayFromGuardian(elderGuardian, projectile);
            return;
        }

        Entity knockbackTarget = directEntity != null ? directEntity : attacker;

        if (knockbackTarget == null || knockbackTarget == elderGuardian) {
            return;
        }

        pushAwayFromGuardian(elderGuardian, knockbackTarget);

        if (playFeedback) {
            playBlockedHitSounds(level, elderGuardian);
            playWaterBurst(
                    level,
                    knockbackTarget.position().add(0.0D, knockbackTarget.getBbHeight() * 0.5D, 0.0D),
                    MELEE_BLOCK_PARTICLES,
                    MELEE_BLOCK_SPREAD
            );
        }
    }

    private static void applyBlockedHitCurse(Entity attacker) {
        if (!(attacker instanceof net.minecraft.world.entity.LivingEntity livingEntity)) {
            return;
        }

        livingEntity.addEffect(
                new MobEffectInstance(
                        MobEffects.MINING_FATIGUE,
                        BLOCKED_HIT_MINING_FATIGUE_TICKS,
                        BLOCKED_HIT_MINING_FATIGUE_AMPLIFIER
                )
        );
    }

    private static boolean shouldPlayBlockedHitFeedback(ServerLevel level, ElderGuardian elderGuardian) {
        long gameTime = level.getGameTime();
        Long lastFeedbackTick = LAST_BLOCK_FEEDBACK_TICK_BY_GUARDIAN.get(elderGuardian.getUUID());

        if (lastFeedbackTick != null && gameTime - lastFeedbackTick < BLOCK_FEEDBACK_COOLDOWN_TICKS) {
            return false;
        }

        LAST_BLOCK_FEEDBACK_TICK_BY_GUARDIAN.put(elderGuardian.getUUID(), gameTime);
        return true;
    }

    private static void playBlockedHitSounds(ServerLevel level, ElderGuardian elderGuardian) {
        level.playSound(
                null,
                elderGuardian.blockPosition(),
                SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundSource.HOSTILE,
                0.65F,
                1.25F
        );
        level.playSound(
                null,
                elderGuardian.blockPosition(),
                SoundEvents.BUBBLE_POP,
                SoundSource.HOSTILE,
                0.8F,
                0.65F
        );
    }

    private static void pushAwayFromGuardian(ElderGuardian elderGuardian, Entity target) {
        Vec3 direction = horizontalOrFallback(target.position().subtract(elderGuardian.position()));

        target.push(
                direction.x * MELEE_KNOCKBACK_STRENGTH,
                MELEE_KNOCKBACK_Y,
                direction.z * MELEE_KNOCKBACK_STRENGTH
        );
        target.hurtMarked = true;
    }

    private static void bounceProjectileAwayFromGuardian(ElderGuardian elderGuardian, Projectile projectile) {
        Vec3 away = horizontalOrFallback(projectile.position().subtract(elderGuardian.position()));
        Vec3 velocity = projectile.getDeltaMovement();
        Vec3 bouncedVelocity;

        if (velocity.lengthSqr() < PROJECTILE_MIN_SPEED_SQR) {
            bouncedVelocity = away
                    .scale(PROJECTILE_STILL_BOUNCE_STRENGTH)
                    .add(0.0D, PROJECTILE_STILL_BOUNCE_Y, 0.0D);
        } else {
            bouncedVelocity = velocity
                    .scale(PROJECTILE_REVERSE_SCALE)
                    .add(away.scale(PROJECTILE_AWAY_SCALE))
                    .add(0.0D, PROJECTILE_BOUNCE_Y, 0.0D);
        }

        Vec3 pushedPosition = projectile.position()
                .add(away.scale(elderGuardian.getBbWidth() * PROJECTILE_REPOSITION_WIDTH_SCALE));

        projectile.setPos(pushedPosition);
        projectile.setDeltaMovement(bouncedVelocity);
        projectile.hurtMarked = true;
    }

    private static void playWaterBurst(ServerLevel level, Vec3 center, int count, double spread) {
        level.sendParticles(
                ParticleTypes.SPLASH,
                center.x,
                center.y,
                center.z,
                count,
                spread,
                spread * 0.55D,
                spread,
                0.12D
        );
    }

    private static Vec3 horizontalOrFallback(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);

        if (horizontal.lengthSqr() < 0.0001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return horizontal.normalize();
    }

    private static void playDryingFeedback(ServerLevel level, ElderGuardian elderGuardian) {
        level.playSound(
                null,
                elderGuardian.blockPosition(),
                SoundEvents.ELDER_GUARDIAN_HURT_LAND,
                SoundSource.HOSTILE,
                0.8F,
                0.85F
        );
        level.sendParticles(
                ParticleTypes.WHITE_SMOKE,
                elderGuardian.getX(),
                elderGuardian.getY() + elderGuardian.getBbHeight() * 0.5D,
                elderGuardian.getZ(),
                12,
                elderGuardian.getBbWidth() * 0.35D,
                elderGuardian.getBbHeight() * 0.25D,
                elderGuardian.getBbWidth() * 0.35D,
                0.02D
        );
    }
}
