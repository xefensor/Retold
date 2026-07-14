package cz.xefensor.retold.event;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RetoldGuardianAlertController {
    private static final double MONUMENT_BLOCK_ALERT_RADIUS = 48.0D;
    private static final double MONUMENT_BLOCK_ALERT_RADIUS_SQR =
            MONUMENT_BLOCK_ALERT_RADIUS * MONUMENT_BLOCK_ALERT_RADIUS;
    private static final int GUARDIAN_ALERT_REFRESH_COOLDOWN_TICKS = 20 * 4;
    private static final int GUARDIAN_MINING_ALERT_DURATION_TICKS = 16 * 20;
    private static final int GUARDIAN_ALERT_DURATION_INCREASE_PER_PRESSURE_LEVEL_TICKS = 6 * 20;
    private static final int GUARDIAN_ALERT_PATH_REFRESH_TICKS = 10;
    private static final int BASE_FORCED_PATHING_GUARDIANS = 1;
    private static final int MAX_FORCED_PATHING_GUARDIANS = 2;
    private static final double GUARDIAN_ALERT_PATH_SPEED = 1.25D;
    private static final double GUARDIAN_ALERT_PATH_SPEED_INCREASE_PER_PRESSURE_LEVEL = 0.08D;

    private static final int GUARDIAN_APPROACH_RECALCULATE_TICKS = 20;
    private static final int GUARDIAN_APPROACH_MIN_RADIUS = 2;
    private static final int GUARDIAN_APPROACH_MAX_RADIUS = 8;
    private static final int GUARDIAN_APPROACH_RADIUS_STEP = 2;
    private static final int GUARDIAN_APPROACH_MIN_Y_OFFSET = -1;
    private static final int GUARDIAN_APPROACH_MAX_Y_OFFSET = 2;
    private static final int[][] GUARDIAN_APPROACH_DIRECTIONS = {
            {1, 0},
            {1, 1},
            {0, 1},
            {-1, 1},
            {-1, 0},
            {-1, -1},
            {0, -1},
            {1, -1}
    };
    private static final List<BlockPos> GUARDIAN_APPROACH_OFFSETS = buildGuardianApproachOffsets();

    private static final Map<UUID, GuardianAlert> GUARDIAN_ALERTS = new HashMap<>();
    private static final Map<UUID, GuardianApproach> GUARDIAN_APPROACHES = new HashMap<>();
    private static final Map<UUID, Long> LAST_ALERT_REFRESH_AT_BY_PLAYER = new HashMap<>();

    private RetoldGuardianAlertController() {
    }

    static void alertNearby(ServerLevel level, BlockPos pos, Player player, int pressureLevel) {
        if (!canRefreshAlert(level, player)) {
            return;
        }

        int guardianLimit = forcedPathingGuardianLimit(pressureLevel);
        AABB alertBounds = AABB.ofSize(
                Vec3.atCenterOf(pos),
                MONUMENT_BLOCK_ALERT_RADIUS * 2.0D,
                MONUMENT_BLOCK_ALERT_RADIUS * 2.0D,
                MONUMENT_BLOCK_ALERT_RADIUS * 2.0D
        );
        List<Guardian> guardians = level.getEntitiesOfClass(Guardian.class, alertBounds, Guardian::isAlive);
        List<Guardian> nearestGuardians = new ArrayList<>(guardianLimit);

        for (Guardian guardian : guardians) {
            addNearestGuardian(nearestGuardians, guardian, player, guardianLimit);
        }

        for (Guardian guardian : nearestGuardians) {
            alertGuardian(level, guardian, player, pressureLevel);
        }
    }

    static void onGuardianTick(Guardian guardian) {
        if (!(guardian.level() instanceof ServerLevel level)) {
            return;
        }

        GuardianAlert alert = GUARDIAN_ALERTS.get(guardian.getUUID());

        if (alert == null) {
            return;
        }

        if (!guardian.isAlive() || level.getGameTime() > alert.expiresAt()) {
            removeAlert(guardian.getUUID());
            return;
        }

        if (!(level.getEntity(alert.playerId()) instanceof ServerPlayer player)
                || !RetoldAiTargets.isValidAssignmentTarget(guardian, player)) {
            removeAlert(guardian.getUUID());
            return;
        }

        if (guardian.distanceToSqr(player) > MONUMENT_BLOCK_ALERT_RADIUS_SQR) {
            removeAlert(guardian.getUUID());
            return;
        }

        if (guardian.hasLineOfSight(player)) {
            if (!RetoldCombatTargets.applyAttackTarget(
                    guardian,
                    player,
                    RetoldTargetSource.FACTION_ASSIST
            )) {
                removeAlert(guardian.getUUID());
                return;
            }
        } else if (guardian.getTarget() == player) {
            RetoldAiTargets.setTargetAndAggression(guardian, null, false);
        }

        if (guardian.tickCount % GUARDIAN_ALERT_PATH_REFRESH_TICKS == 0 || guardian.getNavigation().isDone()) {
            moveGuardianTowardAlertTarget(level, guardian, player, alert.pathSpeed());
        }
    }

    static void removeAlert(UUID guardianId) {
        GUARDIAN_ALERTS.remove(guardianId);
        GUARDIAN_APPROACHES.remove(guardianId);
    }

    private static void alertGuardian(ServerLevel level, Guardian guardian, Player player, int pressureLevel) {
        double pathSpeed = guardianAlertPathSpeed(pressureLevel);

        GUARDIAN_ALERTS.put(
                guardian.getUUID(),
                new GuardianAlert(
                        player.getUUID(),
                        level.getGameTime() + guardianAlertDurationTicks(pressureLevel),
                        pathSpeed
                )
        );

        if (guardian.hasLineOfSight(player)) {
            if (!RetoldCombatTargets.applyAttackTarget(
                    guardian,
                    player,
                    RetoldTargetSource.FACTION_ASSIST
            )) {
                removeAlert(guardian.getUUID());
                return;
            }
        }

        moveGuardianTowardAlertTarget(level, guardian, player, pathSpeed);
    }

    private static void addNearestGuardian(
            List<Guardian> nearestGuardians,
            Guardian candidate,
            Player player,
            int guardianLimit
    ) {
        double candidateDistance = candidate.distanceToSqr(player);
        int insertAt = 0;

        while (insertAt < nearestGuardians.size()
                && nearestGuardians.get(insertAt).distanceToSqr(player) <= candidateDistance) {
            insertAt++;
        }

        if (insertAt >= guardianLimit) {
            return;
        }

        nearestGuardians.add(insertAt, candidate);

        if (nearestGuardians.size() > guardianLimit) {
            nearestGuardians.remove(nearestGuardians.size() - 1);
        }
    }

    private static boolean canRefreshAlert(ServerLevel level, Player player) {
        long gameTime = level.getGameTime();
        UUID playerId = player.getUUID();
        Long lastAlertRefreshAt = LAST_ALERT_REFRESH_AT_BY_PLAYER.get(playerId);

        if (lastAlertRefreshAt != null
                && gameTime - lastAlertRefreshAt < GUARDIAN_ALERT_REFRESH_COOLDOWN_TICKS) {
            return false;
        }

        LAST_ALERT_REFRESH_AT_BY_PLAYER.put(playerId, gameTime);
        return true;
    }

    private static int forcedPathingGuardianLimit(int pressureLevel) {
        return Math.min(
                MAX_FORCED_PATHING_GUARDIANS,
                BASE_FORCED_PATHING_GUARDIANS + Math.max(0, pressureLevel - 1)
        );
    }

    private static int guardianAlertDurationTicks(int pressureLevel) {
        return GUARDIAN_MINING_ALERT_DURATION_TICKS
                + (pressureLevel - 1) * GUARDIAN_ALERT_DURATION_INCREASE_PER_PRESSURE_LEVEL_TICKS;
    }

    private static double guardianAlertPathSpeed(int pressureLevel) {
        return GUARDIAN_ALERT_PATH_SPEED
                + (pressureLevel - 1) * GUARDIAN_ALERT_PATH_SPEED_INCREASE_PER_PRESSURE_LEVEL;
    }

    private static void moveGuardianTowardAlertTarget(
            ServerLevel level,
            Guardian guardian,
            Player player,
            double pathSpeed
    ) {
        if (guardian.hasLineOfSight(player)) {
            guardian.getNavigation().moveTo(player, pathSpeed);
            return;
        }

        Vec3 approachPosition = getGuardianApproachPosition(level, guardian, player);
        guardian.getNavigation().moveTo(approachPosition.x, approachPosition.y, approachPosition.z, pathSpeed);
    }

    private static Vec3 getGuardianApproachPosition(ServerLevel level, Guardian guardian, Player player) {
        long gameTime = level.getGameTime();
        BlockPos playerPos = player.blockPosition();
        GuardianApproach cachedApproach = GUARDIAN_APPROACHES.get(guardian.getUUID());

        if (cachedApproach != null
                && gameTime < cachedApproach.expiresAt()
                && cachedApproach.playerPos().equals(playerPos)) {
            return cachedApproach.position();
        }

        Vec3 approachPosition = findGuardianApproachPosition(level, guardian, player, playerPos);
        GUARDIAN_APPROACHES.put(
                guardian.getUUID(),
                new GuardianApproach(playerPos, approachPosition, gameTime + GUARDIAN_APPROACH_RECALCULATE_TICKS)
        );
        return approachPosition;
    }

    private static Vec3 findGuardianApproachPosition(
            ServerLevel level,
            Guardian guardian,
            Player player,
            BlockPos playerPos
    ) {
        Vec3 playerCenter = player.position();
        Vec3 playerEye = eyePosition(player);
        Vec3 guardianPosition = guardian.position();
        Vec3 bestPosition = null;
        double bestScore = Double.MAX_VALUE;

        for (BlockPos offset : GUARDIAN_APPROACH_OFFSETS) {
            BlockPos candidatePos = playerPos.offset(offset);

            if (!canGuardianApproach(level, candidatePos)) {
                continue;
            }

            Vec3 candidateCenter = Vec3.atCenterOf(candidatePos);
            Vec3 candidateView = candidateCenter.add(0.0D, 0.35D, 0.0D);

            if (!hasClearLine(level, candidateView, playerEye, guardian)) {
                continue;
            }

            double distanceFromGuardian = candidateCenter.distanceToSqr(guardianPosition);
            double distanceFromPlayer = candidateCenter.distanceToSqr(playerCenter);
            double score = distanceFromGuardian + distanceFromPlayer * 0.35D;

            if (score < bestScore) {
                bestScore = score;
                bestPosition = candidateCenter;
            }
        }

        return bestPosition != null ? bestPosition : playerCenter;
    }

    private static List<BlockPos> buildGuardianApproachOffsets() {
        List<BlockPos> offsets = new ArrayList<>();

        for (int radius = GUARDIAN_APPROACH_MIN_RADIUS;
             radius <= GUARDIAN_APPROACH_MAX_RADIUS;
             radius += GUARDIAN_APPROACH_RADIUS_STEP) {
            for (int[] direction : GUARDIAN_APPROACH_DIRECTIONS) {
                for (int dy = GUARDIAN_APPROACH_MIN_Y_OFFSET; dy <= GUARDIAN_APPROACH_MAX_Y_OFFSET; dy++) {
                    offsets.add(new BlockPos(direction[0] * radius, dy, direction[1] * radius));
                }
            }
        }

        return List.copyOf(offsets);
    }

    private static boolean canGuardianApproach(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());

        return state.getCollisionShape(level, pos).isEmpty()
                && aboveState.getCollisionShape(level, pos.above()).isEmpty();
    }

    private static boolean hasClearLine(Level level, Vec3 from, Vec3 to, Entity viewer) {
        return level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                viewer
        )).getType() == HitResult.Type.MISS;
    }

    private static Vec3 eyePosition(Entity entity) {
        return new Vec3(entity.getX(), entity.getEyeY(), entity.getZ());
    }

    private record GuardianAlert(UUID playerId, long expiresAt, double pathSpeed) {
    }

    private record GuardianApproach(BlockPos playerPos, Vec3 position, long expiresAt) {
    }
}
