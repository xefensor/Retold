package cz.xefensor.retold.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class RetoldVexEvents {
    private static final float DIRECT_HIT_DAMAGE_MULTIPLIER = 0.5F;

    private RetoldVexEvents() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        if (!(source.getDirectEntity() instanceof Vex vex)
                || source.getEntity() != vex) {
            return;
        }

        event.setAmount(event.getAmount() * DIRECT_HIT_DAMAGE_MULTIPLIER);
    }
}
