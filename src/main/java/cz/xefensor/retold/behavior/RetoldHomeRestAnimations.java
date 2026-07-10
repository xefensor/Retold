package cz.xefensor.retold.behavior;

import cz.xefensor.retold.mixin.FoxInvoker;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.wolf.Wolf;

final class RetoldHomeRestAnimations {
    private RetoldHomeRestAnimations() {
    }

    static void startResting(PathfinderMob mob) {
        if (mob instanceof Camel camel) {
            if (camel.canCamelChangePose()) {
                camel.sitDown();
            }
            return;
        }

        if (mob instanceof Cat cat) {
            cat.setLying(true);
            return;
        }

        if (mob instanceof Wolf wolf) {
            wolf.setInSittingPose(true);
            return;
        }

        if (mob instanceof Fox fox) {
            fox.setSitting(false);
            fox.setIsCrouching(false);
            fox.setIsInterested(false);
            ((FoxInvoker) fox).retold$setSleeping(true);
        }
    }

    static void stopResting(PathfinderMob mob) {
        if (mob instanceof Camel camel) {
            if (camel.canCamelChangePose()) {
                camel.standUp();
            }
            return;
        }

        if (mob instanceof Cat cat) {
            cat.setLying(false);
            return;
        }

        if (mob instanceof Wolf wolf) {
            wolf.setInSittingPose(false);
            return;
        }

        if (mob instanceof Fox fox) {
            ((FoxInvoker) fox).retold$setSleeping(false);
        }
    }
}
