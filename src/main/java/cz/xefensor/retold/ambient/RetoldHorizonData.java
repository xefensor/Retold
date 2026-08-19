package cz.xefensor.retold.ambient;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import cz.xefensor.retold.Retold;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.UUID;

final class RetoldHorizonData extends SavedData {
    private static final Codec<RetoldHorizonSchedule.PlayerEntry> PLAYER_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(
                            RetoldHorizonSchedule.PlayerEntry::playerId
                    ),
                    Codec.LONG.fieldOf("next").forGetter(
                            RetoldHorizonSchedule.PlayerEntry::nextCueAt
                    )
            ).apply(instance, RetoldHorizonSchedule.PlayerEntry::new));

    private static final Codec<RetoldHorizonSchedule.SerializedState> STATE_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("version").forGetter(
                            RetoldHorizonSchedule.SerializedState::version
                    ),
                    PLAYER_CODEC.listOf().fieldOf("players").forGetter(
                            RetoldHorizonSchedule.SerializedState::players
                    )
            ).apply(instance, RetoldHorizonSchedule.SerializedState::new));

    private static final SavedDataType<RetoldHorizonData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Retold.MODID, "ambient_horizon"),
            RetoldHorizonData::new,
            STATE_CODEC.xmap(RetoldHorizonData::new, RetoldHorizonData::serialize)
    );

    private final RetoldHorizonSchedule schedule;

    private RetoldHorizonData() {
        schedule = new RetoldHorizonSchedule();
    }

    private RetoldHorizonData(RetoldHorizonSchedule.SerializedState state) {
        schedule = RetoldHorizonSchedule.fromSerializedState(state);
    }

    static RetoldHorizonData get(ServerLevel level) {
        return level.getServer().getDataStorage().computeIfAbsent(TYPE);
    }

    boolean scheduleIfAbsent(UUID playerId, long gameTime, long delayTicks) {
        boolean changed = schedule.scheduleIfAbsent(playerId, gameTime, delayTicks);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    boolean isDue(UUID playerId, long gameTime) {
        return schedule.isDue(playerId, gameTime);
    }

    void reschedule(UUID playerId, long gameTime, long delayTicks) {
        schedule.reschedule(playerId, gameTime, delayTicks);
        setDirty();
    }

    void capDelay(UUID playerId, long gameTime, long maximumDelayTicks) {
        if (schedule.capDelay(playerId, gameTime, maximumDelayTicks)) {
            setDirty();
        }
    }

    private RetoldHorizonSchedule.SerializedState serialize() {
        return schedule.serialize();
    }
}
