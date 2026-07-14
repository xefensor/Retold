package cz.xefensor.retold.worldgen.air;

import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GaleCore extends Breeze {
    private static final double ACTIVATION_HORIZONTAL_RANGE = 15.0D;
    private static final double ACTIVATION_VERTICAL_RANGE = 7.0D;
    private static final double FALLBACK_LEASH_HORIZONTAL_RANGE = 68.0D;
    private static final double FALLBACK_LEASH_BELOW_HOME = 60.0D;
    private static final double FALLBACK_LEASH_ABOVE_HOME = 35.0D;
    private static final double RETURN_FOLLOW = 0.16D;
    private static final double RETURN_SNAP_DISTANCE = 0.45D;
    private static final double RETURN_CRUISE_HEIGHT = 7.0D;
    private static final double RETURN_MIN_CRUISE_LIFT = 2.0D;
    private static final double MIN_FLIGHT_DISTANCE = 8.5D;
    private static final double PREFERRED_FLIGHT_DISTANCE = 11.5D;
    private static final double MAX_FLIGHT_DISTANCE = 16.0D;
    private static final double BASE_FLIGHT_RADIUS = 10.5D;
    private static final double FLIGHT_RADIUS_VARIATION = 3.2D;
    private static final double BASE_FLIGHT_HEIGHT = 4.0D;
    private static final double FLIGHT_HEIGHT_VARIATION = 2.0D;
    private static final double FLIGHT_SPEED_BASE = 0.034D;
    private static final double FLIGHT_SPEED_VARIATION = 0.022D;
    private static final double FLIGHT_FOLLOW = 0.12D;
    private static final double SAFE_FLIGHT_ANGLE_STEP = Math.PI / 10.0D;
    private static final double COMBAT_EDGE_BUFFER = 4.0D;
    private static final int HIDDEN_TARGET_WALL_SHOT_TICKS = 45;
    private static final int WALL_BREAKER_SHOT_COOLDOWN_TICKS = 55;
    private static final int FLIGHT_PATTERN_MIN_TICKS = 55;
    private static final int FLIGHT_PATTERN_RANDOM_TICKS = 80;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            this.getUUID(),
            Component.translatable("entity.retold.gale_core"),
            BossEvent.BossBarColor.WHITE,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private boolean active;
    private boolean phaseTwo;
    private UUID targetPlayerId;
    private double orbitAngle;
    private double flightPhase;
    private double radiusBias;
    private double heightBias;
    private int orbitDirection = 1;
    private int nextFlightShiftTick;
    private int hiddenTargetTicks;
    private int nextWallBreakerShotTick;
    private Vec3 homePosition;
    private AABB combatBounds;

    public GaleCore(EntityType<? extends Monster> type, net.minecraft.world.level.Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.xpReward = 40;
    }

    public void setHomePosition(Vec3 homePosition) {
        this.homePosition = homePosition;
    }

    public void setCombatBounds(AABB combatBounds) {
        this.combatBounds = combatBounds;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.43D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.MAX_HEALTH, 160.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public void tick() {
        super.tick();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.level() instanceof ServerLevel serverLevel) {
            tickBossState(serverLevel);
        }
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!active) {
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        manageTargetVisibility(level);
        super.customServerAiStep(level);
        manageTargetVisibility(level);
    }

    @Override
    public void travel(Vec3 input) {
        if (!active || phaseTwo) {
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        super.travel(input);
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
    }

    @Override
    protected void dropFromLootTable(ServerLevel level, DamageSource source, boolean playerKilled) {
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.spawnAtLocation(level, new ItemStack(RetoldBlocks.AIR_ELEMENT.get()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        if (homePosition != null) {
            output.store("retold_home", Vec3.CODEC, homePosition);
        }

        if (combatBounds != null) {
            ValueOutput bounds = output.child("retold_combat_bounds");
            bounds.putDouble("min_x", combatBounds.minX);
            bounds.putDouble("min_y", combatBounds.minY);
            bounds.putDouble("min_z", combatBounds.minZ);
            bounds.putDouble("max_x", combatBounds.maxX);
            bounds.putDouble("max_y", combatBounds.maxY);
            bounds.putDouble("max_z", combatBounds.maxZ);
        }

        output.putBoolean("retold_phase_two", phaseTwo);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        homePosition = input.read("retold_home", Vec3.CODEC).orElse(null);
        combatBounds = input.child("retold_combat_bounds")
                .map(bounds -> new AABB(
                        bounds.getDoubleOr("min_x", 0.0D),
                        bounds.getDoubleOr("min_y", 0.0D),
                        bounds.getDoubleOr("min_z", 0.0D),
                        bounds.getDoubleOr("max_x", 0.0D),
                        bounds.getDoubleOr("max_y", 0.0D),
                        bounds.getDoubleOr("max_z", 0.0D)
                ))
                .orElse(null);
        phaseTwo = input.getBooleanOr("retold_phase_two", phaseTwo);
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(level, source);
    }

    private void tickBossState(ServerLevel level) {
        ensureHomePosition();
        ensureCombatBounds();
        ServerPlayer target = getTargetPlayer(level);

        if (isOutsideCombatArea(this.position())) {
            disengageAndReturnHome();
            return;
        }

        if (!active) {
            target = findActivationPlayer(level);

            if (target == null) {
                returnToHome();
                return;
            }

            active = true;
            targetPlayerId = target.getUUID();
            initializeFlightPattern(target);
            this.setNoGravity(phaseTwo);
        }

        if (!phaseTwo && this.getHealth() <= this.getMaxHealth() * 0.5F) {
            phaseTwo = true;
            this.setNoGravity(true);
            this.setDeltaMovement(Vec3.ZERO);
            initializeFlightPattern(target);
        }

        if (target == null) {
            target = findActivationPlayer(level);

            if (target == null) {
                disengageAndReturnHome();
                return;
            }

            targetPlayerId = target.getUUID();
        }

        if (phaseTwo) {
            flyAround(target);
        } else {
            this.setNoGravity(false);
        }
    }

    private ServerPlayer getTargetPlayer(ServerLevel level) {
        if (targetPlayerId == null) {
            return null;
        }

        if (!(level.getEntity(targetPlayerId) instanceof ServerPlayer player)) {
            return null;
        }

        if (!isValidTarget(player) || isOutsideCombatArea(player.position())) {
            return null;
        }

        return player;
    }

    private ServerPlayer findActivationPlayer(ServerLevel level) {
        ServerPlayer best = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (ServerPlayer player : level.players()) {
            if (!isValidTarget(player)) {
                continue;
            }

            Vec3 home = homePosition();
            double dy = Math.abs(player.getY() - home.y);

            if (dy > ACTIVATION_VERTICAL_RANGE) {
                continue;
            }

            double dx = player.getX() - home.x;
            double dz = player.getZ() - home.z;
            double distanceSq = dx * dx + dz * dz;

            if (distanceSq > ACTIVATION_HORIZONTAL_RANGE * ACTIVATION_HORIZONTAL_RANGE) {
                continue;
            }

            if (distanceSq < bestDistanceSq) {
                best = player;
                bestDistanceSq = distanceSq;
            }
        }

        return best;
    }

    private void disengageAndReturnHome() {
        active = false;
        targetPlayerId = null;
        hiddenTargetTicks = 0;
        this.setTarget(null);
        this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        this.setNoGravity(true);
        returnToHome();
    }

    private void returnToHome() {
        Vec3 home = homePosition();
        Vec3 current = this.position();

        if (current.distanceToSqr(home) <= RETURN_SNAP_DISTANCE * RETURN_SNAP_DISTANCE) {
            this.setPos(home);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        Vec3 target = returnWaypoint(current, home);
        Vec3 next = findSafeReturnStep(target);

        if (next == null) {
            this.setDeltaMovement(Vec3.ZERO);
            this.lookAt(EntityAnchorArgument.Anchor.EYES, home);
            return;
        }

        this.setPos(next);
        this.setDeltaMovement(Vec3.ZERO);
        this.lookAt(EntityAnchorArgument.Anchor.EYES, home);
    }

    private Vec3 returnWaypoint(Vec3 current, Vec3 home) {
        AABB bounds = combatBounds();
        double cruiseY = Mth.clamp(
                Math.max(Math.max(current.y + RETURN_MIN_CRUISE_LIFT, home.y + RETURN_CRUISE_HEIGHT), current.y),
                bounds.minY + 2.0D,
                bounds.maxY - 2.0D
        );
        double horizontalDistanceSq = horizontalDistanceSq(current, home);

        if (horizontalDistanceSq > 9.0D) {
            if (current.y < cruiseY - 1.0D) {
                return new Vec3(current.x, cruiseY, current.z);
            }

            return new Vec3(home.x, cruiseY, home.z);
        }

        return home;
    }

    private Vec3 findSafeReturnStep(Vec3 target) {
        Vec3 current = this.position();
        Vec3 direct = current.lerp(target, RETURN_FOLLOW);

        if (canMoveTo(direct)) {
            return direct;
        }

        for (Vec3 offset : new Vec3[]{
                new Vec3(0.0D, 0.8D, 0.0D),
                new Vec3(0.0D, 1.6D, 0.0D),
                new Vec3(1.0D, 0.5D, 0.0D),
                new Vec3(-1.0D, 0.5D, 0.0D),
                new Vec3(0.0D, 0.5D, 1.0D),
                new Vec3(0.0D, 0.5D, -1.0D)
        }) {
            Vec3 candidate = current.lerp(target.add(offset), RETURN_FOLLOW);

            if (canMoveTo(candidate)) {
                return candidate;
            }
        }

        Vec3 lift = current.add(0.0D, 0.35D, 0.0D);
        return canMoveTo(lift) ? lift : null;
    }

    private boolean isOutsideCombatArea(Vec3 position) {
        return !combatBounds().contains(position);
    }

    private Vec3 homePosition() {
        ensureHomePosition();
        return homePosition;
    }

    private AABB combatBounds() {
        ensureCombatBounds();
        return combatBounds;
    }

    private void ensureHomePosition() {
        if (homePosition == null) {
            homePosition = this.position();
        }
    }

    private void ensureCombatBounds() {
        if (combatBounds == null) {
            Vec3 home = homePosition();
            combatBounds = new AABB(
                    home.x - FALLBACK_LEASH_HORIZONTAL_RANGE,
                    home.y - FALLBACK_LEASH_BELOW_HOME,
                    home.z - FALLBACK_LEASH_HORIZONTAL_RANGE,
                    home.x + FALLBACK_LEASH_HORIZONTAL_RANGE,
                    home.y + FALLBACK_LEASH_ABOVE_HOME,
                    home.z + FALLBACK_LEASH_HORIZONTAL_RANGE
            );
        }
    }

    private static double horizontalDistanceSq(Vec3 first, Vec3 second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return dx * dx + dz * dz;
    }

    private void initializeFlightPattern(ServerPlayer target) {
        orbitAngle = Math.atan2(this.getZ() - target.getZ(), this.getX() - target.getX());
        flightPhase = this.random.nextDouble() * Math.PI * 2.0D;
        orbitDirection = this.random.nextBoolean() ? 1 : -1;
        hiddenTargetTicks = 0;
        randomizeFlightBias();
    }

    private void manageTargetVisibility(ServerLevel level) {
        ServerPlayer target = getTargetPlayer(level);

        if (target == null) {
            hiddenTargetTicks = 0;
            return;
        }

        HitResult sightTrace = traceToTarget(target);

        if (sightTrace.getType() == HitResult.Type.MISS) {
            hiddenTargetTicks = 0;
            return;
        }

        hiddenTargetTicks++;
        suppressNormalShot();

        if (hiddenTargetTicks >= HIDDEN_TARGET_WALL_SHOT_TICKS && this.tickCount >= nextWallBreakerShotTick) {
            shootBlockingWall(level, sightTrace.getLocation());
            nextWallBreakerShotTick = this.tickCount + WALL_BREAKER_SHOT_COOLDOWN_TICKS;
        }
    }

    private void suppressNormalShot() {
        this.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
        this.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING);
        this.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING);
        this.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, 8L);
    }

    private HitResult traceToTarget(ServerPlayer target) {
        Vec3 from = firingPosition();
        Vec3 to = target.getEyePosition();
        return this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
    }

    private void shootBlockingWall(ServerLevel level, Vec3 hitLocation) {
        Vec3 from = firingPosition();
        Vec3 shot = hitLocation.subtract(from);

        if (shot.lengthSqr() < 0.25D) {
            return;
        }

        Projectile.spawnProjectileUsingShoot(
                new BreezeWindCharge(this, level),
                level,
                ItemStack.EMPTY,
                shot.x,
                shot.y,
                shot.z,
                0.8F,
                1.0F
        );
        this.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, 8L);
        this.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 0.85F);
    }

    private Vec3 firingPosition() {
        return new Vec3(this.getX(), this.getFiringYPosition(), this.getZ());
    }

    private void flyAround(ServerPlayer player) {
        updateFlightPattern();
        adjustFlightForDistance(player);
        orbitAngle += orbitDirection * currentFlightSpeed(player);

        Vec3 next = findSafeFlightStep(player);

        if (next == null) {
            this.setDeltaMovement(Vec3.ZERO);
            this.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
            return;
        }

        this.setPos(next);
        this.setDeltaMovement(Vec3.ZERO);
        this.lookAt(EntityAnchorArgument.Anchor.EYES, player.getEyePosition());
    }

    private void updateFlightPattern() {
        if (this.tickCount < nextFlightShiftTick) {
            return;
        }

        if (this.random.nextFloat() < 0.35F) {
            orbitDirection *= -1;
        }

        randomizeFlightBias();
    }

    private void adjustFlightForDistance(ServerPlayer player) {
        double distanceSq = horizontalDistanceSq(this.position(), player.position());
        double retreatAngle = Math.atan2(this.getZ() - player.getZ(), this.getX() - player.getX());

        if (distanceSq < MIN_FLIGHT_DISTANCE * MIN_FLIGHT_DISTANCE) {
            orbitAngle = approachAngle(orbitAngle, retreatAngle + orbitDirection * 0.35D, 0.28D);
            nextFlightShiftTick = Math.min(nextFlightShiftTick, this.tickCount + 8);
        } else if (distanceSq < PREFERRED_FLIGHT_DISTANCE * PREFERRED_FLIGHT_DISTANCE) {
            orbitAngle = approachAngle(orbitAngle, retreatAngle + orbitDirection * 0.75D, 0.09D);
        }
    }

    private void randomizeFlightBias() {
        radiusBias = (this.random.nextDouble() - 0.5D) * FLIGHT_RADIUS_VARIATION;
        heightBias = (this.random.nextDouble() - 0.5D) * FLIGHT_HEIGHT_VARIATION;
        nextFlightShiftTick = this.tickCount + FLIGHT_PATTERN_MIN_TICKS + this.random.nextInt(FLIGHT_PATTERN_RANDOM_TICKS);
    }

    private double currentFlightSpeed(ServerPlayer player) {
        double wave = Math.sin(this.tickCount * 0.025D + flightPhase);
        double speed = FLIGHT_SPEED_BASE + wave * FLIGHT_SPEED_VARIATION;
        double distanceSq = horizontalDistanceSq(this.position(), player.position());

        if (distanceSq < MIN_FLIGHT_DISTANCE * MIN_FLIGHT_DISTANCE) {
            speed += 0.04D;
        } else if (distanceSq < PREFERRED_FLIGHT_DISTANCE * PREFERRED_FLIGHT_DISTANCE) {
            speed += 0.018D;
        }

        return Math.max(0.018D, speed);
    }

    private Vec3 findSafeFlightStep(ServerPlayer player) {
        Vec3 best = null;
        double bestScore = -Double.MAX_VALUE;
        double preferredDistance = currentPreferredFlightDistance();

        for (int i = 0; i <= 12; i++) {
            double angleOffset = i == 0
                    ? 0.0D
                    : ((i + 1) / 2) * SAFE_FLIGHT_ANGLE_STEP * (i % 2 == 0 ? 1.0D : -1.0D);

            for (double radiusScale : new double[]{1.0D, 1.18D, 0.92D, 1.34D, 0.78D}) {
                for (double heightOffset : new double[]{0.0D, 1.4D, -0.8D, 2.6D}) {
                    Vec3 next = flightStep(player, orbitAngle + angleOffset, heightOffset, radiusScale);

                    if (!canUseFlightPosition(next)) {
                        continue;
                    }

                    double score = scoreFlightPosition(player, next, preferredDistance);

                    if (score > bestScore) {
                        best = next;
                        bestScore = score;
                    }
                }
            }
        }

        return best;
    }

    private double currentPreferredFlightDistance() {
        double wave = Math.sin(this.tickCount * 0.021D + flightPhase) * 1.25D;
        return Mth.clamp(PREFERRED_FLIGHT_DISTANCE + radiusBias * 0.35D + wave, MIN_FLIGHT_DISTANCE + 1.0D, MAX_FLIGHT_DISTANCE);
    }

    private boolean canUseFlightPosition(Vec3 next) {
        return combatBounds().contains(next) && canMoveTo(next);
    }

    private double scoreFlightPosition(ServerPlayer player, Vec3 next, double preferredDistance) {
        double horizontalDistance = Math.sqrt(horizontalDistanceSq(next, player.position()));
        double heightAbovePlayer = next.y - player.getY();
        double distanceScore = -Math.abs(horizontalDistance - preferredDistance) * 7.0D;
        double heightScore = -Math.abs(heightAbovePlayer - BASE_FLIGHT_HEIGHT) * 2.5D;
        double edgeScore = Math.min(distanceToCombatEdge(next), 14.0D) * 1.5D;
        double movementPenalty = this.position().distanceTo(next) * 0.25D;

        if (horizontalDistance < MIN_FLIGHT_DISTANCE) {
            distanceScore -= (MIN_FLIGHT_DISTANCE - horizontalDistance) * 80.0D;
        } else if (horizontalDistance > MAX_FLIGHT_DISTANCE) {
            distanceScore -= (horizontalDistance - MAX_FLIGHT_DISTANCE) * 18.0D;
        }

        if (heightAbovePlayer < 2.0D) {
            heightScore -= (2.0D - heightAbovePlayer) * 40.0D;
        }

        if (distanceToCombatEdge(next) < COMBAT_EDGE_BUFFER) {
            edgeScore -= (COMBAT_EDGE_BUFFER - distanceToCombatEdge(next)) * 35.0D;
        }

        return distanceScore + heightScore + edgeScore - movementPenalty;
    }

    private double distanceToCombatEdge(Vec3 position) {
        AABB bounds = combatBounds();
        return Math.min(
                Math.min(position.x - bounds.minX, bounds.maxX - position.x),
                Math.min(position.z - bounds.minZ, bounds.maxZ - position.z)
        );
    }

    private Vec3 flightStep(ServerPlayer player, double angle, double heightOffset, double radiusScale) {
        double time = this.tickCount;
        double rawRadius = BASE_FLIGHT_RADIUS + radiusBias
                + Math.sin(time * 0.033D + flightPhase) * 0.9D
                + Math.sin(time * 0.011D + flightPhase * 0.7D) * 0.65D;
        double radius = Mth.clamp(rawRadius * radiusScale, MIN_FLIGHT_DISTANCE, MAX_FLIGHT_DISTANCE + 1.5D);
        double xRadius = radius * (1.0D + Math.sin(time * 0.017D + flightPhase) * 0.18D);
        double zRadius = radius * (0.88D + Math.cos(time * 0.014D + flightPhase * 1.3D) * 0.16D);
        double sideDrift = Math.sin(time * 0.047D + flightPhase) * 0.85D;
        double height = BASE_FLIGHT_HEIGHT + heightBias + heightOffset
                + Math.sin(time * 0.041D + flightPhase) * 0.95D
                + Math.sin(angle * 2.0D + flightPhase) * 0.45D;

        double desiredX = player.getX() + Math.cos(angle) * xRadius + Math.cos(angle + Math.PI / 2.0D) * sideDrift;
        double desiredY = player.getY() + Math.max(1.9D, height);
        double desiredZ = player.getZ() + Math.sin(angle) * zRadius + Math.sin(angle + Math.PI / 2.0D) * sideDrift;
        Vec3 current = this.position();
        Vec3 desired = clampToCombatBounds(new Vec3(desiredX, desiredY, desiredZ));
        return current.lerp(desired, FLIGHT_FOLLOW);
    }

    private Vec3 clampToCombatBounds(Vec3 position) {
        AABB bounds = combatBounds();
        return new Vec3(
                Mth.clamp(position.x, bounds.minX + 1.5D, bounds.maxX - 1.5D),
                Mth.clamp(position.y, bounds.minY + 1.5D, bounds.maxY - 1.5D),
                Mth.clamp(position.z, bounds.minZ + 1.5D, bounds.maxZ - 1.5D)
        );
    }

    private boolean canMoveTo(Vec3 next) {
        Vec3 delta = next.subtract(this.position());
        return this.isFree(delta.x, delta.y, delta.z);
    }

    private static double approachAngle(double current, double target, double maxStep) {
        double delta = target - current;

        while (delta > Math.PI) {
            delta -= Math.PI * 2.0D;
        }

        while (delta < -Math.PI) {
            delta += Math.PI * 2.0D;
        }

        return current + Mth.clamp(delta, -maxStep, maxStep);
    }

    private static boolean isValidTarget(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isCreative();
    }
}
