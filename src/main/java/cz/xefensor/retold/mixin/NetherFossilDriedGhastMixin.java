package cz.xefensor.retold.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherFossilPieces.NetherFossilPiece.class)
public abstract class NetherFossilDriedGhastMixin {
    @Inject(method = "placeDriedGhast", at = @At("HEAD"), cancellable = true)
    private void retold$omitNaturalDriedGhast(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox fossilBoundingBox,
            BoundingBox chunkBoundingBox,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
