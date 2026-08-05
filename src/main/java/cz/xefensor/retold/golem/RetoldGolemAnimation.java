package cz.xefensor.retold.golem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.Blocks;

import java.util.function.BooleanSupplier;

public final class RetoldGolemAnimation {
    public static final int PLAYER_LEVEL_COST = 5;

    private static final ThreadLocal<Integer> VILLAGER_ANIMATION_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    private RetoldGolemAnimation() {
    }

    public static boolean beginPlayerPumpkinPlacement(
            BlockPlaceContext context,
            CarvedPumpkinBlock pumpkin
    ) {
        if (context == null || pumpkin == null) {
            return true;
        }

        Player player = context.getPlayer();

        if (player == null) {
            return true;
        }

        boolean completesGolem = isIronGolemPlacement(context, pumpkin);

        if (completesGolem
                && !player.isCreative()
                && player.experienceLevel < PLAYER_LEVEL_COST) {
            if (!(player instanceof ServerPlayer serverPlayer)
                    || serverPlayer.connection != null) {
                player.sendOverlayMessage(
                        Component.translatable(
                                "message.retold.iron_golem_requires_levels",
                                PLAYER_LEVEL_COST
                        )
                );
            }
            return false;
        }

        return true;
    }

    public static boolean animateVillagerBuiltGolem(
            BooleanSupplier animation
    ) {
        int previousDepth = VILLAGER_ANIMATION_DEPTH.get();
        VILLAGER_ANIMATION_DEPTH.set(previousDepth + 1);

        try {
            return animation.getAsBoolean();
        } finally {
            if (previousDepth == 0) {
                VILLAGER_ANIMATION_DEPTH.remove();
            } else {
                VILLAGER_ANIMATION_DEPTH.set(previousDepth);
            }
        }
    }

    public static boolean isAnimatingVillagerBuiltGolem() {
        return VILLAGER_ANIMATION_DEPTH.get() > 0;
    }

    public static boolean suppressesSummonedEntityAdvancement(
            Entity entity
    ) {
        return entity instanceof IronGolem
                && isAnimatingVillagerBuiltGolem();
    }

    public static void chargeSuccessfulPlayerPlacement(
            BlockPlaceContext context,
            boolean ironGolemBase
    ) {
        if (context == null || !ironGolemBase) {
            return;
        }

        Player player = context.getPlayer();

        if (player != null
                && !player.isCreative()) {
            player.giveExperienceLevels(-PLAYER_LEVEL_COST);
        }
    }

    public static boolean isIronGolemPlacement(
            BlockPlaceContext context,
            CarvedPumpkinBlock pumpkin
    ) {
        if (context == null || pumpkin == null) {
            return false;
        }

        BlockPos top = context.getClickedPos();
        BlockPos middle = top.below();
        BlockPos bottom = middle.below();

        if (!context.getLevel().getBlockState(middle).is(Blocks.IRON_BLOCK)
                || !context.getLevel().getBlockState(bottom)
                .is(Blocks.IRON_BLOCK)) {
            return false;
        }

        boolean ironShape = context.getLevel().getBlockState(middle.west())
                .is(Blocks.IRON_BLOCK)
                && context.getLevel().getBlockState(middle.east())
                .is(Blocks.IRON_BLOCK)
                || context.getLevel().getBlockState(middle.north())
                .is(Blocks.IRON_BLOCK)
                && context.getLevel().getBlockState(middle.south())
                .is(Blocks.IRON_BLOCK);

        return ironShape && pumpkin.canSpawnGolem(context.getLevel(), top);
    }
}
