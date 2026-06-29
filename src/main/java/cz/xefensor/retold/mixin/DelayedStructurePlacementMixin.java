package cz.xefensor.retold.mixin;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.stage.RetoldStageRuntime;
import cz.xefensor.retold.worldgen.delayed.RetoldAttachments;
import cz.xefensor.retold.worldgen.delayed.RetoldChunkStructureData;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureHelper;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureIds;
import cz.xefensor.retold.worldgen.delayed.RetoldDelayedStructureMobBlocker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public abstract class DelayedStructurePlacementMixin {

    @Inject(
            method = "placeInChunk",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$cancelDelayedStructurePlacementBeforeRequiredStage(
            WorldGenLevel worldGenLevel,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox boundingBox,
            ChunkPos chunkPos,
            CallbackInfo ci
    ) {
        ServerLevel level = worldGenLevel.getLevel();

        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        StructureStart start = (StructureStart) (Object) this;
        Structure structure = start.getStructure();

        if (!RetoldDelayedStructureHelper.isDelayedStructure(level.registryAccess(), structure)) {
            return;
        }

        String structureId =
                RetoldDelayedStructureHelper.getStructureId(level.registryAccess(), structure);

        if (RetoldStageRuntime.isAtLeast(RetoldDelayedStructureIds.requiredStage(structureId))) {
            return;
        }

        retold$markChunkDeferred(worldGenLevel, chunkPos, structureId);

        ci.cancel();
    }

    @Unique
    private static void retold$markChunkDeferred(
            WorldGenLevel worldGenLevel,
            ChunkPos chunkPos,
            String structureId
    ) {
        ChunkAccess chunk;

        try {
            chunk = worldGenLevel.getChunk(chunkPos.x(), chunkPos.z());
        } catch (Exception exception) {
            Retold.LOGGER.warn(
                    "Could not access worldgen chunk [{}, {}] while deferring {}",
                    chunkPos.x(),
                    chunkPos.z(),
                    structureId,
                    exception
            );
            return;
        }

        RetoldChunkStructureData oldData =
                chunk.getData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get());

        RetoldChunkStructureData newData =
                oldData.withDeferred(structureId);

        RetoldDelayedStructureMobBlocker.rememberDeferredStructure(structureId, chunkPos);

        if (newData != oldData) {
            chunk.setData(RetoldAttachments.CHUNK_STRUCTURE_DATA.get(), newData);

            Retold.LOGGER.info(
                    "Deferred vanilla placement of {} in worldgen chunk [{}, {}]",
                    structureId,
                    chunkPos.x(),
                    chunkPos.z()
            );
        }
    }
}