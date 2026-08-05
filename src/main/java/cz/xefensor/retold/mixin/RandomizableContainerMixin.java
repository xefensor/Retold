package cz.xefensor.retold.mixin;

import cz.xefensor.retold.villager.RetoldVillageContainerOwnership;

import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainer.class)
public interface RandomizableContainerMixin {
    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void retold$rememberVillageLootTable(
            Player player,
            CallbackInfo ci
    ) {
        RetoldVillageContainerOwnership.beforeLootUnpack(
                (RandomizableContainer) this
        );
    }

    @Inject(method = "unpackLootTable", at = @At("RETURN"))
    private void retold$protectGeneratedVillageLoot(
            Player player,
            CallbackInfo ci
    ) {
        RetoldVillageContainerOwnership.afterLootUnpack(
                (RandomizableContainer) this
        );
    }
}
