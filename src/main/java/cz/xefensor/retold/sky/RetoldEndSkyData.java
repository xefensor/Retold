package cz.xefensor.retold.sky;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RetoldEndSkyData extends SavedData {
    public static final SavedDataType<RetoldEndSkyData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(Retold.MODID, "end_sky_data"),
                    RetoldEndSkyData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.LONG
                                    .optionalFieldOf("seed")
                                    .forGetter(data -> Optional.of(data.seed))
                    ).apply(instance, RetoldEndSkyData::new))
            );

    private long seed = generateSeed();

    public RetoldEndSkyData() {
    }

    private RetoldEndSkyData(Optional<Long> savedSeed) {
        if (savedSeed.isPresent()) {
            this.seed = savedSeed.get();
            return;
        }

        this.seed = generateSeed();
        setDirty();
    }

    public long getSeed() {
        return seed;
    }

    public void randomizeSeed() {
        this.seed = generateSeed();
        setDirty();
    }

    public static RetoldEndSkyData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    private static long generateSeed() {
        return ThreadLocalRandom.current().nextLong();
    }
}