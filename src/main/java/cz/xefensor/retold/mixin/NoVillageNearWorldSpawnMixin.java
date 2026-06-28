package cz.xefensor.retold.mixin;

import cz.xefensor.retold.worldgen.RetoldWorldSpawnCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
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
public abstract class NoVillageNearWorldSpawnMixin {
    private static final int RETOLD_NO_VILLAGE_RADIUS = 512;
    private static final long RETOLD_NO_VILLAGE_RADIUS_SQR =
            (long) RETOLD_NO_VILLAGE_RADIUS * RETOLD_NO_VILLAGE_RADIUS;

    @Inject(
            method = "tryGenerateStructure",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$preventVillageNearWorldSpawn(
            StructureSet.StructureSelectionEntry entry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState randomState,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            ResourceKey<Level> dimension,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (dimension != Level.OVERWORLD) {
            return;
        }

        if (!entry.structure().is(StructureTags.VILLAGE)) {
            return;
        }

        if (retold$isChunkNearWorldSpawn(chunkPos)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean retold$isChunkNearWorldSpawn(ChunkPos chunkPos) {
        BlockPos spawn = RetoldWorldSpawnCache.getOverworldSpawn();

        int chunkCenterX = (chunkPos.x() << 4) + 8;
        int chunkCenterZ = (chunkPos.z() << 4) + 8;

        long dx = chunkCenterX - spawn.getX();
        long dz = chunkCenterZ - spawn.getZ();

        return dx * dx + dz * dz <= RETOLD_NO_VILLAGE_RADIUS_SQR;
    }
}