package cz.xefensor.retold.event;

import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldElderGuardianEvents {
    private RetoldElderGuardianEvents() {
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        RetoldGuardianMiningPressure.onBreakBlock(event);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        RetoldGuardianMiningPressure.onBreakSpeed(event);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        RetoldElderGuardianBoss.onEntityJoinLevel(event);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && event.getSource().getEntity() instanceof Guardian guardian) {
            RetoldGuardianMiningPressure.handleGuardianDamage(event, player, guardian);
            return;
        }

        if (event.getEntity() instanceof Guardian guardian) {
            RetoldGuardianDefenseAssist.onIncomingDamage(event, guardian);
        }

        if (event.getEntity() instanceof ElderGuardian elderGuardian) {
            RetoldElderGuardianSentinel.onIncomingDamage(event, elderGuardian);
            RetoldElderGuardianBoss.onIncomingDamage(event, elderGuardian);
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Guardian guardian) {
            RetoldGuardianMiningPressure.onGuardianTick(guardian);
        }

        if (event.getEntity() instanceof ElderGuardian elderGuardian) {
            RetoldElderGuardianBoss.onElderGuardianTick(elderGuardian);
            RetoldElderGuardianSentinel.onElderGuardianTick(elderGuardian);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ElderGuardian elderGuardian) {
            RetoldElderGuardianBoss.onLivingDrops(event, elderGuardian);
        }
    }
}
