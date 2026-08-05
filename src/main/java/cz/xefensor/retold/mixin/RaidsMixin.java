package cz.xefensor.retold.mixin;

import cz.xefensor.retold.stage.RetoldRaidProgression;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Raids.class)
public abstract class RaidsMixin {
    @Inject(
            method = "createOrExtendRaid",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$requireStageThreeToStartRaid(
            ServerPlayer player,
            BlockPos raidPosition,
            CallbackInfoReturnable<Raid> cir
    ) {
        if (!RetoldRaidProgression.canStartRaid(player.level())) {
            cir.setReturnValue(null);
        }
    }
}
