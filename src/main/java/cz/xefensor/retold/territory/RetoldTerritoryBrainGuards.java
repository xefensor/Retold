package cz.xefensor.retold.territory;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

public final class RetoldTerritoryBrainGuards {
    private static final ThreadLocal<Deque<Mob>> CURRENT_MOB_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private RetoldTerritoryBrainGuards() {
    }

    public static void pushCurrentMob(Mob mob) {
        if (mob == null) {
            return;
        }

        CURRENT_MOB_STACK.get().push(mob);
    }

    public static void popCurrentMob(Mob mob) {
        Deque<Mob> stack = CURRENT_MOB_STACK.get();

        if (stack.isEmpty()) {
            CURRENT_MOB_STACK.remove();
            return;
        }

        if (stack.peek() == mob) {
            stack.pop();
        } else {
            stack.remove(mob);
        }

        if (stack.isEmpty()) {
            CURRENT_MOB_STACK.remove();
        }
    }

    public static boolean shouldBlockDirectMemoryWrite(
            MemoryModuleType<?> memoryType,
            Object memoryValue
    ) {
        if (memoryValue == null) {
            return false;
        }

        return shouldBlockMemoryWrite(memoryType, memoryValue);
    }

    public static boolean shouldBlockOptionalMemoryWrite(
            MemoryModuleType<?> memoryType,
            Optional<?> memoryValue
    ) {
        if (memoryValue == null || memoryValue.isEmpty()) {
            return false;
        }

        return shouldBlockMemoryWrite(memoryType, memoryValue.get());
    }

    private static boolean shouldBlockMemoryWrite(
            MemoryModuleType<?> memoryType,
            Object memoryValue
    ) {
        if (memoryType == null || memoryValue == null) {
            return false;
        }

        if (
                memoryType != MemoryModuleType.ATTACK_TARGET
                        && memoryType != MemoryModuleType.ANGRY_AT
        ) {
            return false;
        }

        Mob currentMob = getCurrentMob();

        if (!(currentMob instanceof PathfinderMob mob)) {
            return false;
        }

        if (!(mob instanceof AbstractPiglin)) {
            return false;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }

        LivingEntity target = resolveMemoryTarget(level, memoryType, memoryValue);

        if (target == null) {
            return false;
        }

        if (!RetoldTerritoryTargetBlocker.shouldBlockTargetDuringWarning(mob, target)) {
            return false;
        }

        blockPiglinAttackState(mob, target);
        return true;
    }

    private static Mob getCurrentMob() {
        Deque<Mob> stack = CURRENT_MOB_STACK.get();

        if (stack.isEmpty()) {
            return null;
        }

        return stack.peek();
    }

    private static LivingEntity resolveMemoryTarget(
            ServerLevel level,
            MemoryModuleType<?> memoryType,
            Object memoryValue
    ) {
        if (memoryType == MemoryModuleType.ATTACK_TARGET) {
            if (memoryValue instanceof LivingEntity livingEntity) {
                return livingEntity;
            }

            return null;
        }

        if (memoryType == MemoryModuleType.ANGRY_AT) {
            if (!(memoryValue instanceof UUID uuid)) {
                return null;
            }

            Entity entity = level.getEntity(uuid);

            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }

        return null;
    }

    private static void blockPiglinAttackState(
            PathfinderMob mob,
            LivingEntity target
    ) {
        if (mob.getTarget() == target) {
            RetoldFactionTargetGuards.setTargetIgnoringGuard(mob, null);
        }

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(mob, false);
        mob.getNavigation().stop();
    }
}