package cz.xefensor.retold.worldgen.fire;

import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;

/** Restricts Wildfire encounter combat without narrowing the whole Nether Remnant faction. */
public final class WildfireEncounterTargets {
    private static final String ESCORT_MARKER = "retold_wildfire_escort";

    private WildfireEncounterTargets() {
    }

    static void markEscort(Blaze blaze) {
        if (blaze != null) {
            blaze.getPersistentData().putBoolean(ESCORT_MARKER, true);
        }
    }

    public static boolean isEncounterMember(Mob mob) {
        return mob instanceof Wildfire
                || mob instanceof Blaze blaze
                && blaze.getPersistentData().getBooleanOr(ESCORT_MARKER, false);
    }

    public static boolean isAllowedTarget(Entity target) {
        return target instanceof Player || RetoldFactionMembers.isUndead(target);
    }

    public static boolean shouldBlockTarget(Mob attacker, Entity target) {
        return attacker != null
                && target != null
                && isEncounterMember(attacker)
                && !isAllowedTarget(target);
    }
}
