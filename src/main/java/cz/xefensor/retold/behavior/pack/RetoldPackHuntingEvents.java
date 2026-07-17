package cz.xefensor.retold.behavior.pack;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.core.RetoldBehaviorTiming;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldPackHuntingEvents {
    private static final int PACK_THINK_INTERVAL_TICKS = 10;

    private RetoldPackHuntingEvents() {
    }

    public static String debugPackText(PathfinderMob mob) {
        return RetoldPackHuntingDebug.buildText(mob);
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!RetoldPackAnimals.isPackHunter(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        PathfinderMob currentLeader = RetoldPackParties.leaderOf(mob);

        if (currentLeader != null && currentLeader != mob) {
            RetoldPackParty memberParty = RetoldPackParties.partyOf(currentLeader);

            if (memberParty == null || !RetoldPackAnimals.isValidPackAnimal(currentLeader)) {
                RetoldPackLifecycle.releaseMember(mob);
                return;
            }

            RetoldPackUpdates.updateMember(
                    level,
                    currentLeader,
                    mob,
                    memberParty,
                    gameTime
            );

            return;
        }

        RetoldPackParty party = RetoldPackParties.partyOf(mob);

        if (party != null) {
            RetoldPackUpdates.updateLeaderParty(
                    level,
                    mob,
                    party,
                    gameTime
            );

            return;
        }

        if (shouldStartHuntingParty(mob)) {
            createHuntingParty(
                    level,
                    mob,
                    gameTime
            );
        }
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        return RetoldBehaviorTiming.shouldThink(
                mob,
                gameTime,
                PACK_THINK_INTERVAL_TICKS
        );
    }

    private static boolean shouldStartHuntingParty(PathfinderMob leader) {
        if (!RetoldPackAnimals.isValidPackAnimal(leader)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(leader);

        return mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT;
    }

    private static void createHuntingParty(
            ServerLevel level,
            PathfinderMob leader,
            long gameTime
    ) {
        RetoldPackParty party = RetoldPackFormation.tryCreateHuntingParty(
                level,
                leader,
                gameTime
        );

        if (party == null) {
            return;
        }

        RetoldPackUpdates.updateLeaderParty(
                level,
                leader,
                party,
                gameTime
        );
    }

}
