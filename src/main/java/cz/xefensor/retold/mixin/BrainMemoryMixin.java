package cz.xefensor.retold.mixin;

import cz.xefensor.retold.territory.RetoldTerritoryBrainGuards;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Brain.class)
public abstract class BrainMemoryMixin {
    @Inject(
            method = "setMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private <U> void retold$blockUnauthorizedDirectTerritoryAttackMemory(
            MemoryModuleType<U> memoryType,
            U memoryValue,
            CallbackInfo callbackInfo
    ) {
        if (RetoldTerritoryBrainGuards.shouldBlockDirectMemoryWrite(memoryType, memoryValue)) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "setMemory(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/util/Optional;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private <U> void retold$blockUnauthorizedOptionalTerritoryAttackMemory(
            MemoryModuleType<U> memoryType,
            Optional<? extends U> memoryValue,
            CallbackInfo callbackInfo
    ) {
        if (RetoldTerritoryBrainGuards.shouldBlockOptionalMemoryWrite(memoryType, memoryValue)) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "setMemoryWithExpiry(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Ljava/lang/Object;J)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private <U> void retold$blockUnauthorizedExpiringTerritoryAttackMemory(
            MemoryModuleType<U> memoryType,
            U memoryValue,
            long expiry,
            CallbackInfo callbackInfo
    ) {
        if (RetoldTerritoryBrainGuards.shouldBlockDirectMemoryWrite(memoryType, memoryValue)) {
            callbackInfo.cancel();
        }
    }
}