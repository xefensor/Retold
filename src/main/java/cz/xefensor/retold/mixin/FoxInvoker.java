package cz.xefensor.retold.mixin;

import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Fox.class)
public interface FoxInvoker {
    @Invoker("setSleeping")
    void retold$setSleeping(boolean sleeping);
}
