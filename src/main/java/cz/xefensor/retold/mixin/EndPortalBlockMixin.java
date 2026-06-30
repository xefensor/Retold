package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    @Inject(
            method = "getPortalDestination",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$redirectOverworldEndPortalToAender(
            ServerLevel level,
            Entity entity,
            BlockPos pos,
            CallbackInfoReturnable<TeleportTransition> cir
    ) {
        TeleportTransition transition =
                RetoldAenderAccess.getAenderPortalDestination(level, entity);

        if (transition == null) {
            return;
        }

        cir.setReturnValue(transition);
    }
}