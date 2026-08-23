package cz.xefensor.retold.mixin;

import cz.xefensor.retold.worldgen.portal.RetoldNetherPortalDrainTravel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherPortalBlock.class)
public abstract class NetherPortalDrainTravelMixin {
    @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true)
    private void retold$attachSuccessfulPortalDrainPulse(
            ServerLevel currentLevel,
            Entity entity,
            BlockPos portalEntryPos,
            CallbackInfoReturnable<TeleportTransition> cir
    ) {
        TeleportTransition transition = cir.getReturnValue();
        if (transition != null) {
            cir.setReturnValue(
                    RetoldNetherPortalDrainTravel.attachSuccessfulTravelPulse(
                            currentLevel,
                            portalEntryPos,
                            transition
                    )
            );
        }
    }
}
