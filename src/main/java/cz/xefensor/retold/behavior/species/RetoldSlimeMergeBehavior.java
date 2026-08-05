package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

public final class RetoldSlimeMergeBehavior {
    static final int MERGE_COOLDOWN_TICKS = 20 * 30;

    private static final String COOLDOWN_UNTIL_KEY = "RetoldSlimeMergeCooldownUntil";
    private static final int MAX_NATURAL_MERGE_SIZE = 4;
    private static final int SCAN_CACHE_TICKS = 6;
    private static final double MERGE_SCAN_RADIUS_BLOCKS = 2.0D;
    private static final double CONTACT_PADDING_BLOCKS = 0.25D;

    private RetoldSlimeMergeBehavior() {
    }

    public static boolean tryMerge(
            ServerLevel level,
            AbstractCubeMob slime,
            long gameTime
    ) {
        if (level == null || !canParticipate(slime, gameTime)) {
            return false;
        }

        for (AbstractCubeMob candidate : RetoldAiScanCache.nearby(
                level,
                slime,
                AbstractCubeMob.class,
                MERGE_SCAN_RADIUS_BLOCKS,
                gameTime,
                SCAN_CACHE_TICKS
        )) {
            if (!canMergePair(slime, candidate, gameTime)) {
                continue;
            }

            AbstractCubeMob survivor = chooseSurvivor(slime, candidate);

            if (survivor != slime) {
                continue;
            }

            mergeInto(
                    survivor,
                    candidate,
                    gameTime
            );
            return true;
        }

        return false;
    }

    public static boolean isOnCooldown(AbstractCubeMob slime, long gameTime) {
        if (slime == null) {
            return false;
        }

        long cooldownUntil = slime.getPersistentData()
                .getLong(COOLDOWN_UNTIL_KEY)
                .orElse(0L);

        return gameTime < cooldownUntil;
    }

    public static void startCooldown(AbstractCubeMob slime, long gameTime) {
        if (slime != null) {
            slime.getPersistentData().putLong(
                    COOLDOWN_UNTIL_KEY,
                    gameTime + MERGE_COOLDOWN_TICKS
            );
        }
    }

    private static boolean canMergePair(
            AbstractCubeMob first,
            AbstractCubeMob second,
            long gameTime
    ) {
        if (first == null || second == null || first == second) {
            return false;
        }

        if (!canParticipate(second, gameTime)) {
            return false;
        }

        if (first.level() != second.level()
                || first.getType() != second.getType()
                || first.getSize() != second.getSize()) {
            return false;
        }

        return first.getBoundingBox()
                .inflate(CONTACT_PADDING_BLOCKS)
                .intersects(second.getBoundingBox());
    }

    private static boolean canParticipate(AbstractCubeMob slime, long gameTime) {
        if (slime == null
                || !slime.isAlive()
                || slime.isRemoved()
                || slime.getSize() >= MAX_NATURAL_MERGE_SIZE
                || slime.getTarget() != null
                || slime.hasCustomName()
                || slime.isPassenger()
                || slime.isVehicle()
                || slime instanceof Leashable leashable && leashable.isLeashed()) {
            return false;
        }

        return RetoldAiControl.getMode(slime) == RetoldAiControlMode.NONE
                && !isOnCooldown(slime, gameTime);
    }

    private static AbstractCubeMob chooseSurvivor(
            AbstractCubeMob first,
            AbstractCubeMob second
    ) {
        return first.getId() <= second.getId() ? first : second;
    }

    private static void mergeInto(
            AbstractCubeMob survivor,
            AbstractCubeMob absorbed,
            long gameTime
    ) {
        float combinedHealthFraction = combinedHealthFraction(survivor, absorbed);
        int combinedHunger = combinedHunger(survivor, absorbed, gameTime);

        RetoldSlimeItemStorage.transfer(absorbed, survivor);
        RetoldAiControl.clear(absorbed);
        RetoldMobStates.remove(absorbed);
        absorbed.discard();

        survivor.setSize(
                survivor.getSize() * 2,
                true
        );
        survivor.setHealth(survivor.getMaxHealth() * combinedHealthFraction);
        RetoldSlimeItemStorage.applyPendingGrowth(survivor);
        RetoldMobStates.getOrCreate(survivor, gameTime).setHunger(combinedHunger);
        startCooldown(survivor, gameTime);
    }

    private static float combinedHealthFraction(
            AbstractCubeMob first,
            AbstractCubeMob second
    ) {
        float combinedMaximum = first.getMaxHealth() + second.getMaxHealth();

        if (combinedMaximum <= 0.0F) {
            return 1.0F;
        }

        return Math.clamp(
                (first.getHealth() + second.getHealth()) / combinedMaximum,
                0.0F,
                1.0F
        );
    }

    private static int combinedHunger(
            AbstractCubeMob first,
            AbstractCubeMob second,
            long gameTime
    ) {
        RetoldMobState firstState = RetoldMobStates.getOrCreate(first, gameTime);
        RetoldMobState secondState = RetoldMobStates.getOrCreate(second, gameTime);

        return Math.round((firstState.hunger() + secondState.hunger()) / 2.0F);
    }
}
