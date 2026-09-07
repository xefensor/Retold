package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldMobGriefing;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class EarthGuardian extends IronGolem implements VibrationSystem {
    static final int AWAKENING_DURATION_TICKS = 60;
    private static final int VIBRATION_LISTENER_RADIUS = 32;
    private static final int VIBRATION_MEMORY_TICKS = 240;
    private static final int INVESTIGATION_CONTROL_TICKS = 30;
    private static final int PATROL_RETRY_TICKS = 100;
    private static final int TERRAIN_EDIT_INTERVAL_TICKS = 10;
    static final int TERRAIN_EDITS_PER_PULSE = 12;
    private static final int WALL_INTERVAL_TICKS = 40;
    // Covers the full 3-wide ramp work space plus the body-clear trailing space before backfill.
    private static final int TERRAIN_RESERVE_LIMIT = 128;
    private static final int ENVIRONMENTAL_ATTACK_COOLDOWN_TICKS = 80;
    private static final int ENVIRONMENTAL_ATTACK_WINDUP_TICKS = 30;
    private static final int ENVIRONMENTAL_TELEGRAPH_INTERVAL_TICKS = 5;
    private static final int COLLAPSE_MAX_BLOCKS = 5;
    private static final int COLLAPSE_MIN_HEIGHT = 3;
    private static final int COLLAPSE_MAX_HEIGHT = 12;
    private static final int[][] COLLAPSE_OFFSETS = {
            {0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final double ENVIRONMENTAL_ATTACK_RANGE = 24.0D;
    private static final double ENVIRONMENTAL_ATTACK_VERTICAL_RANGE = 6.0D;
    private static final int LAVA_POOL_RADIUS = 1;
    private static final int FLOOR_COLLAPSE_RADIUS = 1;
    private static final int FLOOR_COLLAPSE_DEPTH = 4;
    private static final float COLLAPSE_DAMAGE_PER_DISTANCE = 4.0F;
    private static final int COLLAPSE_MAX_DAMAGE = 18;
    private static final double INVESTIGATION_SPEED = 1.4D;
    private static final double PATROL_SPEED = 0.8D;
    private static final double ACTIVATION_HORIZONTAL_RANGE = 17.0D;
    private static final double ACTIVATION_VERTICAL_RANGE = 12.0D;
    private static final EntityDataAccessor<Integer> LIFECYCLE = SynchedEntityData.defineId(
            EarthGuardian.class,
            EntityDataSerializers.INT
    );

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getUUID(),
            Component.translatable("entity.retold.earth_guardian"),
            BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private final VibrationSystem.User vibrationUser = new GuardianVibrationUser();
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final DynamicGameEventListener<VibrationSystem.Listener> vibrationListener =
            new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
    private BlockPos guardianPosition;
    private BoundingBox labyrinthBounds;
    private final List<BlockState> terrainReserve = new ArrayList<>();
    private final EarthGuardianRoute excavationRoute = new EarthGuardianRoute(this);
    private final EarthGuardianMelee melee = new EarthGuardianMelee(this);
    private long labyrinthKey;
    private boolean structureBound;
    private int awakeningTicks;
    private BlockPos vibrationTarget;
    private UUID vibrationSourcePlayer;
    private int vibrationMemoryTicks;
    private int vibrationStrength;
    private int nextTerrainEditTick;
    private int nextWallTick;
    private int environmentalAttackCooldownTicks;
    private int environmentalAttackWindupTicks;
    private BlockPos environmentalAttackTarget;
    private EnvironmentalAttackType environmentalAttackType;
    private EnvironmentalAttackType nextEnvironmentalAttack = EnvironmentalAttackType.CEILING_COLLAPSE;

    public EarthGuardian(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 0;
        applyLifecycleState();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Sight-based vanilla Golem goals would contradict the confirmed blind guardian design.
        this.goalSelector.addGoal(1, new InvestigateVibrationGoal(this));
        this.goalSelector.addGoal(2, new MazePatrolGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        super.defineSynchedData(entityData);
        entityData.define(LIFECYCLE, EarthGuardianLifecycle.DORMANT.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level() instanceof ServerLevel serverLevel) {
            tickGuardianState(serverLevel);

            if (lifecycle() == EarthGuardianLifecycle.ROAMING) {
                VibrationSystem.Ticker.tick(serverLevel, vibrationData, vibrationUser);
                tickVibrationMemory();
                tickEnvironmentalCombat(serverLevel);
                melee.tick(serverLevel);
                tickTerrainControl(serverLevel);
            }
        }
    }

    void tickGuardianState(ServerLevel level) {
        EarthGuardianLifecycle lifecycle = lifecycle();

        if (structureBound
                && (lifecycle == EarthGuardianLifecycle.DORMANT
                || lifecycle == EarthGuardianLifecycle.AWAKENING)) {
            keepAtGuardianPosition();
        }

        if (lifecycle == EarthGuardianLifecycle.DORMANT
                && structureBound
                && isStageTwo(level)
                && hasActivatingPlayer(level)) {
            transitionTo(level, EarthGuardianLifecycle.AWAKENING);
            awakeningTicks = AWAKENING_DURATION_TICKS;
            return;
        }

        if (lifecycle == EarthGuardianLifecycle.AWAKENING) {
            if (awakeningTicks > 0) {
                awakeningTicks--;
            }

            if (awakeningTicks == 0) {
                transitionTo(level, EarthGuardianLifecycle.ROAMING);
            }
        }
    }

    void configureForLabyrinth(
            ServerLevel level,
            EarthLabyrinthSource source,
            EarthGuardianLifecycle lifecycle
    ) {
        boolean firstBinding = !this.structureBound || this.labyrinthKey != source.key();
        this.guardianPosition = source.guardianPosition();
        this.labyrinthBounds = copyBounds(source.structureBounds());
        this.labyrinthKey = source.key();
        this.structureBound = true;

        // The source scan repeats every 40 ticks, including after entity reload. Only initial
        // placement and the stationary statue states may move the guardian to its chamber.
        if (firstBinding
                || lifecycle == EarthGuardianLifecycle.DORMANT
                || lifecycle == EarthGuardianLifecycle.AWAKENING) {
            keepAtGuardianPosition();
        }

        if (lifecycle() != lifecycle) {
            setLifecycle(lifecycle);
        }

        if (lifecycle == EarthGuardianLifecycle.AWAKENING && awakeningTicks <= 0) {
            awakeningTicks = AWAKENING_DURATION_TICKS;
        }

        EarthGuardianEncounterData.get(level).setLifecycle(labyrinthKey, lifecycle);
    }

    private void transitionTo(ServerLevel level, EarthGuardianLifecycle lifecycle) {
        setLifecycle(lifecycle);

        if (structureBound) {
            EarthGuardianEncounterData.get(level).setLifecycle(labyrinthKey, lifecycle);
        }
    }

    private void setLifecycle(EarthGuardianLifecycle lifecycle) {
        this.entityData.set(LIFECYCLE, lifecycle.ordinal());
        applyLifecycleState();
    }

    public EarthGuardianLifecycle lifecycle() {
        return EarthGuardianLifecycle.bySerializedId(this.entityData.get(LIFECYCLE));
    }

    public boolean isStructureBound() {
        return structureBound;
    }

    public long labyrinthKey() {
        return labyrinthKey;
    }

    public boolean isBossBarVisible() {
        return this.bossEvent.isVisible();
    }

    private void applyLifecycleState() {
        EarthGuardianLifecycle lifecycle = lifecycle();
        boolean dormant = lifecycle == EarthGuardianLifecycle.DORMANT;
        boolean defeated = lifecycle == EarthGuardianLifecycle.DEFEATED;
        boolean roaming = lifecycle == EarthGuardianLifecycle.ROAMING;

        this.setNoAi(!roaming);
        this.setInvulnerable(!roaming && !defeated);
        this.bossEvent.setVisible(!dormant && !defeated);
        this.setTarget(null);

        if (!roaming) {
            this.getNavigation().stop();
            // Client spawn constructs the entity before its network ID exists; AI ownership is server-only.
            if (this.level() instanceof ServerLevel) {
                RetoldAiControl.clearIfOwnedBy(this, RetoldAiControlOwner.EARTH_GUARDIAN);
            }
            this.setDeltaMovement(Vec3.ZERO);
            clearPendingEnvironmentalAttack();
        }
    }

    private void keepAtGuardianPosition() {
        if (guardianPosition == null) {
            return;
        }

        double expectedX = guardianPosition.getX() + 0.5D;
        double expectedY = guardianPosition.getY();
        double expectedZ = guardianPosition.getZ() + 0.5D;

        if (this.distanceToSqr(expectedX, expectedY, expectedZ) > 0.01D) {
            this.setPos(expectedX, expectedY, expectedZ);
        }

        this.setDeltaMovement(Vec3.ZERO);
    }

    private boolean hasActivatingPlayer(ServerLevel level) {
        if (guardianPosition == null) {
            return false;
        }

        AABB activationBounds = new AABB(guardianPosition).inflate(
                ACTIVATION_HORIZONTAL_RANGE,
                ACTIVATION_VERTICAL_RANGE,
                ACTIVATION_HORIZONTAL_RANGE
        );
        return level.players().stream().anyMatch(player ->
                EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                        && activationBounds.intersects(player.getBoundingBox())
        );
    }

    private static boolean isStageTwo(ServerLevel level) {
        return RetoldWorldData.get(level).getStage().getId()
                >= RetoldWorldStage.STAGE_2.getId();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);

        if (spawnReason == EntitySpawnReason.SPAWN_ITEM_USE
                || spawnReason == EntitySpawnReason.COMMAND) {
            this.structureBound = false;
            setLifecycle(EarthGuardianLifecycle.ROAMING);
        }

        return result;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (structureBound) {
            if (lifecycle() != EarthGuardianLifecycle.ROAMING) {
                return false;
            }

            if (source.getEntity() instanceof ServerPlayer player
                    && isValidVibrationPlayer(player)) {
                rememberVibration(
                        player.blockPosition(),
                        GameEvent.ENTITY_DAMAGE,
                        player,
                        0.0F
                );
            }
        }

        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!melee.canAttack(level, target)) {
            return false;
        }
        melee.recordAttack();
        return super.doHurtTarget(level, target);
    }

    @Override
    public void die(DamageSource source) {
        if (structureBound && this.level() instanceof ServerLevel level) {
            transitionTo(level, EarthGuardianLifecycle.DEFEATED);
        }

        super.die(source);
    }

    @Override
    public boolean isPushable() {
        return !structureBound && super.isPushable();
    }

    @Override
    protected void doPush(Entity entity) {
        if (structureBound
                && lifecycle() == EarthGuardianLifecycle.ROAMING
                && entity instanceof ServerPlayer player
                && isValidVibrationPlayer(player)) {
            rememberVibration(
                    player.blockPosition(),
                    GameEvent.HIT_GROUND,
                    player,
                    0.0F
            );
        } else if (!structureBound) {
            super.doPush(entity);
        }
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return !structureBound && super.canUsePortal(ignorePassenger);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
        RetoldAiControl.clear(this);
    }

    @Override
    public void updateDynamicGameEventListener(
            BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer
    ) {
        if (this.level() instanceof ServerLevel serverLevel) {
            consumer.accept(vibrationListener, serverLevel);
        }
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return vibrationUser;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("retold_lifecycle", lifecycle().ordinal());
        output.putBoolean("retold_structure_bound", structureBound);
        output.putLong("retold_labyrinth_key", labyrinthKey);
        output.putInt("retold_awakening_ticks", awakeningTicks);
        output.putInt("retold_vibration_memory_ticks", vibrationMemoryTicks);
        output.putInt("retold_vibration_strength", vibrationStrength);
        output.putInt("retold_environmental_attack_cooldown", environmentalAttackCooldownTicks);
        output.putInt("retold_environmental_attack_windup", environmentalAttackWindupTicks);
        output.putInt("retold_next_environmental_attack", nextEnvironmentalAttack.ordinal());
        output.store("retold_vibration_listener", VibrationSystem.Data.CODEC, vibrationData);
        ValueOutput.TypedOutputList<BlockState> reserve = output.list(
                "retold_terrain_reserve",
                BlockState.CODEC
        );
        terrainReserve.forEach(reserve::add);
        excavationRoute.save(output);
        melee.save(output);

        if (guardianPosition != null) {
            output.store("retold_guardian_position", BlockPos.CODEC, guardianPosition);
        }
        if (vibrationTarget != null) {
            output.store("retold_vibration_target", BlockPos.CODEC, vibrationTarget);
        }
        if (vibrationSourcePlayer != null) {
            output.putString("retold_vibration_source_player", vibrationSourcePlayer.toString());
        }
        if (environmentalAttackTarget != null && environmentalAttackType != null) {
            output.store(
                    "retold_environmental_attack_target",
                    BlockPos.CODEC,
                    environmentalAttackTarget
            );
            output.putInt("retold_environmental_attack_type", environmentalAttackType.ordinal());
        }
        if (labyrinthBounds != null) {
            output.putInt("retold_labyrinth_min_x", labyrinthBounds.minX());
            output.putInt("retold_labyrinth_min_y", labyrinthBounds.minY());
            output.putInt("retold_labyrinth_min_z", labyrinthBounds.minZ());
            output.putInt("retold_labyrinth_max_x", labyrinthBounds.maxX());
            output.putInt("retold_labyrinth_max_y", labyrinthBounds.maxY());
            output.putInt("retold_labyrinth_max_z", labyrinthBounds.maxZ());
            output.putBoolean("retold_has_labyrinth_bounds", true);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        melee.load(input);
        this.structureBound = input.getBooleanOr("retold_structure_bound", false);
        this.labyrinthKey = input.getLongOr("retold_labyrinth_key", 0L);
        this.awakeningTicks = input.getIntOr("retold_awakening_ticks", 0);
        this.vibrationMemoryTicks = input.getIntOr("retold_vibration_memory_ticks", 0);
        this.vibrationStrength = input.getIntOr("retold_vibration_strength", 0);
        this.environmentalAttackCooldownTicks = Math.max(
                0,
                input.getIntOr("retold_environmental_attack_cooldown", 0)
        );
        this.environmentalAttackWindupTicks = Math.max(
                0,
                input.getIntOr("retold_environmental_attack_windup", 0)
        );
        // Preserve the next attack from saves made before the three-hazard rotation existed.
        EnvironmentalAttackType restoredNext = EnvironmentalAttackType.bySerializedId(input.getIntOr(
                "retold_next_environmental_attack",
                input.getBooleanOr("retold_prefer_lava_attack", false) ? 1 : 0
        ));
        this.nextEnvironmentalAttack = restoredNext == null
                ? EnvironmentalAttackType.CEILING_COLLAPSE : restoredNext;
        this.vibrationData = input.read(
                "retold_vibration_listener",
                VibrationSystem.Data.CODEC
        ).orElseGet(VibrationSystem.Data::new);
        this.guardianPosition = input.read("retold_guardian_position", BlockPos.CODEC)
                .orElse(null);
        this.vibrationTarget = input.read("retold_vibration_target", BlockPos.CODEC)
                .orElse(null);
        this.vibrationSourcePlayer = parseUuid(
                input.getStringOr("retold_vibration_source_player", "")
        );
        this.environmentalAttackTarget = input.read(
                "retold_environmental_attack_target",
                BlockPos.CODEC
        )
                .orElse(null);
        this.environmentalAttackType = EnvironmentalAttackType.bySerializedId(
                input.getIntOr("retold_environmental_attack_type", -1)
        );
        this.labyrinthBounds = input.getBooleanOr("retold_has_labyrinth_bounds", false)
                ? new BoundingBox(
                input.getIntOr("retold_labyrinth_min_x", 0),
                input.getIntOr("retold_labyrinth_min_y", 0),
                input.getIntOr("retold_labyrinth_min_z", 0),
                input.getIntOr("retold_labyrinth_max_x", 0),
                input.getIntOr("retold_labyrinth_max_y", 0),
                input.getIntOr("retold_labyrinth_max_z", 0)
                )
                : null;
        this.terrainReserve.clear();
        excavationRoute.load(input);
        input.listOrEmpty("retold_terrain_reserve", BlockState.CODEC).forEach(state -> {
            if (terrainReserve.size() < TERRAIN_RESERVE_LIMIT && !state.isAir()) {
                terrainReserve.add(state);
            }
        });
        setLifecycle(EarthGuardianLifecycle.bySerializedId(
                input.getIntOr("retold_lifecycle", EarthGuardianLifecycle.DORMANT.ordinal())
        ));

        if (environmentalAttackTarget == null || environmentalAttackType == null) {
            clearPendingEnvironmentalAttack();
        }
    }

    @Nullable BlockPos vibrationTarget() {
        return vibrationTarget;
    }

    int vibrationStrength() {
        return vibrationStrength;
    }

    @Nullable UUID vibrationSourcePlayer() {
        return vibrationSourcePlayer;
    }

    private boolean hasReachedVibrationClue() {
        return vibrationTarget != null
                && this.position().distanceToSqr(Vec3.atBottomCenterOf(vibrationTarget)) <= 1.44D;
    }

    private void tickVibrationMemory() {
        if (vibrationMemoryTicks <= 0) {
            clearVibrationMemory();
            return;
        }

        vibrationMemoryTicks--;
    }

    private void clearVibrationMemory() {
        vibrationTarget = null;
        vibrationSourcePlayer = null;
        vibrationMemoryTicks = 0;
        vibrationStrength = 0;
        excavationRoute.clear();
    }

    void tickEnvironmentalCombat(ServerLevel level) {
        if (environmentalAttackCooldownTicks > 0) {
            environmentalAttackCooldownTicks--;
        }

        if (isPreparingEnvironmentalAttack()) {
            emitEnvironmentalTelegraph(level);
            environmentalAttackWindupTicks--;

            if (environmentalAttackWindupTicks == 0) {
                releaseEnvironmentalAttack(level);
            }
            return;
        }

        tryStartEnvironmentalAttack(level);
    }

    boolean isPreparingEnvironmentalAttack() {
        return environmentalAttackTarget != null
                && environmentalAttackType != null
                && environmentalAttackWindupTicks > 0;
    }

    private void tryStartEnvironmentalAttack(ServerLevel level) {
        if (environmentalAttackCooldownTicks > 0 || vibrationTarget == null) {
            return;
        }

        BlockPos attackFloor = findAttackFloor(level, vibrationTarget);

        if (attackFloor == null
                || horizontalDistanceSqr(this.position(), Vec3.atBottomCenterOf(attackFloor))
                > ENVIRONMENTAL_ATTACK_RANGE * ENVIRONMENTAL_ATTACK_RANGE
                || Math.abs(attackFloor.getY() - this.getY())
                > ENVIRONMENTAL_ATTACK_VERTICAL_RANGE) {
            return;
        }

        EnvironmentalAttackType attackType = nextEnvironmentalAttack;
        if (attackType == EnvironmentalAttackType.CEILING_COLLAPSE
                && !hasCollapsibleCeilingBlock(level, attackFloor)) {
            attackType = EnvironmentalAttackType.LAVA_ERUPTION;
        }

        environmentalAttackTarget = attackFloor.immutable();
        environmentalAttackType = attackType;
        environmentalAttackWindupTicks = ENVIRONMENTAL_ATTACK_WINDUP_TICKS;
        environmentalAttackCooldownTicks = ENVIRONMENTAL_ATTACK_COOLDOWN_TICKS;
        nextEnvironmentalAttack = switch (attackType) {
            case CEILING_COLLAPSE -> EnvironmentalAttackType.LAVA_ERUPTION;
            case LAVA_ERUPTION -> EnvironmentalAttackType.FLOOR_COLLAPSE;
            case FLOOR_COLLAPSE -> EnvironmentalAttackType.CEILING_COLLAPSE;
        };
        emitEnvironmentalTelegraph(level);
        playCombatSound(
                level,
                attackType == EnvironmentalAttackType.LAVA_ERUPTION
                        ? SoundEvents.LAVA_AMBIENT
                        : SoundEvents.DRIPSTONE_BLOCK_FALL,
                1.4F,
                attackType == EnvironmentalAttackType.CEILING_COLLAPSE ? 0.55F : 0.8F
        );
    }

    private static @Nullable BlockPos findAttackFloor(ServerLevel level, BlockPos target) {
        BlockPos.MutableBlockPos candidate = target.mutable();

        for (int offset = 0; offset <= 4; offset++) {
            candidate.set(target.getX(), target.getY() - offset, target.getZ());
            if (level.getBlockState(candidate).isAir()
                    && !level.getBlockState(candidate.below()).isAir()) {
                return candidate.immutable();
            }
        }

        return null;
    }

    private void emitEnvironmentalTelegraph(ServerLevel level) {
        if (environmentalAttackTarget == null
                || environmentalAttackType == null
                || environmentalAttackWindupTicks % ENVIRONMENTAL_TELEGRAPH_INTERVAL_TICKS != 0) {
            return;
        }

        Vec3 target = Vec3.atBottomCenterOf(environmentalAttackTarget);
        boolean collapse = environmentalAttackType == EnvironmentalAttackType.CEILING_COLLAPSE;
        boolean lava = environmentalAttackType == EnvironmentalAttackType.LAVA_ERUPTION;
        sendParticlesToConnectedPlayers(
                level,
                lava ? ParticleTypes.SMOKE : ParticleTypes.DUST_PLUME,
                target.x,
                target.y + (collapse ? 0.15D : 0.4D),
                target.z,
                collapse ? 18 : 10,
                collapse ? 1.5D : 0.8D,
                collapse ? 0.15D : 0.35D,
                collapse ? 1.5D : 0.8D,
                collapse ? 0.04D : 0.02D
        );
        if (collapse) {
            emitCeilingWarning(level, environmentalAttackTarget);
        }
    }

    private void emitCeilingWarning(ServerLevel level, BlockPos target) {
        for (int[] offset : COLLAPSE_OFFSETS) {
            BlockPos ceiling = findCollapsibleCeilingBlock(
                    level,
                    target,
                    offset[0],
                    offset[1]
            );
            if (ceiling == null) {
                continue;
            }

            Vec3 warning = Vec3.atCenterOf(ceiling).add(0.0D, -0.45D, 0.0D);
            sendParticlesToConnectedPlayers(
                    level,
                    ParticleTypes.DUST_PLUME,
                    warning.x,
                    warning.y,
                    warning.z,
                    3,
                    0.2D,
                    0.05D,
                    0.2D,
                    0.01D
            );
        }
    }

    private void releaseEnvironmentalAttack(ServerLevel level) {
        if (environmentalAttackTarget == null || environmentalAttackType == null) {
            clearPendingEnvironmentalAttack();
            return;
        }

        switch (environmentalAttackType) {
            case CEILING_COLLAPSE -> releaseCeilingCollapse(level, environmentalAttackTarget);
            case LAVA_ERUPTION -> releaseLavaEruption(level, environmentalAttackTarget);
            case FLOOR_COLLAPSE -> releaseFloorCollapse(level, environmentalAttackTarget);
        }
        clearPendingEnvironmentalAttack();
    }

    private void releaseCeilingCollapse(ServerLevel level, BlockPos target) {
        int collapsedBlocks = 0;

        for (int[] offset : COLLAPSE_OFFSETS) {
            BlockPos ceiling = findCollapsibleCeilingBlock(
                    level,
                    target,
                    offset[0],
                    offset[1]
            );
            if (ceiling == null) {
                continue;
            }

            BlockState state = level.getBlockState(ceiling);
            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(level, ceiling, state);
            fallingBlock.setHurtsEntities(COLLAPSE_DAMAGE_PER_DISTANCE, COLLAPSE_MAX_DAMAGE);
            collapsedBlocks++;

            if (collapsedBlocks >= COLLAPSE_MAX_BLOCKS) {
                break;
            }
        }

        if (collapsedBlocks == 0) {
            releaseLavaEruption(level, target);
            return;
        }

        Vec3 center = Vec3.atCenterOf(target);
        sendParticlesToConnectedPlayers(
                level,
                ParticleTypes.DUST_PLUME,
                center.x,
                center.y,
                center.z,
                55,
                1.8D,
                0.25D,
                1.8D,
                0.1D
        );
        playCombatSound(level, SoundEvents.DRIPSTONE_BLOCK_FALL, 1.8F, 0.45F);
    }

    private boolean hasCollapsibleCeilingBlock(ServerLevel level, BlockPos target) {
        for (int[] offset : COLLAPSE_OFFSETS) {
            if (findCollapsibleCeilingBlock(
                    level,
                    target,
                    offset[0],
                    offset[1]
            ) != null) {
                return true;
            }
        }

        return false;
    }

    private @Nullable BlockPos findCollapsibleCeilingBlock(
            ServerLevel level,
            BlockPos target,
            int offsetX,
            int offsetZ
    ) {
        for (int height = COLLAPSE_MIN_HEIGHT; height <= COLLAPSE_MAX_HEIGHT; height++) {
            BlockPos candidate = target.offset(offsetX, height, offsetZ);
            BlockState state = level.getBlockState(candidate);

            if (!state.isAir()
                    && state.getFluidState().isEmpty()
                    && hasClearFallPath(level, target, candidate, offsetX, offsetZ)
                    && RetoldMobGriefing.canBreakBlock(level, this, candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private static boolean hasClearFallPath(
            ServerLevel level,
            BlockPos target,
            BlockPos ceiling,
            int offsetX,
            int offsetZ
    ) {
        BlockPos landing = target.offset(offsetX, 0, offsetZ);

        for (int y = landing.getY(); y < ceiling.getY(); y++) {
            if (!level.getBlockState(new BlockPos(landing.getX(), y, landing.getZ())).isAir()) {
                return false;
            }
        }

        return true;
    }

    private void releaseLavaEruption(ServerLevel level, BlockPos target) {
        // Replace the supporting layer at the locked clue, then let vanilla lava own flow and damage.
        // No palette/hardness restriction: the guardian's unrestricted earth control also applies here.
        BlockState lava = Blocks.LAVA.defaultBlockState();
        for (int x = -LAVA_POOL_RADIUS; x <= LAVA_POOL_RADIUS; x++) {
            for (int z = -LAVA_POOL_RADIUS; z <= LAVA_POOL_RADIUS; z++) {
                BlockPos ground = target.offset(x, -1, z);
                if (!canEditTerrain(level, ground)) {
                    continue;
                }

                BlockState state = level.getBlockState(ground);
                if (state.isAir() || state.equals(lava)
                        || !RetoldMobGriefing.canBreakBlock(level, this, ground)
                        || !RetoldMobGriefing.canPlaceBlock(level, this, ground)) {
                    continue;
                }

                if (level.setBlock(ground, lava, Block.UPDATE_ALL)) {
                    level.gameEvent(this, GameEvent.BLOCK_CHANGE, ground);
                }
            }
        }
    }

    private void releaseFloorCollapse(ServerLevel level, BlockPos target) {
        // Top-down removal opens a real pit; vanilla gravity and the landing surface decide damage.
        // Stop a column at denied terrain so a protected floor is not hollowed out from underneath.
        for (int x = -FLOOR_COLLAPSE_RADIUS; x <= FLOOR_COLLAPSE_RADIUS; x++) {
            for (int z = -FLOOR_COLLAPSE_RADIUS; z <= FLOOR_COLLAPSE_RADIUS; z++) {
                for (int depth = 1; depth <= FLOOR_COLLAPSE_DEPTH; depth++) {
                    BlockPos ground = target.offset(x, -depth, z);
                    if (!canEditTerrain(level, ground)) {
                        break;
                    }
                    if (level.getBlockState(ground).isAir()) {
                        continue;
                    }
                    if (!RetoldMobGriefing.canBreakBlock(level, this, ground)
                            || !level.destroyBlock(ground, false, this)) {
                        break;
                    }
                }
            }
        }
    }

    private static <T extends ParticleOptions> void sendParticlesToConnectedPlayers(
            ServerLevel level,
            T particles,
            double x,
            double y,
            double z,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        for (ServerPlayer player : level.players()) {
            if (player.connection != null) {
                level.sendParticles(
                        player,
                        particles,
                        false,
                        false,
                        x,
                        y,
                        z,
                        count,
                        offsetX,
                        offsetY,
                        offsetZ,
                        speed
                );
            }
        }
    }

    private void playCombatSound(
            ServerLevel level,
            net.minecraft.sounds.SoundEvent sound,
            float volume,
            float pitch
    ) {
        // GameTest players deliberately have no connection; normal play never suppresses this path.
        if (level.players().stream().allMatch(player -> player.connection != null)) {
            this.playSound(sound, volume, pitch);
        }
    }

    private void clearPendingEnvironmentalAttack() {
        environmentalAttackTarget = null;
        environmentalAttackType = null;
        environmentalAttackWindupTicks = 0;
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double deltaX = first.x - second.x;
        double deltaZ = first.z - second.z;
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    void tickTerrainControl(ServerLevel level) {
        if (vibrationTarget == null
                || hasReachedVibrationClue()
                || isPreparingEnvironmentalAttack()
                || !canOwnMovement()
                || tickCount < nextTerrainEditTick) {
            return;
        }

        nextTerrainEditTick = tickCount + TERRAIN_EDIT_INTERVAL_TICKS;
        boolean needsRoute = excavationRoute.active() || this.getNavigation().isDone();
        Direction huntDirection = horizontalDirectionToward(vibrationTarget);
        int edits = 0;
        while (edits < TERRAIN_EDITS_PER_PULSE) {
            int reserveBefore = terrainReserve.size();
            if (needsRoute) {
                excavationRoute.tick(level, vibrationTarget);
            } else if (huntDirection != null) {
                if (isTerrainReserveFull()) {
                    placeBarrierBehind(level, huntDirection);
                } else {
                    excavateToward(level, huntDirection);
                }
            }
            // Each primitive either stores or consumes exactly one state. No progress means
            // ready, denied, unloaded, or out of material: do not repeat a failed scan 12 times.
            if (terrainReserve.size() == reserveBefore) {
                break;
            }
            edits++;
        }
        if (tickCount >= nextWallTick && TERRAIN_EDITS_PER_PULSE - edits >= EarthGuardianWalls.WALL_BLOCKS
                && (!needsRoute || excavationRoute.destination() != null)) {
            Direction wallDirection = excavationRoute.active() ? excavationRoute.direction() : huntDirection;
            if (wallDirection != null) {
                nextWallTick = tickCount + WALL_INTERVAL_TICKS;
                EarthGuardianWalls.buildBehind(level, this, wallDirection);
            }
        }
    }

    private boolean excavateToward(ServerLevel level, Direction direction) {
        Direction side = direction.getClockWise();
        BlockPos origin = this.blockPosition();
        int[] lateralOffsets = {0, -1, 1};

        for (int distance = 1; distance <= 3; distance++) {
            for (int height = 0; height < Math.ceil(getBbHeight()); height++) {
                for (int lateral : lateralOffsets) {
                    BlockPos candidate = origin.relative(direction, distance)
                            .relative(side, lateral)
                            .above(height);

                    if (tryExcavateBlock(level, candidate)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean placeBarrierBehind(ServerLevel level, Direction huntDirection) {
        Direction behind = huntDirection.getOpposite();
        Direction side = huntDirection.getClockWise();
        BlockPos origin = this.blockPosition();
        int[] lateralOffsets = {-1, 1, 0, -2, 2};

        for (int distance = 2; distance <= 8; distance++) {
            // A ramp leaves air below our new feet as well as above the old head height.
            // Reuse that full trailing corridor so a full reserve does not deadlock excavation.
            for (int height = -2; height <= 12; height++) {
                for (int lateral : lateralOffsets) {
                    BlockPos candidate = origin.relative(behind, distance)
                            .relative(side, lateral)
                            .above(height);

                    if (!isActiveRoutePosition(candidate) && tryPlaceReservedBlock(level, candidate)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    boolean tryExcavateBlock(ServerLevel level, BlockPos pos) {
        if (!canEditTerrain(level, pos)
                || terrainReserve.size() >= TERRAIN_RESERVE_LIMIT) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        if (state.isAir()
                || !RetoldMobGriefing.canBreakBlock(level, this, pos)) {
            return false;
        }

        // Vanilla destruction leaves the contained fluid behind; destroying a lava source
        // therefore does nothing. Relocate the entire state, including waterlogged contents,
        // so the guardian can rebuild a walking surface across its own eruption pools.
        boolean removed = state.getFluidState().isEmpty()
                ? level.destroyBlock(pos, false, this)
                : level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!removed) {
            return false;
        }

        terrainReserve.add(state);
        level.gameEvent(this, GameEvent.BLOCK_DESTROY, pos);
        return true;
    }

    boolean tryPlaceReservedBlock(ServerLevel level, BlockPos pos) {
        if (terrainReserve.isEmpty()
                || !canEditTerrain(level, pos)
                || !level.getBlockState(pos).isAir()
                || !level.getEntities(
                (Entity) null,
                new AABB(pos),
                Entity::isAlive
        ).isEmpty()
                || !RetoldMobGriefing.canPlaceBlock(level, this, pos)) {
            return false;
        }

        BlockState state = terrainReserve.getLast();

        if (!level.setBlock(pos, state, Block.UPDATE_ALL)) {
            return false;
        }

        terrainReserve.removeLast();
        level.gameEvent(this, GameEvent.BLOCK_PLACE, pos);
        return true;
    }

    int terrainReserveSize() {
        return terrainReserve.size();
    }

    boolean isActiveRoutePosition(BlockPos pos) {
        return excavationRoute.reserves(pos);
    }

    boolean isTerrainReserveFull() {
        return terrainReserve.size() >= TERRAIN_RESERVE_LIMIT;
    }

    String routeDescription() {
        return excavationRoute.describe();
    }

    boolean tryPlaceRouteSupport(ServerLevel level, BlockPos pos) {
        for (int index = terrainReserve.size() - 1; index >= 0; index--) {
            BlockState state = terrainReserve.get(index);
            if (!state.isCollisionShapeFullBlock(level, pos)) {
                continue;
            }
            // Reuse the normal loaded/protection/occupancy transaction without losing other states.
            int last = terrainReserve.size() - 1;
            java.util.Collections.swap(terrainReserve, index, last);
            if (tryPlaceReservedBlock(level, pos)) {
                return true;
            }
            java.util.Collections.swap(terrainReserve, index, last);
            return false;
        }
        return false;
    }

    boolean hasRouteSupport(ServerLevel level, BlockPos pos) {
        return terrainReserve.stream().anyMatch(state -> state.isCollisionShapeFullBlock(level, pos));
    }

    private boolean canEditTerrain(ServerLevel level, BlockPos pos) {
        return lifecycle() == EarthGuardianLifecycle.ROAMING
                && level == this.level()
                && level.hasChunkAt(pos);
    }

    @Nullable
    private Direction horizontalDirectionToward(BlockPos target) {
        int deltaX = target.getX() - this.blockPosition().getX();
        int deltaZ = target.getZ() - this.blockPosition().getZ();

        if (deltaX == 0 && deltaZ == 0) {
            return null;
        }

        if (Math.abs(deltaX) >= Math.abs(deltaZ)) {
            return deltaX >= 0 ? Direction.EAST : Direction.WEST;
        }

        return deltaZ >= 0 ? Direction.SOUTH : Direction.NORTH;
    }


    private void rememberVibration(
            BlockPos position,
            Holder<GameEvent> event,
            @Nullable ServerPlayer player,
            float distance
    ) {
        if (lifecycle() != EarthGuardianLifecycle.ROAMING) {
            return;
        }

        int score = Math.max(1, vibrationWeight(event) - (int) distance);
        boolean sameSource = player != null
                && player.getUUID().equals(vibrationSourcePlayer);

        if (vibrationTarget != null && !sameSource && score < vibrationStrength) {
            return;
        }

        vibrationTarget = position.immutable();
        vibrationSourcePlayer = player == null ? null : player.getUUID();
        vibrationMemoryTicks = VIBRATION_MEMORY_TICKS;
        vibrationStrength = score;
    }

    private static int vibrationWeight(Holder<GameEvent> event) {
        if (event.is(GameEvent.ENTITY_DAMAGE)) {
            return 100;
        }
        if (event.is(GameEvent.EXPLODE)
                || event.is(GameEvent.PROJECTILE_SHOOT)
                || event.is(GameEvent.PROJECTILE_LAND)) {
            return 85;
        }
        if (event.is(GameEvent.BLOCK_DESTROY)
                || event.is(GameEvent.BLOCK_PLACE)
                || event.is(GameEvent.BLOCK_CHANGE)) {
            return 70;
        }
        if (event.is(GameEvent.CONTAINER_OPEN)
                || event.is(GameEvent.CONTAINER_CLOSE)
                || event.is(GameEvent.BLOCK_ACTIVATE)
                || event.is(GameEvent.BLOCK_DEACTIVATE)) {
            return 60;
        }
        if (event.is(GameEvent.HIT_GROUND)) {
            return 55;
        }
        if (event.is(GameEvent.STEP) || event.is(GameEvent.SWIM)) {
            return 25;
        }
        return 40;
    }

    boolean canOwnMovement() {
        return RetoldAiControl.isControlledBy(this, RetoldAiControlOwner.EARTH_GUARDIAN)
                || RetoldAiControl.getPriority(this) <= RetoldAiPriorities.HUNT;
    }

    private boolean claimMovement(RetoldAiControlMode mode, String reason) {
        return RetoldAiControl.tryClaim(
                this,
                mode,
                RetoldAiControlOwner.EARTH_GUARDIAN,
                RetoldAiPriorities.HUNT,
                reason,
                this.level().getGameTime(),
                INVESTIGATION_CONTROL_TICKS
        );
    }

    private void releaseMovement() {
        this.getNavigation().stop();
        RetoldAiControl.clearIfOwnedBy(this, RetoldAiControlOwner.EARTH_GUARDIAN);
    }

    private boolean isValidVibrationPlayer(Player player) {
        return player.level() == this.level()
                && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                && player.isAlive();
    }

    @Nullable
    private ServerPlayer vibrationPlayer(@Nullable Entity source) {
        Entity candidate = source instanceof TraceableEntity traceableEntity
                ? traceableEntity.getOwner()
                : source;
        return candidate instanceof ServerPlayer player && isValidVibrationPlayer(player)
                ? player
                : null;
    }

    private static BoundingBox copyBounds(BoundingBox bounds) {
        return new BoundingBox(
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.maxX(),
                bounds.maxY(),
                bounds.maxZ()
        );
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private enum EnvironmentalAttackType {
        CEILING_COLLAPSE,
        LAVA_ERUPTION,
        FLOOR_COLLAPSE;

        private static @Nullable EnvironmentalAttackType bySerializedId(int id) {
            return id >= 0 && id < values().length ? values()[id] : null;
        }
    }

    private final class GuardianVibrationUser implements VibrationSystem.User {
        private final PositionSource positionSource = new EntityPositionSource(
                EarthGuardian.this,
                EarthGuardian.this.getEyeHeight()
        );

        @Override
        public int getListenerRadius() {
            return VIBRATION_LISTENER_RADIUS;
        }

        @Override
        public PositionSource getPositionSource() {
            return positionSource;
        }

        @Override
        public net.minecraft.tags.TagKey<GameEvent> getListenableEvents() {
            return GameEventTags.WARDEN_CAN_LISTEN;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(
                ServerLevel level,
                BlockPos position,
                Holder<GameEvent> event,
                GameEvent.Context context
        ) {
            return lifecycle() == EarthGuardianLifecycle.ROAMING
                    && !isDeadOrDying()
                    && vibrationPlayer(context.sourceEntity()) != null;
        }

        @Override
        public void onReceiveVibration(
                ServerLevel level,
                BlockPos position,
                Holder<GameEvent> event,
                @Nullable Entity source,
                @Nullable Entity projectileOwner,
                float distance
        ) {
            ServerPlayer player = vibrationPlayer(
                    projectileOwner == null ? source : projectileOwner
            );

            if (player != null) {
                rememberVibration(position, event, player, distance);
            }
        }
    }

    private static final class InvestigateVibrationGoal extends Goal {
        private final EarthGuardian guardian;
        private int nextPathTick;

        private InvestigateVibrationGoal(EarthGuardian guardian) {
            this.guardian = guardian;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return guardian.lifecycle() == EarthGuardianLifecycle.ROAMING
                    && guardian.vibrationTarget != null
                    && guardian.canOwnMovement();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && guardian.vibrationMemoryTicks > 0;
        }

        @Override
        public void start() {
            nextPathTick = 0;
            followClue();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!guardian.claimMovement(RetoldAiControlMode.HUNT, "earth_vibration")) {
                return;
            }

            BlockPos target = guardian.vibrationTarget;

            if (target == null) {
                return;
            }

            if (guardian.hasReachedVibrationClue()) {
                // Keep the clue for contact combat until its normal expiry or a new sound.
                guardian.getNavigation().stop();
                return;
            }

            // A failed or finished route must still respect the retry interval.
            if (guardian.tickCount >= nextPathTick) {
                followClue();
            }
        }

        @Override
        public void stop() {
            nextPathTick = 0;
            guardian.releaseMovement();
        }

        private void followClue() {
            BlockPos target = guardian.vibrationTarget;

            if (target == null
                    || !guardian.claimMovement(RetoldAiControlMode.HUNT, "earth_vibration")) {
                return;
            }

            if (guardian.excavationRoute.active()) {
                target = guardian.excavationRoute.destination();
                if (target == null) {
                    guardian.getNavigation().stop();
                    nextPathTick = guardian.tickCount + 20;
                    return;
                }
            }

            RetoldBehaviorMovement.throttledMoveToExact(
                    guardian,
                    target,
                    INVESTIGATION_SPEED,
                    guardian.level().getGameTime(),
                    20,
                    1.0D
            );
            nextPathTick = guardian.tickCount + 20;
        }
    }

    private static final class MazePatrolGoal extends Goal {
        private final EarthGuardian guardian;
        private Path path;
        private int nextAttemptTick;

        private MazePatrolGoal(EarthGuardian guardian) {
            this.guardian = guardian;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (guardian.lifecycle() != EarthGuardianLifecycle.ROAMING
                    || guardian.vibrationTarget != null
                    || guardian.isPreparingEnvironmentalAttack()
                    || guardian.tickCount < nextAttemptTick
                    || !guardian.canOwnMovement()) {
                return false;
            }

            path = findPatrolPath();

            if (path == null) {
                nextAttemptTick = guardian.tickCount + PATROL_RETRY_TICKS;
                return false;
            }

            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return guardian.lifecycle() == EarthGuardianLifecycle.ROAMING
                    && guardian.vibrationTarget == null
                    && !guardian.isPreparingEnvironmentalAttack()
                    && path != null
                    && !guardian.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (guardian.claimMovement(RetoldAiControlMode.SEARCH, "earth_patrol")) {
                RetoldAiControl.withNavigationBypass(
                        () -> guardian.getNavigation().moveTo(path, PATROL_SPEED)
                );
            }
        }

        @Override
        public void tick() {
            guardian.claimMovement(RetoldAiControlMode.SEARCH, "earth_patrol");
        }

        @Override
        public void stop() {
            path = null;
            nextAttemptTick = guardian.tickCount + PATROL_RETRY_TICKS;
            guardian.releaseMovement();
        }

        @Nullable
        private Path findPatrolPath() {
            BoundingBox bounds = guardian.labyrinthBounds;
            int minX = bounds == null ? guardian.blockPosition().getX() - 24 : bounds.minX() + 2;
            int maxX = bounds == null ? guardian.blockPosition().getX() + 24 : bounds.maxX() - 2;
            int minZ = bounds == null ? guardian.blockPosition().getZ() - 24 : bounds.minZ() + 2;
            int maxZ = bounds == null ? guardian.blockPosition().getZ() + 24 : bounds.maxZ() - 2;

            for (int attempt = 0; attempt < 4; attempt++) {
                BlockPos candidate = new BlockPos(
                        guardian.getRandom().nextIntBetweenInclusive(minX, maxX),
                        guardian.blockPosition().getY(),
                        guardian.getRandom().nextIntBetweenInclusive(minZ, maxZ)
                );
                Path candidatePath = guardian.getNavigation().createPath(candidate, 2);

                if (candidatePath != null && candidatePath.canReach()) {
                    return candidatePath;
                }
            }

            return null;
        }
    }
}
