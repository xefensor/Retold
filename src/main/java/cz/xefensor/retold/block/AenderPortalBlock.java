package cz.xefensor.retold.block;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.aender.portal.AenderPortalLogic;
import cz.xefensor.retold.aender.portal.AenderPortalShape;
import cz.xefensor.retold.aender.portal.AenderPortalWarmup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class AenderPortalBlock extends Block implements Portal {
    public static final MapCodec<AenderPortalBlock> CODEC = simpleCodec(AenderPortalBlock::new);

    private static final DustParticleOptions PARTICLE = new DustParticleOptions(0x660F92, 0.85F);
    private static final VoxelShape SHAPE = Block.column(16.0D, 6.0D, 10.0D);
    private static final int SURVIVAL_TRANSITION_TICKS = 80;

    public AenderPortalBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return state.getShape(level, pos);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            RandomSource random
    ) {
        return AenderPortalShape.findComplete(level, pos).isPresent()
                ? super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random)
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void entityInside(
            BlockState state,
            Level level,
            BlockPos pos,
            Entity entity,
            InsideBlockEffectApplier effectApplier,
            boolean isPrecise
    ) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);

            if (level instanceof ServerLevel serverLevel
                    && entity instanceof ServerPlayer player
                    && !player.getAbilities().invulnerable) {
                AenderPortalWarmup.tick(
                        serverLevel,
                        player,
                        getPortalTransitionTime(serverLevel, player)
                );
            }
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        if (!(entity instanceof Player player)) {
            return 0;
        }

        int configuredDelay = Math.max(
                0,
                level.getGameRules().get(
                        player.getAbilities().invulnerable
                                ? GameRules.PLAYERS_NETHER_PORTAL_CREATIVE_DELAY
                                : GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY
                )
        );

        return player.getAbilities().invulnerable
                ? configuredDelay
                : Math.max(SURVIVAL_TRANSITION_TICKS, configuredDelay);
    }

    @Override
    public @Nullable TeleportTransition getPortalDestination(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalEntryPos
    ) {
        return AenderPortalLogic.getPortalDestination(currentLevel, entity, portalEntryPos);
    }

    @Override
    public Portal.Transition getLocalTransition() {
        return Portal.Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS,
                    0.5F,
                    random.nextFloat() * 0.4F + 0.8F,
                    false
            );
        }

        double x = pos.getX() + random.nextDouble();
        double y = pos.getY() + 0.25D + random.nextDouble() * 0.25D;
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(PARTICLE, x, y, z, 0.0D, 0.02D, 0.0D);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }
}
