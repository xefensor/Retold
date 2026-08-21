package cz.xefensor.retold.worldgen.fire;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Narrow exceptions that let Nether Remnant fireballs hurt their fire-immune Undead rivals. */
public final class RetoldRemnantFireDamage {
    static final float WILDFIRE_FIREBALL_DAMAGE = 8.0F;

    private RetoldRemnantFireDamage() {
    }

    @SubscribeEvent
    public static void onEntityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        Entity target = event.getEntity();

        boolean isFireImmuneUndeadRival = target.getType() == EntityTypes.WITHER_SKELETON
                || target.getType() == EntityTypes.GHAST;

        if (!isFireImmuneUndeadRival
                || target.isRemoved()
                || target.isInvulnerable()
                || !(event.getSource().getDirectEntity() instanceof SmallFireball)
                || !(event.getSource().getEntity() instanceof Blaze)) {
            return;
        }

        event.setInvulnerable(false);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof SmallFireball)
                || !(event.getSource().getEntity() instanceof Wildfire)) {
            return;
        }

        event.setAmount(Math.max(event.getAmount(), WILDFIRE_FIREBALL_DAMAGE));
    }
}
