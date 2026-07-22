package cz.xefensor.retold.aender.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Resolves the biome of each floating island in three dimensions. */
public final class AenderBiomeSource extends BiomeSource {
    public static final MapCodec<AenderBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.CODEC.fieldOf("plains").forGetter(source -> source.plains),
            Biome.CODEC.fieldOf("desert").forGetter(source -> source.desert)
    ).apply(instance, AenderBiomeSource::new));

    private final Holder<Biome> plains;
    private final Holder<Biome> desert;
    private final ThreadLocal<BiomeQueryCache> queryCache = ThreadLocal.withInitial(BiomeQueryCache::new);

    public AenderBiomeSource(Holder<Biome> plains, Holder<Biome> desert) {
        this.plains = plains;
        this.desert = desert;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(plains, desert);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int blockX = QuartPos.toBlock(quartX) + 2;
        int blockY = QuartPos.toBlock(quartY) + 2;
        int blockZ = QuartPos.toBlock(quartZ) + 2;
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        long revision = AenderVolatility.currentGenerationRevision();
        BiomeQueryCache cache = queryCache.get();

        if (!cache.matches(chunkX, chunkZ, revision)) {
            cache.reset(chunkX, chunkZ, revision);
        }

        List<AenderIslandSampler.BiomeColumn> columns = cache.columns.computeIfAbsent(
                columnKey(blockX, blockZ),
                ignored -> AenderIslandSampler.biomeColumnsAt(cache.islands, blockX, blockZ)
        );

        return switch (AenderIslandSampler.biomeFromColumns(columns, blockY)) {
            case DESERT -> desert;
            case PLAINS -> plains;
        };
    }

    private static long columnKey(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    private static final class BiomeQueryCache {
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ = Integer.MIN_VALUE;
        private long revision = Long.MIN_VALUE;
        private List<AenderIslandSampler.Island> islands = List.of();
        private final Map<Long, List<AenderIslandSampler.BiomeColumn>> columns = new HashMap<>();

        private boolean matches(int candidateChunkX, int candidateChunkZ, long candidateRevision) {
            return chunkX == candidateChunkX
                    && chunkZ == candidateChunkZ
                    && revision == candidateRevision;
        }

        private void reset(int nextChunkX, int nextChunkZ, long nextRevision) {
            chunkX = nextChunkX;
            chunkZ = nextChunkZ;
            revision = nextRevision;
            islands = AenderIslandSampler.islandsForChunk(nextChunkX, nextChunkZ);
            columns.clear();
        }
    }
}
