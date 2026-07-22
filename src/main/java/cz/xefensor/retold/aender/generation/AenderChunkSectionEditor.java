package cz.xefensor.retold.aender.generation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Low-level section replacement shared by initial generation and loaded-chunk regeneration.
 */
final class AenderChunkSectionEditor {
    private AenderChunkSectionEditor() {
    }

    static void clear(ChunkAccess chunk) {
        // Remove block entities and their tickers before replacing their backing states.
        for (BlockPos blockEntityPos : chunk.getBlockEntitiesPos()) {
            chunk.removeBlockEntity(blockEntityPos);
        }

        LevelChunkSection[] sections = chunk.getSections();
        LevelChunk levelChunk = chunk instanceof LevelChunk loadedChunk ? loadedChunk : null;

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection oldSection = sections[sectionIndex];

            if (oldSection.hasOnlyAir()) {
                continue;
            }

            if (levelChunk != null) {
                int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                BlockPos sectionOrigin = new BlockPos(
                        chunk.getPos().getMinBlockX(),
                        SectionPos.sectionToBlockCoord(sectionY),
                        chunk.getPos().getMinBlockZ()
                );

                levelChunk.getLevel().getChunkSource().getLightEngine().updateSectionStatus(sectionOrigin, true);
                levelChunk.getLevel().getChunkSource().onSectionEmptinessChanged(
                        chunk.getPos().x(),
                        sectionY,
                        chunk.getPos().z(),
                        true
                );

                sections[sectionIndex] = new LevelChunkSection(
                        levelChunk.getLevel().palettedContainerFactory().createForBlockStates(),
                        oldSection.getBiomes()
                );
            } else {
                // World-generation containers start with air at palette index zero.
                sections[sectionIndex] = new LevelChunkSection(
                        oldSection.getStates().recreate(),
                        oldSection.getBiomes()
                );
            }
        }

        chunk.markUnsaved();
    }

    static void primeFreshHeightmaps(ChunkAccess chunk) {
        EnumSet<Heightmap.Types> heightmaps = EnumSet.of(
                Heightmap.Types.WORLD_SURFACE_WG,
                Heightmap.Types.OCEAN_FLOOR_WG,
                Heightmap.Types.MOTION_BLOCKING
        );

        chunk.getHeightmaps().forEach(entry -> {
            heightmaps.add(entry.getKey());
            Arrays.fill(entry.getValue().getRawData(), 0L);
        });

        Heightmap.primeHeightmaps(chunk, heightmaps);
    }
}
