package cz.xefensor.retold.territory;

public final class RetoldTerritoryConstants {
    public static final double NOTICE_MOB_RADIUS_BLOCKS = 48.0D;
    public static final double ILLEGAL_ACTION_WITNESS_RADIUS_BLOCKS = NOTICE_MOB_RADIUS_BLOCKS;
    public static final double ATTACK_CHAIN_RADIUS_BLOCKS = NOTICE_MOB_RADIUS_BLOCKS;

    public static final double ATTACK_TARGET_RELEASE_DISTANCE_BLOCKS = 40.0D;
    public static final double ATTACK_TARGET_RELEASE_DISTANCE_SQUARED =
            ATTACK_TARGET_RELEASE_DISTANCE_BLOCKS * ATTACK_TARGET_RELEASE_DISTANCE_BLOCKS;

    public static final int WARNING_TARGET_RECHECK_INTERVAL_TICKS = 35;
    public static final int WARNING_LOST_SIGHT_MEMORY_TICKS = 100;
    public static final int WARNING_MIN_FINAL_WARNING_TICKS_BEFORE_ATTACK = 40;

    public static final int WARNING_NONE_PULSE_INTERVAL_TICKS = 140;
    public static final int WARNING_NOTICED_PULSE_INTERVAL_TICKS = 130;
    public static final int WARNING_WARNING_PULSE_INTERVAL_TICKS = 95;
    public static final int WARNING_FINAL_WARNING_PULSE_INTERVAL_TICKS = 60;
    public static final int WARNING_ATTACK_PULSE_INTERVAL_TICKS = 40;

    public static final int WARNING_NOTICED_SUSPICION_GAIN = 3;
    public static final int WARNING_WARNING_SUSPICION_GAIN = 8;
    public static final int WARNING_FINAL_WARNING_SUSPICION_GAIN = 13;

    public static final double WARNING_TOO_CLOSE_DISTANCE_BLOCKS = 4.0D;
    public static final double WARNING_TOO_CLOSE_DISTANCE_SQUARED =
            WARNING_TOO_CLOSE_DISTANCE_BLOCKS * WARNING_TOO_CLOSE_DISTANCE_BLOCKS;

    public static final int WARNING_FORMATION_RECHECK_INTERVAL_TICKS = 45;

    public static final double WARNING_POSITION_REPATH_DISTANCE_BLOCKS = 2.5D;
    public static final double WARNING_POSITION_REPATH_DISTANCE_SQUARED =
            WARNING_POSITION_REPATH_DISTANCE_BLOCKS * WARNING_POSITION_REPATH_DISTANCE_BLOCKS;

    public static final double WARNING_TARGET_DRIFT_REPATH_DISTANCE_BLOCKS = 4.0D;
    public static final double WARNING_TARGET_DRIFT_REPATH_DISTANCE_SQUARED =
            WARNING_TARGET_DRIFT_REPATH_DISTANCE_BLOCKS * WARNING_TARGET_DRIFT_REPATH_DISTANCE_BLOCKS;

    public static final double WARNING_POSITION_STOP_DISTANCE_BLOCKS = 1.75D;
    public static final double WARNING_POSITION_STOP_DISTANCE_SQUARED =
            WARNING_POSITION_STOP_DISTANCE_BLOCKS * WARNING_POSITION_STOP_DISTANCE_BLOCKS;

    public static final int WARNING_MIN_REPATH_INTERVAL_TICKS = 30;

    public static final int WARNING_CROSSBOW_CHARGE_TICKS = 28;

    public static final int BASE_WARNING_PARTICLE_COUNT = 2;
    public static final int WARNING_PARTICLE_COUNT_PER_INTENSITY = 2;

    public static final double WARNING_PARTICLE_SPREAD_X = 0.35D;
    public static final double WARNING_PARTICLE_SPREAD_Y = 0.25D;
    public static final double WARNING_PARTICLE_SPREAD_Z = 0.35D;
    public static final double WARNING_PARTICLE_SPEED = 0.01D;
    public static final double WARNING_PARTICLE_EYE_OFFSET_Y = 0.25D;

    public static final float BASE_WARNING_SOUND_VOLUME = 0.35F;
    public static final float WARNING_SOUND_VOLUME_PER_INTENSITY = 0.12F;
    public static final float WARNING_SOUND_PITCH = 0.85F;

    public static final int REPUTATION_MAX_SUSPICION = 160;

    public static final int REPUTATION_NOTICED_THRESHOLD = 5;
    public static final int REPUTATION_WARNING_THRESHOLD = 25;
    public static final int REPUTATION_FINAL_WARNING_THRESHOLD = 65;
    public static final int REPUTATION_ATTACK_THRESHOLD = 110;

    public static final int REPUTATION_TRESPASS_SUSPICION_GAIN = 8;
    public static final int REPUTATION_VISIBLE_WARNING_DEFAULT_SUSPICION_GAIN = 8;
    public static final int REPUTATION_TOO_CLOSE_SUSPICION_GAIN = 6;
    public static final int REPUTATION_STEALING_SUSPICION_GAIN = 55;
    public static final int REPUTATION_BLOCK_BREAK_SUSPICION_GAIN = 35;
    public static final int REPUTATION_ATTACK_SUSPICION_GAIN = 110;
    public static final int REPUTATION_KILL_SUSPICION_GAIN = 140;

    public static final int REPUTATION_TRESPASS_SUSPICION_COOLDOWN_TICKS = 20 * 18;
    public static final int REPUTATION_VISIBLE_WARNING_SUSPICION_COOLDOWN_TICKS = 20 * 4;
    public static final int REPUTATION_TOO_CLOSE_SUSPICION_COOLDOWN_TICKS = 20 * 4;

    public static final int REPUTATION_SEEN_DECAY_BLOCK_TICKS = 20 * 8;
    public static final int REPUTATION_DECAY_INTERVAL_TICKS = 40;
    public static final int REPUTATION_DECAY_AMOUNT = 1;

    public static final long REPUTATION_INITIAL_COOLDOWN_TIME = -999999L;

    public static final int REPUTATION_SAVE_VERSION = 1;
    public static final int REPUTATION_SAVE_INTERVAL_TICKS = 20 * 10;
    public static final String REPUTATION_SAVE_DIRECTORY = "retold";
    public static final String REPUTATION_SAVE_FILE_NAME = "territory_reputation.json";

    public static final boolean DEBUG_REPUTATION = false;
    public static final int DEBUG_REPUTATION_INTERVAL_TICKS = 10;

    private RetoldTerritoryConstants() {
    }
}