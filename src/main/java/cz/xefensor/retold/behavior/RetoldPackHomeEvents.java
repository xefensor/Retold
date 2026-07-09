package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RetoldPackHomeEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER = RetoldAiControlOwner.REGROUP;
    private static final String REASON_RETURN_HOME = "return_home";
    private static final String REASON_DEN_IDLE = "den_idle";
    private static final String REASON_DEN_DEFENSE = "den_defense";

    private static final int THINK_INTERVAL_TICKS = 20;
    private static final int HOME_RETURN_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_CONTROL_TICKS = 20 * 5;
    private static final int HOME_IDLE_PRIORITY = 10;
    private static final int HOME_IDLE_MOVE_INTERVAL_TICKS = 20 * 6;
    private static final int DEN_DEFENSE_CONTROL_TICKS = 20 * 4;
    private static final int DEN_DEFENSE_PRIORITY = 86;
    private static final int MAX_PASSIVE_DEN_MEMBERS = 4;

    private static final double HOME_CLOSE_DISTANCE_BLOCKS = 5.0D;
    private static final double HOME_CLOSE_DISTANCE_SQUARED =
            HOME_CLOSE_DISTANCE_BLOCKS * HOME_CLOSE_DISTANCE_BLOCKS;

    private static final double HOME_IDLE_RETURN_DISTANCE_BLOCKS = 28.0D;
    private static final double HOME_IDLE_RETURN_DISTANCE_SQUARED =
            HOME_IDLE_RETURN_DISTANCE_BLOCKS * HOME_IDLE_RETURN_DISTANCE_BLOCKS;

    private static final double WOLF_DEN_IDLE_RADIUS_BLOCKS = 8.0D;
    private static final double WOLF_DEN_IDLE_RADIUS_SQUARED =
            WOLF_DEN_IDLE_RADIUS_BLOCKS * WOLF_DEN_IDLE_RADIUS_BLOCKS;

    private static final double WOLF_PASSIVE_DEN_RADIUS_BLOCKS = 18.0D;
    private static final double WOLF_PASSIVE_DEN_RADIUS_SQUARED =
            WOLF_PASSIVE_DEN_RADIUS_BLOCKS * WOLF_PASSIVE_DEN_RADIUS_BLOCKS;

    private static final double WOLF_DEN_DEFENSE_RADIUS_BLOCKS = 20.0D;
    private static final double WOLF_DEN_DEFENSE_RADIUS_SQUARED =
            WOLF_DEN_DEFENSE_RADIUS_BLOCKS * WOLF_DEN_DEFENSE_RADIUS_BLOCKS;

    private static final double WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS = 28.0D;
    private static final double WOLF_DEN_DEFENSE_JOIN_RADIUS_SQUARED =
            WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS * WOLF_DEN_DEFENSE_JOIN_RADIUS_BLOCKS;

    private static final double WOLF_HOME_RETURN_SPEED = 0.82D;
    private static final double DOLPHIN_HOME_RETURN_SPEED = 1.02D;
    private static final double WOLF_DEN_IDLE_STROLL_SPEED = 0.62D;
    private static final double WOLF_DEN_DEFENSE_SPEED = 1.10D;

    private RetoldPackHomeEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!shouldThink(mob, level.getGameTime())) {
            return;
        }

        if (!isHomeReturningMob(mob)) {
            return;
        }

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
            home = tryCreatePassiveDenHome(
                    level,
                    mob,
                    level.getGameTime()
            );

            if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
                return;
            }
        }

        updateHomeReturn(
                level,
                mob,
                home,
                level.getGameTime()
        );
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        int offset = Math.floorMod(
                mob.getId(),
                THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean isHomeReturningMob(PathfinderMob mob) {
        if (mob == null || !mob.isAlive() || mob.isRemoved()) {
            return false;
        }

        if (!RetoldMobRules.isPackSocialHunter(mob)) {
            return false;
        }

        return !(mob instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame());
    }

    private static RetoldAnimalHomeMemory tryCreatePassiveDenHome(
            ServerLevel level,
            PathfinderMob wolf,
            long gameTime
    ) {
        if (!canCreatePassiveDenHome(wolf, gameTime)) {
            return null;
        }

        AABB area = wolf.getBoundingBox().inflate(WOLF_PASSIVE_DEN_RADIUS_BLOCKS);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isPassiveDenCandidate(
                        level,
                        wolf,
                        candidate,
                        gameTime
                )
        );

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(
                Comparator.comparingDouble(candidate -> wolf.distanceToSqr(candidate))
        );

        List<PathfinderMob> members = new ArrayList<>();

        for (PathfinderMob candidate : candidates) {
            if (members.size() >= MAX_PASSIVE_DEN_MEMBERS - 1) {
                break;
            }

            members.add(candidate);
        }

        return RetoldAnimalHomes.getOrCreatePackHome(
                level,
                wolf,
                members,
                calculateHomeCenter(wolf, members),
                gameTime
        );
    }

    private static boolean canCreatePassiveDenHome(
            PathfinderMob wolf,
            long gameTime
    ) {
        if (!isWolf(wolf)) {
            return false;
        }

        if (wolf.getTarget() != null && wolf.getTarget().isAlive()) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(wolf);

        if (mode != RetoldAiControlMode.NONE) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                wolf,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(wolf, state);
    }

    private static boolean isPassiveDenCandidate(
            ServerLevel level,
            PathfinderMob leader,
            PathfinderMob candidate,
            long gameTime
    ) {
        if (leader == null || candidate == null || leader == candidate) {
            return false;
        }

        if (!isWolf(candidate)) {
            return false;
        }

        if (!isHomeReturningMob(candidate)) {
            return false;
        }

        if (leader.level() != candidate.level()) {
            return false;
        }

        if (leader.distanceToSqr(candidate) > WOLF_PASSIVE_DEN_RADIUS_SQUARED) {
            return false;
        }

        if (candidate.getTarget() != null && candidate.getTarget().isAlive()) {
            return false;
        }

        RetoldAiControlMode candidateMode = RetoldAiControl.getMode(candidate);

        if (
                candidateMode != RetoldAiControlMode.NONE
                        && !RetoldAiControl.isControlledAsBy(
                        candidate,
                        RetoldAiControlMode.REGROUP,
                        CONTROL_OWNER
                )
        ) {
            return false;
        }

        RetoldAnimalHomeMemory candidateHome = RetoldAnimalHomes.get(candidate);

        if (
                candidateHome != null
                        && !RetoldAnimalHomes.isValidFor(
                        level,
                        candidate,
                        candidateHome
                )
        ) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                candidate,
                gameTime
        );

        return !RetoldMobRules.hasHuntDrive(candidate, state);
    }

    private static BlockPos calculateHomeCenter(
            PathfinderMob leader,
            List<PathfinderMob> members
    ) {
        double x = leader.getX();
        double y = leader.getY();
        double z = leader.getZ();
        int count = 1;

        for (PathfinderMob member : members) {
            x += member.getX();
            y += member.getY();
            z += member.getZ();
            count++;
        }

        return new BlockPos(
                (int) Math.floor(x / count),
                (int) Math.floor(y / count),
                (int) Math.floor(z / count)
        ).immutable();
    }

    private static void updateHomeReturn(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);
        RetoldAiControlOwner owner = RetoldAiControl.getOwner(mob);

        if (!canUseHomeReturnControl(mob, mode, owner)) {
            return;
        }

        if (tryDefendHome(level, mob, home, mode, owner, gameTime)) {
            return;
        }

        double distanceSquared = mob.blockPosition().distSqr(home.pos());

        if (shouldIdleAtHome(mob, home, mode, owner, distanceSquared, gameTime)) {
            idleAtHome(
                    mob,
                    home.pos(),
                    distanceSquared,
                    gameTime
            );
            return;
        }

        if (distanceSquared <= HOME_CLOSE_DISTANCE_SQUARED) {
            stopHomeReturn(mob, gameTime);
            return;
        }

        if (
                mode == RetoldAiControlMode.NONE
                        && distanceSquared < HOME_IDLE_RETURN_DISTANCE_SQUARED
        ) {
            return;
        }

        moveHome(
                level,
                mob,
                home.pos(),
                gameTime
        );
    }

    private static boolean canUseHomeReturnControl(
            PathfinderMob mob,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner
    ) {
        if (mode == RetoldAiControlMode.NONE) {
            LivingEntity target = mob.getTarget();
            return target == null || !target.isAlive();
        }

        if (mode != RetoldAiControlMode.REGROUP) {
            return false;
        }

        return owner == RetoldAiControlOwner.SYSTEM
                || owner == CONTROL_OWNER;
    }

    private static void moveHome(
            ServerLevel level,
            PathfinderMob mob,
            BlockPos homePos,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                21,
                REASON_RETURN_HOME,
                gameTime,
                HOME_RETURN_CONTROL_TICKS
        )) {
            return;
        }

        clearCombatState(mob);

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        double speed = getHomeReturnSpeed(mob);

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    homePos.getX() + 0.5D,
                    homePos.getY(),
                    homePos.getZ() + 0.5D,
                    speed
            );
        });
    }

    private static void stopHomeReturn(
            PathfinderMob mob,
            long gameTime
    ) {
        if (RetoldAiControl.isControlledBy(mob, CONTROL_OWNER)) {
            RetoldAiControl.clear(mob);
        }

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        clearCombatState(mob);
        mob.getNavigation().stop();
    }

    private static void clearCombatState(PathfinderMob mob) {
        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                false
        );

        RetoldPredatorStrike.clear(mob);
        mob.setSprinting(false);
    }

    private static double getHomeReturnSpeed(PathfinderMob mob) {
        String path = RetoldMobRules.getEntityTypePath(
                mob.getType()
        );

        if (path.equals("dolphin")) {
            return DOLPHIN_HOME_RETURN_SPEED;
        }

        return WOLF_HOME_RETURN_SPEED;
    }

    private static boolean isWolf(PathfinderMob mob) {
        if (mob == null) {
            return false;
        }

        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        ).equals("wolf");
    }

    private static boolean tryDefendHome(
            ServerLevel level,
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            long gameTime
    ) {
        if (home.type() != RetoldAnimalHomeType.WOLF_DEN) {
            return false;
        }

        if (!canStartDenDefense(mob, mode, owner)) {
            return false;
        }

        if (mob.blockPosition().distSqr(home.pos()) > WOLF_DEN_DEFENSE_JOIN_RADIUS_SQUARED) {
            return false;
        }

        LivingEntity enemy = findDenEnemy(
                level,
                mob,
                home.pos()
        );

        if (enemy == null) {
            return false;
        }

        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.ATTACK,
                RetoldAiControlOwner.COMBAT,
                DEN_DEFENSE_PRIORITY,
                REASON_DEN_DEFENSE,
                gameTime,
                DEN_DEFENSE_CONTROL_TICKS
        )) {
            return false;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                enemy
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                true
        );

        mob.getLookControl().setLookAt(
                enemy,
                30.0F,
                30.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    enemy,
                    WOLF_DEN_DEFENSE_SPEED
            );
        });

        return true;
    }

    private static boolean canStartDenDefense(
            PathfinderMob mob,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner
    ) {
        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        if (mode != RetoldAiControlMode.REGROUP) {
            return false;
        }

        if (owner == RetoldAiControlOwner.SYSTEM) {
            return true;
        }

        return owner == CONTROL_OWNER
                && (
                REASON_DEN_IDLE.equals(RetoldAiControl.getReason(mob))
                        || REASON_RETURN_HOME.equals(RetoldAiControl.getReason(mob))
        );
    }

    private static LivingEntity findDenEnemy(
            ServerLevel level,
            PathfinderMob wolf,
            BlockPos homePos
    ) {
        AABB area = new AABB(
                homePos
        ).inflate(WOLF_DEN_DEFENSE_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidDenEnemy(
                        wolf,
                        homePos,
                        candidate
                )
        );

        LivingEntity bestEnemy = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceFromHome = candidate.blockPosition().distSqr(homePos);

            if (distanceFromHome > WOLF_DEN_DEFENSE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceFromHome;

            if (wolf.hasLineOfSight(candidate)) {
                score -= 12.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestEnemy = candidate;
            }
        }

        return bestEnemy;
    }

    private static boolean isValidDenEnemy(
            PathfinderMob wolf,
            BlockPos homePos,
            LivingEntity candidate
    ) {
        if (wolf == null || homePos == null || candidate == null) {
            return false;
        }

        if (wolf == candidate) {
            return false;
        }

        if (!candidate.isAlive() || candidate.isRemoved()) {
            return false;
        }

        if (wolf.level() != candidate.level()) {
            return false;
        }

        if (candidate instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        if (candidate.blockPosition().distSqr(homePos) > WOLF_DEN_DEFENSE_RADIUS_SQUARED) {
            return false;
        }

        if (!RetoldMobRules.isWolfEnemyButNotFood(candidate)) {
            return false;
        }

        return wolf.hasLineOfSight(candidate)
                || wolf.distanceToSqr(candidate) <= 36.0D
                || candidate.blockPosition().distSqr(homePos) <= 64.0D;
    }

    private static boolean shouldIdleAtHome(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            RetoldAiControlMode mode,
            RetoldAiControlOwner owner,
            double distanceSquared,
            long gameTime
    ) {
        if (home.type() != RetoldAnimalHomeType.WOLF_DEN) {
            return false;
        }

        if (distanceSquared > WOLF_DEN_IDLE_RADIUS_SQUARED) {
            return false;
        }

        if (mob.getTarget() != null && mob.getTarget().isAlive()) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        if (RetoldMobRules.hasHuntDrive(mob, state)) {
            return false;
        }

        if (mode == RetoldAiControlMode.NONE) {
            return true;
        }

        return mode == RetoldAiControlMode.REGROUP
                && owner == CONTROL_OWNER
                && REASON_DEN_IDLE.equals(RetoldAiControl.getReason(mob));
    }

    private static void idleAtHome(
            PathfinderMob mob,
            BlockPos homePos,
            double distanceSquared,
            long gameTime
    ) {
        if (!RetoldAiControl.tryClaim(
                mob,
                RetoldAiControlMode.REGROUP,
                CONTROL_OWNER,
                HOME_IDLE_PRIORITY,
                REASON_DEN_IDLE,
                gameTime,
                HOME_IDLE_CONTROL_TICKS
        )) {
            return;
        }

        clearCombatState(mob);

        RetoldAnimalHomes.markUsed(
                mob,
                gameTime
        );

        if (distanceSquared > HOME_CLOSE_DISTANCE_SQUARED) {
            RetoldAiControl.withNavigationBypass(() -> {
                mob.getNavigation().moveTo(
                        homePos.getX() + 0.5D,
                        homePos.getY(),
                        homePos.getZ() + 0.5D,
                        WOLF_HOME_RETURN_SPEED
                );
            });
            return;
        }

        if ((gameTime + mob.getId()) % HOME_IDLE_MOVE_INTERVAL_TICKS == 0L) {
            moveToRandomDenPoint(
                    mob,
                    homePos
            );
            return;
        }

        mob.getNavigation().stop();
    }

    private static void moveToRandomDenPoint(
            PathfinderMob mob,
            BlockPos homePos
    ) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = 2.0D + mob.getRandom().nextDouble() * 4.0D;
        double x = homePos.getX() + 0.5D + Math.cos(angle) * distance;
        double z = homePos.getZ() + 0.5D + Math.sin(angle) * distance;

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    x,
                    homePos.getY(),
                    z,
                    WOLF_DEN_IDLE_STROLL_SPEED
            );
        });
    }
}
