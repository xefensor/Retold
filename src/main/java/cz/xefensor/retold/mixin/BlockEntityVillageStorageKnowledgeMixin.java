package cz.xefensor.retold.mixin;

import cz.xefensor.retold.villager.RetoldVillageContainerOwnership;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Event-time storage observation; this does not add a block-entity tick path.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityVillageStorageKnowledgeMixin {
    @Inject(method = "setChanged", at = @At("TAIL"))
    private void retold$refreshVillageStorageKnowledge(CallbackInfo ci) {
        RetoldVillageContainerOwnership.onContainerChanged(
                (BlockEntity) (Object) this
        );
    }
}
