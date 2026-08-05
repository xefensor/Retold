package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;

import java.util.ArrayList;
import java.util.List;

public final class RetoldSlimeStarvationBehavior {
    static final int CRITICAL_HUNGER = 100;

    private static final int SPLIT_CHILD_COUNT = 2;

    private RetoldSlimeStarvationBehavior() {
    }

    public static int hungerGain(PathfinderMob mob) {
        if (mob instanceof AbstractCubeMob cubeMob
                && RetoldMobRules.isSlimeHungry(cubeMob)) {
            return Math.max(1, (cubeMob.getSize() + 1) / 2);
        }

        return 1;
    }

    public static boolean tryApplyCriticalHunger(
            ServerLevel level,
            PathfinderMob mob,
            RetoldMobState state,
            long gameTime
    ) {
        if (level == null
                || !(mob instanceof AbstractCubeMob cubeMob)
                || !RetoldMobRules.isSlimeHungry(cubeMob)
                || state == null
                || state.hunger() < CRITICAL_HUNGER) {
            return false;
        }

        if (cubeMob.getSize() <= AbstractCubeMob.MIN_SIZE) {
            RetoldAiControl.clear(cubeMob);
            cubeMob.getNavigation().stop();
            cubeMob.kill(level);
            return true;
        }

        return splitFromHunger(
                cubeMob,
                gameTime
        );
    }

    private static boolean splitFromHunger(
            AbstractCubeMob parent,
            long gameTime
    ) {
        int childSize = Math.max(
                AbstractCubeMob.MIN_SIZE,
                parent.getSize() / 2
        );
        float width = parent.getDimensions(parent.getPose()).width();
        float horizontalOffset = width / 4.0F;
        ConversionParams conversion = new ConversionParams(
                ConversionType.SPLIT_ON_DEATH,
                false,
                false,
                parent.getTeam()
        );
        List<AbstractCubeMob> children = new ArrayList<>(SPLIT_CHILD_COUNT);

        for (int index = 0; index < SPLIT_CHILD_COUNT; index++) {
            float xOffset = index == 0 ? -horizontalOffset : horizontalOffset;
            AbstractCubeMob child = parent.convertTo(
                    parent.getType(),
                    conversion,
                    EntitySpawnReason.TRIGGERED,
                    spawned -> {
                        spawned.setSize(childSize, true);
                        spawned.snapTo(
                                parent.getX() + xOffset,
                                parent.getY() + 0.5D,
                                parent.getZ(),
                                parent.getRandom().nextFloat() * 360.0F,
                                0.0F
                        );
                    }
            );

            if (child == null) {
                for (AbstractCubeMob createdChild : children) {
                    createdChild.discard();
                }

                return false;
            }

            children.add(child);
        }

        /*
         * The vanilla split conversion retains normal identity and presentation data.
         * Retold hunger and swallowed storage are reassigned explicitly so exact item
         * components survive once instead of being copied into every child.
         */
        for (AbstractCubeMob child : children) {
            RetoldSlimeItemStorage.clearStorage(child);
            RetoldSlimeSplitBehavior.inheritHalfHunger(
                    parent,
                    child,
                    gameTime
            );
            RetoldSlimeMergeBehavior.startCooldown(child, gameTime);
        }

        RetoldSlimeItemStorage.transfer(parent, children.getFirst());
        RetoldAiControl.clear(parent);
        RetoldMobStates.remove(parent);
        parent.discard();
        return true;
    }
}
