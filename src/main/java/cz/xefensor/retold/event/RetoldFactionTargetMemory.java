package cz.xefensor.retold.event;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldFactionTargetMemory {
    private static final Map<Mob, TargetOwnership> OWNED_TARGETS = Collections.synchronizedMap(
            new WeakHashMap<>()
    );

    private RetoldFactionTargetMemory() {
    }

    public static boolean trySetTarget(Mob mob, LivingEntity target, RetoldTargetSource source) {
        if (target == null) {
            return false;
        }

        if (!target.isAlive()) {
            return false;
        }

        if (mob.level() != target.level()) {
            return false;
        }

        cleanupTargetState(mob);

        LivingEntity currentTarget = mob.getTarget();
        TargetOwnership ownership = OWNED_TARGETS.get(mob);

        if (currentTarget != null && currentTarget != target && isUsefulCurrentTarget(mob, currentTarget)) {
            if (ownership == null) {
                if (!canOverrideUnownedTarget(source)) {
                    return false;
                }
            } else if (ownership.target == currentTarget && ownership.source.priority() > source.priority()) {
                return false;
            }
        }

        OWNED_TARGETS.put(mob, new TargetOwnership(target, source));

        if (currentTarget != target) {
            RetoldFactionTargetGuards.setTargetIgnoringWarning(mob, target);
        }

        mob.setAggressive(true);
        return true;
    }

    public static boolean clearTargetIfOwnedBy(Mob mob, LivingEntity target, RetoldTargetSource source) {
        cleanupTargetState(mob);

        TargetOwnership ownership = OWNED_TARGETS.get(mob);

        if (ownership == null) {
            return false;
        }

        if (ownership.target != target) {
            return false;
        }

        if (ownership.source != source) {
            return false;
        }

        OWNED_TARGETS.remove(mob);

        if (mob.getTarget() == target) {
            RetoldFactionTargetGuards.setTargetIgnoringWarning(mob, null);
        }

        mob.setAggressive(false);
        clearPiglinBrainIfTarget(mob, target);

        return true;
    }

    public static boolean clearTargetIfOwnedByAny(
            Mob mob,
            LivingEntity target,
            RetoldTargetSource firstSource,
            RetoldTargetSource secondSource
    ) {
        return clearTargetIfOwnedBy(mob, target, firstSource)
                || clearTargetIfOwnedBy(mob, target, secondSource);
    }

    public static boolean isOwnedBy(Mob mob, LivingEntity target, RetoldTargetSource source) {
        cleanupTargetState(mob);

        TargetOwnership ownership = OWNED_TARGETS.get(mob);

        return ownership != null
                && ownership.target == target
                && ownership.source == source;
    }

    public static void cleanupTargetState(Mob mob) {
        TargetOwnership ownership = OWNED_TARGETS.get(mob);
        LivingEntity currentTarget = mob.getTarget();

        if (ownership != null) {
            boolean targetChanged = currentTarget != ownership.target;
            boolean targetInvalid = !isUsefulCurrentTarget(mob, ownership.target);

            if (targetChanged || targetInvalid) {
                OWNED_TARGETS.remove(mob);
                clearPiglinBrainIfTarget(mob, ownership.target);

                if (currentTarget == null || currentTarget == ownership.target || targetInvalid) {
                    mob.setAggressive(false);
                }
            }
        }

        // Safety cleanup for illagers: if our systems left them aggressive
        // but they currently have no target, lower the weapon.
        if (currentTarget == null && RetoldFactionMembers.getFaction(mob) == RetoldFaction.ILLAGERS) {
            mob.setAggressive(false);
        }
    }

    private static boolean canOverrideUnownedTarget(RetoldTargetSource source) {
        return source.priority() >= RetoldTargetSource.TERRITORY_ATTACK.priority();
    }

    private static boolean isUsefulCurrentTarget(Mob mob, LivingEntity target) {
        return target.isAlive() && mob.level() == target.level();
    }

    private static void clearPiglinBrainIfTarget(Mob mob, LivingEntity target) {
        if (!(mob instanceof AbstractPiglin)) {
            return;
        }

        AbstractPiglin piglin = (AbstractPiglin) mob;

        if (piglin.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == target) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }

        if (piglin.getBrain().getMemory(MemoryModuleType.ANGRY_AT).orElse(null) == target.getUUID()) {
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        }
    }

    private static final class TargetOwnership {
        private final LivingEntity target;
        private final RetoldTargetSource source;

        private TargetOwnership(LivingEntity target, RetoldTargetSource source) {
            this.target = target;
            this.source = source;
        }
    }
}