package cz.xefensor.retold.combat;

import cz.xefensor.retold.behavior.species.RetoldSlimeHungerCombat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Guardian;

/**
 * Global rules that must apply regardless of which vanilla or Retold system owns combat.
 */
public final class RetoldMobTargetPolicy {
    private RetoldMobTargetPolicy() {
    }

    public static boolean shouldBlockDeliberateHostility(
            Mob attacker,
            Entity target
    ) {
        return shouldBlockDeliberateHostility(attacker, target, null);
    }

    public static boolean shouldBlockDeliberateHostility(
            Mob attacker,
            Entity target,
            RetoldTargetSource source
    ) {
        if (attacker == null || target == null || attacker == target) {
            return false;
        }

        if (RetoldSlimeHungerCombat.shouldBlockHostility(attacker)) {
            return true;
        }

        if (attacker.getType() == EntityTypes.AXOLOTL
                && target instanceof Guardian) {
            return source != RetoldTargetSource.RETALIATION
                    && source != RetoldTargetSource.FACTION_ASSIST;
        }

        /*
         * A creature attacking a creeper usually creates uncontrolled terrain damage and
         * collateral deaths. Creepers are therefore never valid deliberate mob targets.
         * Players can still attack them normally.
         */
        return target.getType() == EntityTypes.CREEPER;
    }
}
