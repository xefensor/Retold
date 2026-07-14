package cz.xefensor.retold.mixin;

import cz.xefensor.retold.Retold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    private static final ResourceKey<Level> RETOLD_AENDER =
            ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(Retold.MODID, "aender")
            );

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void retold$explodeBedsInAender(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(RETOLD_AENDER)) {
            return;
        }

        level.removeBlock(pos, false);
        level.explode(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                5.0F,
                true,
                Level.ExplosionInteraction.BLOCK
        );

        cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
    }
}
