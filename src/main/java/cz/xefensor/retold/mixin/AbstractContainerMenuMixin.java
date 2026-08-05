package cz.xefensor.retold.mixin;

import cz.xefensor.retold.villager.RetoldVillageContainerOwnership;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Unique
    private RetoldVillageContainerOwnership.PlayerTransaction
            retold$villageContainerTransaction;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void retold$beginVillageContainerTransaction(
            int slotIndex,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci
    ) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        retold$villageContainerTransaction =
                RetoldVillageContainerOwnership.beginPlayerTransaction(
                        player,
                        menu.slots
                );
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void retold$finishVillageContainerTransaction(
            int slotIndex,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci
    ) {
        RetoldVillageContainerOwnership.finishPlayerTransaction(
                retold$villageContainerTransaction
        );
        retold$villageContainerTransaction = null;
    }
}
