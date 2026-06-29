package cz.xefensor.retold.mixin;

import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.worldgen.RetoldStructureTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public abstract class DelayedStructureGenerationMixin {

    @Inject(
            method = "tryGenerateStructure",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$blockDelayedStructuresBeforeStage2(
            StructureSet.StructureSelectionEntry entry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            ResourceKey dimension,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (dimension != Level.OVERWORLD) {
            return;
        }

        if (!entry.structure().is(RetoldStructureTags.DELAYED_UNTIL_STAGE_2)) {
            return;
        }

        if (!RetoldStageRuntime.isAtLeast(RetoldWorldStage.STAGE_2)) {
            cir.setReturnValue(false);
        }
    }
}