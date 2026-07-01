package cz.xefensor.retold.aender;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class RetoldAenderTerrainData extends SavedData {
    public static final SavedDataType<RetoldAenderTerrainData> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            Retold.MODID,
                            "aender_terrain_data"
                    ),
                    RetoldAenderTerrainData::new,
                    RecordCodecBuilder.create(instance -> instance.group(
                            Codec.LONG
                                    .optionalFieldOf("revision", 0L)
                                    .forGetter(RetoldAenderTerrainData::getRevision),

                            Codec.LONG
                                    .optionalFieldOf("salt", 0L)
                                    .forGetter(RetoldAenderTerrainData::getSalt)
                    ).apply(instance, RetoldAenderTerrainData::new))
            );

    private long revision = 0L;
    private long salt = 0L;

    public RetoldAenderTerrainData() {
    }

    private RetoldAenderTerrainData(
            long revision,
            long salt
    ) {
        this.revision = Math.max(0L, revision);
        this.salt = salt;
    }

    public long getRevision() {
        return revision;
    }

    public long getSalt() {
        return salt;
    }

    public long bumpTerrain(long newSalt) {
        revision++;

        if (revision <= 0L) {
            revision = 1L;
        }

        salt = newSalt == 0L ? 1L : newSalt;
        setDirty();

        return revision;
    }

    public static RetoldAenderTerrainData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }
}