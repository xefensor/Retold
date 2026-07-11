package cz.xefensor.retold.aender.entity;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.aender.generation.AenderIslandSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AenderEye extends PathfinderMob implements ItemSupplier {
    private static final DustParticleOptions IDLE_PARTICLE = new DustParticleOptions(0x87FF7A, 0.55F);
    private static final int SPAWN_RELOCATION_ATTEMPTS = 32;
    private static final int BETWEEN_ISLAND_SEARCH_RADIUS = 80;

    public AenderEye(EntityType<? extends AenderEye> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 18, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.10D)
                .add(Attributes.FLYING_SPEED, 0.08D);
    }

    public static boolean checkAenderEyeSpawnRules(
            EntityType<AenderEye> type,
            ServerLevelAccessor level,
            EntitySpawnReason spawnReason,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension() == RetoldAenderDimensions.AENDER
                && random.nextInt(5) == 0
                && level.isEmptyBlock(pos)
                && level.isEmptyBlock(pos.above())
                && level.getFluidState(pos).isEmpty();
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            EntitySpawnReason spawnReason,
            @Nullable SpawnGroupData groupData
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, groupData);

        if (spawnReason == EntitySpawnReason.NATURAL || spawnReason == EntitySpawnReason.CHUNK_GENERATION) {
            relocateToAenderObservationSpace(level);
        }

        return result;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 5.0F, 1.0D, 1.25D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 18.0F, 0.08F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Mob.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setRequiredPathLength(32.0F);
        return navigation;
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        super.tick();

        if (this.level().isClientSide() && this.isAlive()) {
            spawnIdleParticles();
        }
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(level, source);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.ENDER_EYE);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    private void spawnIdleParticles() {
        RandomSource random = this.getRandom();

        if (random.nextInt(3) != 0) {
            return;
        }

        double x = this.getX() + (random.nextDouble() - 0.5D) * this.getBbWidth();
        double y = this.getY() + this.getBbHeight() * (0.35D + random.nextDouble() * 0.45D);
        double z = this.getZ() + (random.nextDouble() - 0.5D) * this.getBbWidth();

        double xd = (random.nextDouble() - 0.5D) * 0.012D;
        double yd = 0.004D + random.nextDouble() * 0.008D;
        double zd = (random.nextDouble() - 0.5D) * 0.012D;

        this.level().addParticle(IDLE_PARTICLE, x, y, z, xd, yd, zd);
    }

    private void relocateToAenderObservationSpace(ServerLevelAccessor level) {
        RandomSource random = level.getRandom();
        BlockPos origin = this.blockPosition();

        for (int attempt = 0; attempt < SPAWN_RELOCATION_ATTEMPTS; attempt++) {
            boolean betweenIslands = attempt > 7 && random.nextBoolean();
            BlockPos candidate = betweenIslands
                    ? findBetweenIslandSpawn(level, origin, random)
                    : findAboveIslandSpawn(level, origin, random);

            if (candidate == null || !isValidObservationSpawn(level, candidate)) {
                continue;
            }

            this.snapTo(
                    candidate.getX() + 0.5D,
                    candidate.getY(),
                    candidate.getZ() + 0.5D,
                    random.nextFloat() * 360.0F,
                    0.0F
            );
            return;
        }

        BlockPos fallback = fallbackObservationSpawn(level, origin, random);
        this.snapTo(
                fallback.getX() + 0.5D,
                fallback.getY(),
                fallback.getZ() + 0.5D,
                random.nextFloat() * 360.0F,
                0.0F
        );
    }

    private static @Nullable BlockPos findAboveIslandSpawn(
            ServerLevelAccessor level,
            BlockPos origin,
            RandomSource random
    ) {
        int x = origin.getX() + random.nextInt(65) - 32;
        int z = origin.getZ() + random.nextInt(65) - 32;
        int surfaceY = AenderIslandSampler.highestBlockYAt(x, z);

        if (surfaceY < AenderIslandSampler.MIN_Y) {
            return null;
        }

        int y = surfaceY + 8 + random.nextInt(18);
        return clampToAenderHeight(level, new BlockPos(x, y, z));
    }

    private static @Nullable BlockPos findBetweenIslandSpawn(
            ServerLevelAccessor level,
            BlockPos origin,
            RandomSource random
    ) {
        int x = origin.getX() + random.nextInt(129) - 64;
        int z = origin.getZ() + random.nextInt(129) - 64;

        if (AenderIslandSampler.highestBlockYAt(x, z) >= AenderIslandSampler.MIN_Y) {
            return null;
        }

        int nearbySurfaceY = nearbySurfaceY(x, z, BETWEEN_ISLAND_SEARCH_RADIUS);

        if (nearbySurfaceY < AenderIslandSampler.MIN_Y) {
            return null;
        }

        int y = nearbySurfaceY + 6 + random.nextInt(24);
        return clampToAenderHeight(level, new BlockPos(x, y, z));
    }

    private static BlockPos fallbackObservationSpawn(
            ServerLevelAccessor level,
            BlockPos origin,
            RandomSource random
    ) {
        int surfaceY = AenderIslandSampler.highestBlockYAt(origin.getX(), origin.getZ());

        if (surfaceY < AenderIslandSampler.MIN_Y) {
            surfaceY = nearbySurfaceY(origin.getX(), origin.getZ(), BETWEEN_ISLAND_SEARCH_RADIUS);
        }

        if (surfaceY < AenderIslandSampler.MIN_Y) {
            surfaceY = AenderIslandSampler.MIN_Y + AenderIslandSampler.HEIGHT / 2;
        }

        return clampToAenderHeight(
                level,
                new BlockPos(origin.getX(), surfaceY + 12 + random.nextInt(12), origin.getZ())
        );
    }

    private static int nearbySurfaceY(int x, int z, int radius) {
        int best = AenderIslandSampler.MIN_Y - 1;

        for (int dx = -radius; dx <= radius; dx += 8) {
            for (int dz = -radius; dz <= radius; dz += 8) {
                int surfaceY = AenderIslandSampler.highestBlockYAt(x + dx, z + dz);

                if (surfaceY > best) {
                    best = surfaceY;
                }
            }
        }

        return best;
    }

    private static BlockPos clampToAenderHeight(ServerLevelAccessor level, BlockPos pos) {
        int minY = Math.max(level.getMinY() + 4, AenderIslandSampler.MIN_Y + 4);
        int maxY = Math.min(level.getMaxY() - 4, AenderIslandSampler.MAX_Y - 4);
        int y = Math.max(minY, Math.min(maxY, pos.getY()));

        return new BlockPos(pos.getX(), y, pos.getZ());
    }

    private static boolean isValidObservationSpawn(ServerLevelAccessor level, BlockPos pos) {
        return level.hasChunkAt(pos)
                && level.isEmptyBlock(pos)
                && level.isEmptyBlock(pos.above())
                && level.isEmptyBlock(pos.below())
                && level.getFluidState(pos).isEmpty();
    }
}
