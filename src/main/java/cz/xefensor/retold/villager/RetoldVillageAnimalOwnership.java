package cz.xefensor.retold.villager;

import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Persistent provenance for livestock actually tended by a village. */
public final class RetoldVillageAnimalOwnership {
    private static final String VILLAGE_OWNED_KEY =
            "retold:village_owned_animal";
    private static final String PLAYER_ASSOCIATED_KEY =
            "retold:player_associated_animal";

    private RetoldVillageAnimalOwnership() {
    }

    public static boolean isVillageOwned(Animal animal) {
        return animal != null
                && animal.getPersistentData().getBooleanOr(
                VILLAGE_OWNED_KEY,
                false
        );
    }

    public static void markVillageOwned(Animal animal) {
        if (animal == null || !isSupportedLivestock(animal)) {
            return;
        }

        animal.getPersistentData().putBoolean(VILLAGE_OWNED_KEY, true);
    }

    public static void markPlayerAssociated(Animal animal) {
        if (animal == null || !isSupportedLivestock(animal)) {
            return;
        }

        animal.getPersistentData().putBoolean(PLAYER_ASSOCIATED_KEY, true);
    }

    public static boolean isPlayerAssociated(Animal animal) {
        return animal != null
                && animal.getPersistentData().getBooleanOr(
                        PLAYER_ASSOCIATED_KEY,
                        false
                );
    }

    public static boolean isProtectedPlayerAnimal(Animal animal) {
        return isPlayerAssociated(animal) && !isVillageOwned(animal);
    }

    public static void markPlayerBred(Animal animal) {
        if (animal == null || !isSupportedLivestock(animal)) {
            return;
        }

        animal.getPersistentData().remove(VILLAGE_OWNED_KEY);
        markPlayerAssociated(animal);
    }

    public static boolean canBecomeVillageOwned(Animal animal) {
        return animal != null
                && isSupportedLivestock(animal)
                && (isVillageOwned(animal)
                || !isPlayerAssociated(animal));
    }

    public static boolean canTend(Villager villager, Animal animal) {
        if (villager == null
                || animal == null
                || !canBecomeVillageOwned(animal)) {
            return false;
        }

        var profession = villager.getVillagerData().profession();

        if (profession.is(VillagerProfession.SHEPHERD)) {
            return animal instanceof Sheep || animal instanceof Goat;
        }

        if (profession.is(VillagerProfession.LEATHERWORKER)) {
            return animal instanceof AbstractCow;
        }

        if (profession.is(VillagerProfession.BUTCHER)) {
            return animal instanceof Pig
                    || animal instanceof Chicken
                    || animal instanceof Rabbit;
        }

        return false;
    }

    public static ItemStack tendingFood(Animal animal) {
        if (animal instanceof Sheep
                || animal instanceof Goat
                || animal instanceof AbstractCow) {
            return Items.WHEAT.getDefaultInstance();
        }

        if (animal instanceof Pig || animal instanceof Rabbit) {
            return Items.CARROT.getDefaultInstance();
        }

        if (animal instanceof Chicken) {
            return Items.WHEAT_SEEDS.getDefaultInstance();
        }

        return ItemStack.EMPTY;
    }

    public static boolean isSupportedLivestock(Animal animal) {
        return animal instanceof Sheep
                || animal instanceof Goat
                || animal instanceof AbstractCow
                || animal instanceof Pig
                || animal instanceof Chicken
                || animal instanceof Rabbit;
    }
}
