package cz.xefensor.retold.chronolith;

public final class ChronolithTuning {
    public static final double CHANNEL_RANGE_BLOCKS = 8.0D;
    public static final double MAX_DISTANCE_SQR = CHANNEL_RANGE_BLOCKS * CHANNEL_RANGE_BLOCKS;

    public static final int TIME_TICKS_PER_SERVER_TICK = 16;
    public static final int RAMP_UP_TICKS = 30;
    public static final float ADDED_TIME_TICKS_PER_XP = 40.0F;

    public static final boolean CLEAR_WEATHER_ON_START = true;
    public static final int ACTIVE_SOUND_INTERVAL_TICKS = 20;
    public static final int ACTIVE_PARTICLE_INTERVAL_TICKS = 4;

    private ChronolithTuning() {
    }
}
