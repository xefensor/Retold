package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.performance.RetoldAiSightCache;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;
import java.util.UUID;

public final class RetoldAiTargets {
    private RetoldAiTargets() {
    }

    public static boolean isInvalidPlayerTarget(Entity entity) {
        return entity instanceof Player player
                && (player.isCreative() || player.isSpectator());
    }

    public static boolean isAliveInSameLevel(Entity observer, LivingEntity target) {
        return observer != null
                && target != null
                && target.isAlive()
                && !target.isRemoved()
                && observer.level() == target.level();
    }

    public static boolean isValidAssignmentTarget(Mob mob, LivingEntity target) {
        return isAliveInSameLevel(mob, target)
                && !isInvalidPlayerTarget(target);
    }

    public static boolean isVisibleTo(PathfinderMob mob, LivingEntity target) {
        return isAliveInSameLevel(mob, target)
                && RetoldAiSightCache.canSee(
                mob,
                target,
                mob.level().getGameTime()
        );
    }

    public static void setTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean aggressive
    ) {
        if (mob == null) {
            return;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                target
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                aggressive
        );
    }

    public static void setAggression(
            Mob mob,
            boolean aggressive
    ) {
        if (mob == null) {
            return;
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                aggressive
        );
    }

    public static void clearTargetAndAggression(
            Mob mob,
            LivingEntity target,
            boolean stopNavigation
    ) {
        if (mob == null || target == null) {
            return;
        }

        if (mob.getTarget() == target) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(mob, null);
        }

        clearPiglinBrainTargetIfPresent(mob, target);
        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, false);

        if (stopNavigation && mob instanceof PathfinderMob pathfinderMob) {
            pathfinderMob.getNavigation().stop();
        }
    }

    public static LivingEntity getBrainAttackTargetSafely(Mob mob) {
        try {
            return mob.getBrain()
                    .getMemory(MemoryModuleType.ATTACK_TARGET)
                    .orElse(null);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public static Optional<UUID> getAngryAtSafely(Mob mob) {
        try {
            return mob.getBrain().getMemory(MemoryModuleType.ANGRY_AT);
        } catch (IllegalStateException ignored) {
            return Optional.empty();
        }
    }

    public static void eraseMemorySafely(
            Mob mob,
            MemoryModuleType<?> memoryType
    ) {
        try {
            mob.getBrain().eraseMemory(memoryType);
        } catch (IllegalStateException ignored) {
        }
    }

    public static void setPiglinBrainTargetIfNeeded(
            Mob mob,
            LivingEntity target
    ) {
        if (!(mob instanceof AbstractPiglin piglin)) {
            return;
        }

        if (!isValidAssignmentTarget(mob, target)) {
            return;
        }

        if (piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) != target) {
            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }

        piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT, target.getUUID());
    }

    public static void clearPiglinBrainTargetIfPresent(
            Mob mob,
            LivingEntity target
    ) {
        if (!(mob instanceof AbstractPiglin piglin)) {
            return;
        }

        LivingEntity brainTarget = piglin.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElse(null);

        if (brainTarget != target) {
            return;
        }

        piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);

        if (target != null) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    public static LivingEntity resolveAngryAt(
            ServerLevel level,
            Mob mob
    ) {
        Optional<UUID> angryAt = getAngryAtSafely(mob);

        if (angryAt.isEmpty()) {
            return null;
        }

        Entity entity = level.getEntity(angryAt.get());

        if (entity instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }
}
