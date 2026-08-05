package cz.xefensor.retold.mixin;

import cz.xefensor.retold.villager.RetoldVillageCropOwnership;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HarvestFarmland.class)
public abstract class HarvestFarmlandMixin {
    @Shadow
    private BlockPos aboveFarmlandPos;

    @Unique
    private BlockPos retold$workedCropPos;

    @Unique
    private BlockState retold$cropBeforeWork;

    @Inject(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
            at = @At("HEAD")
    )
    private void retold$rememberCropBeforeFarmerWork(
            ServerLevel level,
            Villager villager,
            long gameTime,
            CallbackInfo ci
    ) {
        if (aboveFarmlandPos == null) {
            retold$workedCropPos = null;
            retold$cropBeforeWork = null;
            return;
        }

        retold$workedCropPos = aboveFarmlandPos.immutable();
        retold$cropBeforeWork = level.getBlockState(retold$workedCropPos);
    }

    @Inject(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/npc/villager/Villager;J)V",
            at = @At("RETURN")
    )
    private void retold$recordCropAfterFarmerWork(
            ServerLevel level,
            Villager villager,
            long gameTime,
            CallbackInfo ci
    ) {
        if (retold$workedCropPos != null && retold$cropBeforeWork != null) {
            RetoldVillageCropOwnership.afterFarmerWork(
                    level,
                    retold$workedCropPos,
                    retold$cropBeforeWork,
                    level.getBlockState(retold$workedCropPos)
            );
        }

        retold$workedCropPos = null;
        retold$cropBeforeWork = null;
    }
}
