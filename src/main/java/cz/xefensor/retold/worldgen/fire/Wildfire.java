package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import cz.xefensor.retold.registry.RetoldBlocks;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stage 2 Nether roaming miniboss combining the original Hovering Inferno encounter
 * pitch with its later Minecraft Dungeons shield and shockwave identity.
 */
public class Wildfire extends Blaze {
    public static final int MAX_SHIELDS = 4;
    static final float SHIELD_DURABILITY = 12.0F;
    static final int SHIELD_BREAK_INVULNERABILITY_TICKS = 12;
    static final int FIRST_SHIELD_REGEN_TICKS = 200;
    static final int LATER_SHIELD_REGEN_TICKS = 160;
    static final int NATURAL_SPAWN_EXCLUSION_RADIUS = 128;
    private static final int NATURAL_SPAWN_EXCLUSION_VERTICAL_RADIUS = 64;
    private static final int SHOCKWAVE_COOLDOWN_TICKS = 60;
    private static final double SHOCKWAVE_TRIGGER_RANGE = 5.0D;
    private static final double SHOCKWAVE_RADIUS = 6.0D;
    private static final float SHOCKWAVE_DAMAGE = 10.0F;
    private static final double SHOCKWAVE_PUSH = 1.6D;
    private static final int LAVA_SEARCH_INTERVAL_TICKS = 40;
    private static final int LAVA_SEARCH_HORIZONTAL_RADIUS = 8;
    private static final int LAVA_SEARCH_VERTICAL_RADIUS = 5;
    private static final int LAVA_SEARCH_CACHE_TICKS = 60;
    private static final int RECOVERY_SOURCE_VERTICAL_SCAN = 4;
    private static final int RETREAT_CONTROL_TICKS = 45;
    private static final double RETREAT_SPEED = 1.15D;
    private static final int LAVA_HEAL_INTERVAL_TICKS = 20;
    private static final float LAVA_HEAL_AMOUNT = 6.0F;
    private static final int COMPANION_MIN = 3;
    private static final int COMPANION_RANDOM_EXTRA = 3;
    private static final int FORMATION_MAINTENANCE_INTERVAL_TICKS = 20;
    private static final EntityDataAccessor<Integer> DATA_SHIELD_COUNT =
            SynchedEntityData.defineId(Wildfire.class, EntityDataSerializers.INT);

    private float currentShieldDamage;
    private int shieldBreakInvulnerabilityTicks;
    private int shieldRegenerationTicks;
    private int shockwaveCooldownTicks;
    private int nextLavaSearchTick;
    private int lavaHealthRegenerationTicks;
    private BlockPos lavaRetreatSource;
    private BlockPos lavaRetreatTarget;
    private boolean lavaRecoveryActive;
    private boolean lavaRecoverySubmerged;
    private final List<UUID> blazeCompanionIds = new ArrayList<>();
    private Vec3 patrolDirection = Vec3.ZERO;

    public Wildfire(EntityType<? extends Blaze> type, Level level) {
        super(type, level);
        this.xpReward = 50;
        this.goalSelector.addGoal(3, WildfireMovement.createCombatGoal(this));
        this.goalSelector.addGoal(2, WildfireFormation.createPatrolGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Blaze.createAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    public static boolean checkWildfireSpawnRules(
            EntityType<? extends Blaze> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        ServerLevel serverLevel = level.getLevel();

        if (serverLevel.dimension() != Level.NETHER
                || RetoldWorldData.get(serverLevel).getStage().getId()
                < RetoldWorldStage.STAGE_2.getId()
                || !Monster.checkAnyLightMonsterSpawnRules(
                        type,
                        level,
                        spawnReason,
                        pos,
                        random
                )) {
            return false;
        }

        if (spawnReason != EntitySpawnReason.NATURAL
                && spawnReason != EntitySpawnReason.CHUNK_GENERATION) {
            return true;
        }

        return isNaturalSpawnAreaClear(serverLevel, pos);
    }

    static boolean isNaturalSpawnAreaClear(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(
                NATURAL_SPAWN_EXCLUSION_RADIUS,
                NATURAL_SPAWN_EXCLUSION_VERTICAL_RADIUS,
                NATURAL_SPAWN_EXCLUSION_RADIUS
        );

        return level.getEntitiesOfClass(
                Wildfire.class,
                area,
                wildfire -> wildfire.isAlive() && !wildfire.isRemoved()
        ).isEmpty();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHIELD_COUNT, MAX_SHIELDS);
    }

    public int getShieldCount() {
        return this.entityData.get(DATA_SHIELD_COUNT);
    }

    void setShieldCount(int shieldCount) {
        this.entityData.set(DATA_SHIELD_COUNT, Math.clamp(shieldCount, 0, MAX_SHIELDS));
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);

        if (spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.CHUNK_GENERATION) {
            spawnBlazeCompanions(level, difficulty);
        }

        return result;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
            return;
        }

