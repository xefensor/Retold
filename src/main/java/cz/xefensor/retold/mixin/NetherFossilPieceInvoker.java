package cz.xefensor.retold.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetherFossilPieces.NetherFossilPiece.class)
public interface NetherFossilPieceInvoker {
    @Invoker("placeDriedGhast")
    void retold$invokePlaceDriedGhast(
            WorldGenLevel level,
            RandomSource random,
            BoundingBox fossilBoundingBox,
            BoundingBox chunkBoundingBox
    );
}
