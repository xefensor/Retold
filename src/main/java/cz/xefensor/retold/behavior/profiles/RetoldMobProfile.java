package cz.xefensor.retold.behavior.profiles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

public record RetoldMobProfile(
        RetoldMobProfileType type,
        boolean managed,
        boolean predator,
        boolean packSocial,
        boolean territoryGuard,
        int hungerIntervalTicks,
        int eatThreshold,
        int huntThreshold
) {
    private static final int DISABLED_THRESHOLD = 101;

    private static final Codec<RetoldMobProfile> RAW_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RetoldMobProfileType.CODEC.fieldOf("type").forGetter(RetoldMobProfile::type),
                    Codec.BOOL.optionalFieldOf("managed", false).forGetter(RetoldMobProfile::managed),
                    Codec.BOOL.optionalFieldOf("predator", false).forGetter(RetoldMobProfile::predator),
                    Codec.BOOL.optionalFieldOf("pack_social", false).forGetter(RetoldMobProfile::packSocial),
                    Codec.BOOL.optionalFieldOf("territory_guard", false).forGetter(RetoldMobProfile::territoryGuard),
                    Codec.INT.optionalFieldOf("hunger_interval_ticks", 0).forGetter(RetoldMobProfile::hungerIntervalTicks),
                    Codec.INT.optionalFieldOf("eat_threshold", DISABLED_THRESHOLD).forGetter(RetoldMobProfile::eatThreshold),
                    Codec.INT.optionalFieldOf("hunt_threshold", DISABLED_THRESHOLD).forGetter(RetoldMobProfile::huntThreshold)
            ).apply(instance, RetoldMobProfile::new)
    );

    public static final Codec<RetoldMobProfile> CODEC = RAW_CODEC.flatXmap(
            RetoldMobProfile::validate,
            RetoldMobProfile::validate
    );

    public boolean is(RetoldMobProfileType type) {
        return this.type == type;
    }

    private static DataResult<RetoldMobProfile> validate(RetoldMobProfile profile) {
        return profile.validationError()
                .<DataResult<RetoldMobProfile>>map(message -> DataResult.error(() -> message))
                .orElseGet(() -> DataResult.success(profile));
    }

    public Optional<String> validationError() {
        return RetoldMobProfileValidation.validate(
                type.serializedName(),
                hungerIntervalTicks,
                eatThreshold,
                huntThreshold
        );
    }
}
