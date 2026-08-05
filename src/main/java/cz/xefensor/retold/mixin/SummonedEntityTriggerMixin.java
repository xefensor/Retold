package cz.xefensor.retold.mixin;

import cz.xefensor.retold.golem.RetoldGolemAnimation;

import net.minecraft.advancements.triggers.SummonedEntityTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SummonedEntityTrigger.class)
public abstract class SummonedEntityTriggerMixin {
    @Inject(
            method = "trigger",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$skipVillagerBuiltIronGolemAdvancement(
            ServerPlayer player,
            Entity entity,
            CallbackInfo ci
    ) {
        if (RetoldGolemAnimation.suppressesSummonedEntityAdvancement(entity)) {
            ci.cancel();
        }
    }
}
