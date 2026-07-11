package cz.xefensor.retold.event;

import cz.xefensor.retold.combat.RetoldAiTargets;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldTargetSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class RetoldGuardianDefenseAssist {
    private static final int GUARDIAN_DEFENSE_ALERT_COOLDOWN_TICKS = 20 * 3;
    private static final int GUARDIAN_DEFENSE_PRESSURE_LEVEL = 2;

    private static final Map<UUID, Long> LAST_DEFENSE_ALERT_AT = new HashMap<>();

    private RetoldGuardianDefenseAssist() {
    }

    static void onIncomingDamage(
            LivingIncomingDamageEvent event,
            Guardian guardian
    ) {
        if (guardian instanceof ElderGuardian) {
            return;
        }

        if (!(guardian.level() instanceof ServerLevel level)) {
            return;
        }

        if (!guardian.isAlive() || guardian.isRemoved()) {
            cleanup(guardian);
            return;
        }

        Player attacker = playerAttacker(event.getSource().getEntity());

        if (!isValidAttacker(guardian, attacker)) {
            return;
        }

        if (!RetoldOceanMonumentSupport.isValidMonumentAt(level, guardian.blockPosition())) {
            return;
        }

        long gameTime = level.getGameTime();
        Long lastAlertAt = LAST_DEFENSE_ALERT_AT.get(guardian.getUUID());

        if (lastAlertAt != null && gameTime - lastAlertAt < GUARDIAN_DEFENSE_ALERT_COOLDOWN_TICKS) {
            return;
        }

        LAST_DEFENSE_ALERT_AT.put(
                guardian.getUUID(),
                gameTime
        );

        if (!RetoldCombatTargets.applyAttackTarget(
                guardian,
                attacker,
                RetoldTargetSource.FACTION_ASSIST
        )) {
            return;
        }

        RetoldGuardianAlertController.alertNearby(
                level,
                guardian.blockPosition(),
                attacker,
                GUARDIAN_DEFENSE_PRESSURE_LEVEL
        );
    }

    private static boolean isValidAttacker(
            Guardian guardian,
            Player attacker
    ) {
        if (RetoldAiTargets.isInvalidPlayerTarget(attacker)) {
            return false;
        }

        return attacker.level() == guardian.level();
    }

    private static Player playerAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }

        return null;
    }

    private static void cleanup(Guardian guardian) {
        LAST_DEFENSE_ALERT_AT.remove(guardian.getUUID());
    }
}
