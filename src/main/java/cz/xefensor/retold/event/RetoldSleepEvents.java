package cz.xefensor.retold.event;

import java.util.Objects;

import cz.xefensor.retold.registry.RetoldGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

public final class RetoldSleepEvents {
    private RetoldSleepEvents() {
    }

    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Player.BedSleepingProblem problem = event.getProblem();
        if (problem == null || !isRetoldDayRestProblem(level, event.getPos(), problem)) {
            return;
        }

        if (hasRestPreventingMonster(level, player, event.getPos())) {
            event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
            return;
        }

        event.setProblem(null);
    }

    @SubscribeEvent
    public static void onCanContinueSleeping(CanContinueSleepingEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (event.getProblem() == null || !player.isSleeping()) {
            return;
        }

        player.getSleepingPos()
                .filter(pos -> level.getBlockState(pos).isBed(level, pos, player))
                .filter(pos -> canRetoldRestDuringDay(level, pos))
                .ifPresent(pos -> event.setContinueSleeping(true));
    }

    private static boolean isRetoldDayRestProblem(ServerLevel level, BlockPos pos, Player.BedSleepingProblem problem) {
        if (!canRetoldRestDuringDay(level, pos)) {
            return false;
        }

        Player.BedSleepingProblem bedRuleProblem = bedRule(level, pos).asProblem();
        return Objects.equals(problem.message(), bedRuleProblem.message());
    }

    private static boolean canRetoldRestDuringDay(ServerLevel level, BlockPos pos) {
        if (RetoldGameRules.doBedNightSkipping(level)) {
            return false;
        }

        BedRule bedRule = bedRule(level, pos);
        return bedRule.canSetSpawn(level) && !bedRule.canSleep(level);
    }

    private static BedRule bedRule(Level level, BlockPos pos) {
        return level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, pos);
    }

    private static boolean hasRestPreventingMonster(ServerLevel level, ServerPlayer player, BlockPos bedPos) {
        if (player.isCreative()) {
            return false;
        }

        Vec3 bedCenter = Vec3.atBottomCenterOf(bedPos);
        AABB searchArea = new AABB(
                bedCenter.x() - 8.0,
                bedCenter.y() - 5.0,
                bedCenter.z() - 8.0,
                bedCenter.x() + 8.0,
                bedCenter.y() + 5.0,
                bedCenter.z() + 8.0
        );

        return !level.getEntitiesOfClass(
                Monster.class,
                searchArea,
                monster -> monster.isPreventingPlayerRest(level, player)
        ).isEmpty();
    }
}