        if (shieldBreakInvulnerabilityTicks > 0) {
            shieldBreakInvulnerabilityTicks--;
        }

        if (shockwaveCooldownTicks > 0) {
            shockwaveCooldownTicks--;
        }

        beginLavaRecoveryIfNeeded(level);
        boolean protectedByDeepLava = lavaRecoveryActive
                && isAtDeepLavaRecoverySite(level);
        tickShieldRegeneration(level, protectedByDeepLava);
        tickLavaRecovery(level, protectedByDeepLava);
        WildfireMovement.tickLavaMovement(
                this,
                lavaRecoveryActive && protectedByDeepLava,
                lavaRecoverySubmerged
        );
        tickLavaRetreat(level, protectedByDeepLava);
        maintainEscortFormation(level);
        tryShockwave(level);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, damage);
        }

        if (damage > 0.0F && getShieldCount() > 0) {
            if (shieldBreakInvulnerabilityTicks > 0) {
                return false;
            }

            currentShieldDamage += damage;
            this.playSound(SoundEvents.BLAZE_HURT, 1.0F, 0.65F);

            if (currentShieldDamage >= SHIELD_DURABILITY) {
                currentShieldDamage = 0.0F;
                setShieldCount(getShieldCount() - 1);
                shieldBreakInvulnerabilityTicks = SHIELD_BREAK_INVULNERABILITY_TICKS;
                shieldRegenerationTicks = 0;
                level.sendParticles(
                        ParticleTypes.FLAME,
                        this.getX(),
                        this.getY() + 1.2D,
                        this.getZ(),
                        24,
                        0.8D,
                        0.7D,
                        0.8D,
                        0.08D
                );
            }

            return true;
        }

        return super.hurtServer(level, source, damage);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("retold_wildfire_shields", getShieldCount());
        output.putFloat("retold_wildfire_shield_damage", currentShieldDamage);
        output.putBoolean("retold_wildfire_lava_recovery", lavaRecoveryActive);
        output.putBoolean(
                "retold_wildfire_lava_recovery_submerged",
                lavaRecoverySubmerged
        );
        ValueOutput.TypedOutputList<UUID> companionIds = output.list(
                "retold_wildfire_blaze_escorts",
                UUIDUtil.CODEC
        );
        blazeCompanionIds.forEach(companionIds::add);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setShieldCount(input.getIntOr("retold_wildfire_shields", MAX_SHIELDS));
        currentShieldDamage = Math.clamp(
                input.getFloatOr("retold_wildfire_shield_damage", 0.0F),
                0.0F,
                SHIELD_DURABILITY
        );
        lavaRecoveryActive = input.getBooleanOr(
                "retold_wildfire_lava_recovery",
                false
        );
        lavaRecoverySubmerged = input.getBooleanOr(
                "retold_wildfire_lava_recovery_submerged",
                false
        );
        blazeCompanionIds.clear();
        input.listOrEmpty(
                "retold_wildfire_blaze_escorts",
                UUIDUtil.CODEC
        ).forEach(blazeCompanionIds::add);
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level,
            DamageSource source,
            boolean killedByPlayer
    ) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.spawnAtLocation(
                level,
                new ItemStack(RetoldBlocks.NETHER_REACTOR_CORE.get())
        );
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !lavaRecoveryActive
                && lavaRetreatTarget == null
                && super.canAttack(target);
    }

    private void spawnBlazeCompanions(
            ServerLevelAccessor level,
            DifficultyInstance difficulty
    ) {
        int companionCount = COMPANION_MIN + this.random.nextInt(COMPANION_RANDOM_EXTRA);

        for (int index = 0; index < companionCount; index++) {
            Blaze blaze = EntityTypes.BLAZE.create(level.getLevel(), EntitySpawnReason.MOB_SUMMONED);

            if (blaze == null) {
                continue;
            }

            double angle = Math.PI * 2.0D * index / companionCount;
            blaze.setPos(
                    this.getX() + Math.cos(angle) * 2.5D,
                    this.getY(),
                    this.getZ() + Math.sin(angle) * 2.5D
            );
            blaze.finalizeSpawn(
                    level,
                    difficulty,
                    EntitySpawnReason.MOB_SUMMONED,
                    null
            );
            level.addFreshEntityWithPassengers(blaze);
            blazeCompanionIds.add(blaze.getUUID());
            WildfireFormation.installEscort(blaze, this.getUUID());
        }
    }

    private void maintainEscortFormation(ServerLevel level) {
        if (this.tickCount % FORMATION_MAINTENANCE_INTERVAL_TICKS != 0) {
            return;
        }

        for (UUID companionId : blazeCompanionIds) {
            if (level.getEntity(companionId) instanceof Blaze blaze
                    && blaze.isAlive()
                    && !blaze.isRemoved()) {
                WildfireFormation.installEscort(blaze, this.getUUID());
            }
        }
    }

    boolean hasLoadedFormationCompanion(ServerLevel level) {
        for (UUID companionId : blazeCompanionIds) {
            if (level.getEntity(companionId) instanceof Blaze blaze
                    && blaze.isAlive()
                    && !blaze.isRemoved()) {
                return true;
            }
        }

        return false;
    }

    int formationSlot(UUID companionId) {
        return blazeCompanionIds.indexOf(companionId);
    }

    Vec3 formationDirection() {
        if (patrolDirection.horizontalDistanceSqr() < 0.01D) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            patrolDirection = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        return patrolDirection;
    }

    void setFormationDirection(Vec3 direction) {
        Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);

        if (horizontal.horizontalDistanceSqr() >= 0.01D) {
            patrolDirection = horizontal.normalize();
        }
    }

    private void tickShieldRegeneration(
            ServerLevel level,
            boolean protectedByDeepLava
    ) {
        if (getShieldCount() >= MAX_SHIELDS
                || !canRegenerateShields(protectedByDeepLava)) {
            shieldRegenerationTicks = 0;
            return;
        }

        shieldRegenerationTicks++;
        int requiredTicks = getShieldCount() == 0
                ? FIRST_SHIELD_REGEN_TICKS
                : LATER_SHIELD_REGEN_TICKS;

        if (shieldRegenerationTicks < requiredTicks) {
            return;
        }

        setShieldCount(getShieldCount() + 1);
        currentShieldDamage = 0.0F;
        shieldRegenerationTicks = 0;
        this.playSound(SoundEvents.BLAZE_BURN, 1.0F, 0.75F);
        level.sendParticles(
                ParticleTypes.FLAME,
                this.getX(),
                this.getY() + 1.2D,
                this.getZ(),
                18,
                0.65D,
                0.75D,
                0.65D,
                0.04D
        );
    }

    private boolean canRegenerateShields(boolean protectedByDeepLava) {
        return lavaRecoveryActive
                && protectedByDeepLava
                && this.isEyeInFluid(FluidTags.LAVA);
    }

    private void beginLavaRecoveryIfNeeded(ServerLevel level) {
        if (!lavaRecoveryActive
                && WildfireMovement.isTouchingLava(this)
                && this.getHealth() <= this.getMaxHealth() * 0.5F
                && !isFullyRecovered()) {
            BlockPos deepLavaSource = findDeepLavaSourceAtCurrentColumn(level);

            if (deepLavaSource == null) {
                return;
            }

            lavaRetreatSource = deepLavaSource;
            lavaRetreatTarget = deepLavaSource;
            lavaRecoveryActive = true;
            lavaRecoverySubmerged = false;
            lavaHealthRegenerationTicks = 0;
        }
    }

    private void tickLavaRecovery(
            ServerLevel level,
            boolean protectedByDeepLava
    ) {
        if (!lavaRecoveryActive) {
            lavaHealthRegenerationTicks = 0;
            return;
        }

        LivingEntity target = this.getTarget();

        if (target != null) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    this,
                    target,
                    true
            );
        }

        RetoldAiControl.tryClaim(
                this,
                RetoldAiControlMode.SHELTER,
                RetoldAiControlOwner.WILDFIRE_RECOVERY,
                level.getGameTime(),
                RETREAT_CONTROL_TICKS
        );

        lavaRecoverySubmerged = protectedByDeepLava
                && this.isEyeInFluid(FluidTags.LAVA);

        if (protectedByDeepLava
                && this.isEyeInFluid(FluidTags.LAVA)) {
            lavaHealthRegenerationTicks++;

            if (lavaHealthRegenerationTicks >= LAVA_HEAL_INTERVAL_TICKS) {
                this.heal(LAVA_HEAL_AMOUNT);
                lavaHealthRegenerationTicks = 0;
            }
        } else {
            lavaHealthRegenerationTicks = 0;
        }

        if (!isFullyRecovered()) {
            return;
        }

        lavaRecoveryActive = false;
        lavaRecoverySubmerged = false;
        lavaHealthRegenerationTicks = 0;
        clearLavaRetreat();
        this.playSound(SoundEvents.BLAZE_BURN, 1.2F, 1.0F);
        level.sendParticles(
                ParticleTypes.FLAME,
                this.getX(),
                this.getY() + 1.2D,
                this.getZ(),
                32,
                0.8D,
                0.9D,
                0.8D,
                0.08D
        );
    }

    private boolean isFullyRecovered() {
        return getShieldCount() >= MAX_SHIELDS
                && this.getHealth() >= this.getMaxHealth();
    }

    boolean isLavaRecoveryActive() {
        return lavaRecoveryActive;
    }

    boolean isLavaRecoverySubmerged() {
        return lavaRecoverySubmerged;
    }

    void tickLavaRetreat(ServerLevel level) {
        tickLavaRetreat(
                level,
                lavaRecoveryActive && isAtDeepLavaRecoverySite(level)
        );
    }

    private void tickLavaRetreat(
            ServerLevel level,
            boolean protectedByDeepLava
    ) {
        if (lavaRecoveryActive && protectedByDeepLava) {
            return;
        }

        if (!lavaRecoveryActive && (isFullyRecovered()
                || this.getHealth() > this.getMaxHealth() * 0.5F)) {
            clearLavaRetreat();
            return;
        }

        if (lavaRetreatSource != null
                && RetoldBlockTargetSearch.isDeepLavaSourceAt(level, lavaRetreatSource)
                && lavaRetreatTarget != null) {
            continueLavaRetreat(level);
            return;
        }

        lavaRetreatSource = null;
        lavaRetreatTarget = null;

        if (this.tickCount < nextLavaSearchTick) {
            return;
        }

        nextLavaSearchTick = this.tickCount + LAVA_SEARCH_INTERVAL_TICKS;
        BlockPos lava = RetoldBlockTargetSearch.findDeepLavaSource(
                level,
                this,
                LAVA_SEARCH_HORIZONTAL_RADIUS,
                LAVA_SEARCH_VERTICAL_RADIUS,
                level.getGameTime(),
                LAVA_SEARCH_CACHE_TICKS
        );

        if (lava == null) {
            return;
        }

        lavaRetreatSource = lava;
        lavaRetreatTarget = lava;
        continueLavaRetreat(level);
    }

    void continueLavaRetreat(ServerLevel level) {
        if (!RetoldAiControl.isControlledAsBy(
                this,
                RetoldAiControlMode.SHELTER,
                RetoldAiControlOwner.WILDFIRE_RECOVERY
        ) && !RetoldAiControl.tryClaim(
                this,
                RetoldAiControlMode.SHELTER,
                RetoldAiControlOwner.WILDFIRE_RECOVERY,
                level.getGameTime(),
                RETREAT_CONTROL_TICKS
        )) {
            return;
        }

        LivingEntity target = this.getTarget();

        if (target != null) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    this,
                    target,
                    true
            );
        }

        this.getMoveControl().setWantedPosition(
                lavaRetreatTarget.getX() + 0.5D,
                lavaRetreatTarget.getY(),
                lavaRetreatTarget.getZ() + 0.5D,
                RETREAT_SPEED
        );
        WildfireMovement.moveTowardRecoverySource(
                this,
                Vec3.atBottomCenterOf(lavaRetreatTarget),
                RETREAT_SPEED
        );
    }

    boolean hasLavaRetreatTarget(ServerLevel level) {
        return !lavaRecoveryActive
                && lavaRetreatSource != null
                && RetoldBlockTargetSearch.isDeepLavaSourceAt(level, lavaRetreatSource)
                && lavaRetreatTarget != null;
    }

    boolean isRetreatingToLavaSource(BlockPos source) {
        return source != null && source.equals(lavaRetreatSource);
    }

    private boolean isAtDeepLavaRecoverySite(ServerLevel level) {
        if (!WildfireMovement.isTouchingLava(this)) {
            return false;
        }

        BlockPos localSource = findDeepLavaSourceAtCurrentColumn(level);

        if (localSource == null) {
            return false;
        }

        lavaRetreatSource = localSource;
        lavaRetreatTarget = localSource;
        return true;
    }

    private BlockPos findDeepLavaSourceAtCurrentColumn(ServerLevel level) {
        BlockPos current = this.blockPosition();

        for (int offset = RECOVERY_SOURCE_VERTICAL_SCAN; offset >= -1; offset--) {
            BlockPos candidate = current.offset(0, offset, 0);

            if (RetoldBlockTargetSearch.isDeepLavaSourceAt(level, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private void clearLavaRetreat() {
        lavaRetreatSource = null;
        lavaRetreatTarget = null;
        RetoldAiControl.clearIfOwnedBy(this, RetoldAiControlOwner.WILDFIRE_RECOVERY);
    }

    private void tryShockwave(ServerLevel level) {
        LivingEntity target = this.getTarget();

        if (shockwaveCooldownTicks > 0
                || target == null
                || !target.isAlive()
                || this.distanceToSqr(target) > SHOCKWAVE_TRIGGER_RANGE * SHOCKWAVE_TRIGGER_RANGE) {
            return;
        }

        shockwaveCooldownTicks = SHOCKWAVE_COOLDOWN_TICKS;
        for (LivingEntity victim : RetoldAiScanCache.nearby(
                level,
                this,
                LivingEntity.class,
                SHOCKWAVE_RADIUS,
                level.getGameTime(),
                2
        )) {
            if (victim == this
                    || !victim.isAlive()
                    || Math.abs(victim.getY() - this.getY()) > 2.0D
                    || !RetoldFactionRelations.shouldAttack(this, victim)) {
                continue;
            }

            Vec3 away = victim.position().subtract(this.position());
            double horizontalLength = Math.max(0.001D, Math.sqrt(
                    away.x * away.x + away.z * away.z
            ));
            victim.hurtServer(level, level.damageSources().mobAttack(this), SHOCKWAVE_DAMAGE);
            victim.push(
                    away.x / horizontalLength * SHOCKWAVE_PUSH,
                    0.45D,
                    away.z / horizontalLength * SHOCKWAVE_PUSH
            );
        }

        level.sendParticles(
                ParticleTypes.FLAME,
                this.getX(),
                this.getY() + 0.25D,
                this.getZ(),
                48,
                2.3D,
                0.15D,
                2.3D,
                0.03D
        );
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.5F, 0.55F);
    }
}
