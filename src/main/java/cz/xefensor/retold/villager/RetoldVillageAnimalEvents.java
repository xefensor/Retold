package cz.xefensor.retold.villager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Connects animal provenance, breeding, and witnessed killing. */
public final class RetoldVillageAnimalEvents {
    private RetoldVillageAnimalEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerInteractAnimal(
            PlayerInteractEvent.EntityInteract event
    ) {
        if (!event.isCanceled()
                && event.getTarget() instanceof Animal animal) {
            RetoldVillageAnimalOwnership.markPlayerAssociated(animal);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerInteractAnimalSpecific(
            PlayerInteractEvent.EntityInteractSpecific event
    ) {
        if (!event.isCanceled()
                && event.getTarget() instanceof Animal animal) {
            RetoldVillageAnimalOwnership.markPlayerAssociated(animal);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof Animal child)) {
            return;
        }

        if (event.getCausedByPlayer() != null) {
            RetoldVillageAnimalOwnership.markPlayerBred(child);
            return;
        }

        if (event.getParentA() instanceof Animal parentA
                && event.getParentB() instanceof Animal parentB) {
            if (RetoldVillageAnimalOwnership.isProtectedPlayerAnimal(parentA)
                    || RetoldVillageAnimalOwnership.isProtectedPlayerAnimal(
                            parentB
                    )) {
                RetoldVillageAnimalOwnership.markPlayerBred(child);
            } else if (RetoldVillageAnimalOwnership.isVillageOwned(parentA)
                    && RetoldVillageAnimalOwnership.isVillageOwned(parentB)) {
                RetoldVillageAnimalOwnership.markVillageOwned(child);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVillageAnimalDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Animal animal)
                || !RetoldVillageAnimalOwnership.isVillageOwned(animal)
                || !(animal.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity()
                instanceof ServerPlayer player)) {
            return;
        }

        RetoldVillageWitnessReputation.report(
                level,
                player,
                animal.blockPosition(),
                RetoldVillageWitnessReputation.Offense.ANIMAL_KILLING
        );
    }
}
