package cz.xefensor.retold.mixin;

import cz.xefensor.retold.worldgen.RetoldRuinedPortalChests;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RuinedPortalPiece.class)
public abstract class RuinedPortalPieceMixin {
    @Inject(
            method = "makeSettings("
                    + "Lnet/minecraft/core/HolderLookup$Provider;"
                    + "Lnet/minecraft/world/level/block/Mirror;"
                    + "Lnet/minecraft/world/level/block/Rotation;"
                    + "Lnet/minecraft/world/level/levelgen/structure/structures/"
                    + "RuinedPortalPiece$VerticalPlacement;"
                    + "Lnet/minecraft/core/BlockPos;"
                    + "Lnet/minecraft/world/level/levelgen/structure/structures/"
                    + "RuinedPortalPiece$Properties;"
                    + ")Lnet/minecraft/world/level/levelgen/structure/templatesystem/"
                    + "StructurePlaceSettings;",
            at = @At("RETURN")
    )
    private static void retold$removeChestsFromPlacementSettings(
            CallbackInfoReturnable<StructurePlaceSettings> cir
    ) {
        RetoldRuinedPortalChests.removeChests(cir.getReturnValue());
    }
}
