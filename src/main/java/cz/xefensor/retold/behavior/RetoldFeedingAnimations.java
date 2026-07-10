package cz.xefensor.retold.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.sheep.Sheep;

final class RetoldFeedingAnimations {
    private static final byte SHEEP_EAT_EVENT = 10;
    private static final byte DOLPHIN_EAT_EVENT = 38;
    private static final byte FOX_EAT_EVENT = 45;

    private RetoldFeedingAnimations() {
    }

    static void play(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (mob instanceof Sheep) {
            level.broadcastEntityEvent(
                    mob,
                    SHEEP_EAT_EVENT
            );
            return;
        }

        if (mob instanceof Llama llama) {
            llama.setEating(true);
            llama.playSound(
                    SoundEvents.LLAMA_EAT,
                    1.0F,
                    1.0F
            );
            return;
        }

        if (mob instanceof AbstractHorse horse) {
            horse.setEating(true);
            horse.playSound(
                    horse.isBaby() ? SoundEvents.HORSE_EAT_BABY : SoundEvents.HORSE_EAT,
                    1.0F,
                    1.0F
            );
            return;
        }

        if (mob instanceof Dolphin dolphin) {
            dolphin.playSound(
                    SoundEvents.DOLPHIN_EAT,
                    1.0F,
                    1.0F
            );
            level.broadcastEntityEvent(
                    dolphin,
                    DOLPHIN_EAT_EVENT
            );
            return;
        }

        if (mob instanceof Fox fox) {
            fox.playSound(
                    SoundEvents.FOX_EAT,
                    1.0F,
                    1.0F
            );
            level.broadcastEntityEvent(
                    fox,
                    FOX_EAT_EVENT
            );
            return;
        }

        if (mob instanceof Camel camel) {
            playNeutralEatSound(
                    level,
                    camel,
                    SoundEvents.CAMEL_EAT
            );
            return;
        }

        if (mob instanceof Goat goat) {
            playNeutralEatSound(
                    level,
                    goat,
                    goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_EAT : SoundEvents.GOAT_EAT
            );
            return;
        }

        if (mob instanceof Frog frog) {
            playNeutralEatSound(
                    level,
                    frog,
                    SoundEvents.FROG_EAT
            );
        }
    }

    private static void playNeutralEatSound(
            ServerLevel level,
            PathfinderMob mob,
            net.minecraft.sounds.SoundEvent sound
    ) {
        level.playSound(
                null,
                mob,
                sound,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F
        );
    }
}
