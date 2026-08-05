package cz.xefensor.retold.mixin;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.villager.RetoldVillagerGolemConstruction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerGolemConstructionMixin {
    @Inject(
            method = "spawnGolemIfNeeded",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$replaceInstantGolemWithConstruction(
            ServerLevel level,
            long timestamp,
            int villagersNeededToAgree,
            CallbackInfo ci
    ) {
        if (RetoldWorldData.get(level).getStage().getId()
                < RetoldWorldStage.STAGE_2.getId()) {
            return;
        }

        RetoldVillagerGolemConstruction.replaceVanillaSpawnAttempt(
                level,
                (Villager) (Object) this,
                timestamp,
                villagersNeededToAgree
        );
        ci.cancel();
    }
}
