package cz.xefensor.retold.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobMoveControl")
public interface CubeMobMoveControlInvoker {
    @Invoker("setDirection")
    void retold$setDirection(float yRot, boolean aggressive);

    @Invoker("setWantedMovement")
    void retold$setWantedMovement(double speedModifier);
}
