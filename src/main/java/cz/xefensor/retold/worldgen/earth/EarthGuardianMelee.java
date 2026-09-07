package cz.xefensor.retold.worldgen.earth;

import cz.xefensor.retold.behavior.performance.RetoldAiWorkBudget;
import cz.xefensor.retold.behavior.performance.RetoldBehaviorPerf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Contact combat with the heard player, without acquiring a live navigation target. */
final class EarthGuardianMelee {
    private static final int COOLDOWN_TICKS = 30;
    private static final int CONTACT_CHECK_INTERVAL = 5;
    private final EarthGuardian guardian;
    private int cooldown;

    EarthGuardianMelee(EarthGuardian guardian) {
        this.guardian = guardian;
    }

    void tick(ServerLevel level) {
        if (cooldown > 0) {
            cooldown--;
        }
        if (cooldown > 0 || guardian.tickCount % CONTACT_CHECK_INTERVAL != 0
                || guardian.vibrationSourcePlayer() == null) {
            return;
        }
        var player = level.getEntity(guardian.vibrationSourcePlayer());
        if (player instanceof ServerPlayer) {
            guardian.doHurtTarget(level, player);
        }
    }

    boolean canAttack(ServerLevel level, Entity target) {
        if (cooldown > 0 || level != guardian.level() || !guardian.isAlive()
                || !guardian.canOwnMovement()
                || guardian.lifecycle() != EarthGuardianLifecycle.ROAMING
                || guardian.vibrationTarget() == null
                || !(target instanceof ServerPlayer player)
                || player.level() != level || !player.isAlive()
                || !EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                || !player.getUUID().equals(guardian.vibrationSourcePlayer())
                || !guardian.isWithinMeleeAttackRange(player)) {
            return false;
        }
        // A cached clear view can outlive a newly placed wall. Contact strikes need a fresh,
        // budgeted obstruction check, used only after the cheap source/range/cooldown guards.
        if (!RetoldAiWorkBudget.tryUseSightRaycast(level.getGameTime())) {
            return false;
        }
        RetoldBehaviorPerf.recordSightCache(false);
        return guardian.hasLineOfSight(player);
    }

    void recordAttack() {
        cooldown = COOLDOWN_TICKS;
    }

    void save(ValueOutput output) {
        output.putInt("retold_melee_cooldown", cooldown);
    }

    void load(ValueInput input) {
        cooldown = Math.clamp(input.getIntOr("retold_melee_cooldown", 0), 0, COOLDOWN_TICKS);
    }
}
