package cz.xefensor.retold.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class RetoldSnowballEvents {
    private static final float SNOWBALL_DAMAGE = 1.0F;

    private RetoldSnowballEvents() {
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        if (!(source.getDirectEntity() instanceof Snowball)
                || event.getEntity() instanceof Blaze) {
            return;
        }

        event.setAmount(SNOWBALL_DAMAGE);
    }
}
