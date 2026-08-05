package cz.xefensor.retold.mixin;

import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.species.RetoldCubeMobContactDamage;
import cz.xefensor.retold.behavior.species.RetoldSlimeHungerCombat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobPushMixin {
    @Shadow
    protected abstract boolean isDealsDamage();

    @Shadow
    protected abstract void dealDamage(LivingEntity target);

    @Redirect(
            method = {
                    "push(Lnet/minecraft/world/entity/Entity;)V",
                    "playerTouch(Lnet/minecraft/world/entity/player/Player;)V"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob;isDealsDamage()Z"
            )
    )
    private boolean retold$requireHungerForVanillaContactDamage(
            AbstractCubeMob cubeMob
    ) {
        return retold$canDealContactDamage(cubeMob);
    }

    @Inject(
            method = "push(Lnet/minecraft/world/entity/Entity;)V",
            at = @At("TAIL")
    )
    private void retold$damageCurrentNonPlayerTarget(
            Entity collidedEntity,
            CallbackInfo ci
    ) {
        AbstractCubeMob cubeMob = (AbstractCubeMob) (Object) this;

        if (!retold$canDealContactDamage(cubeMob)) {
            return;
        }

        LivingEntity target = RetoldCubeMobContactDamage.resolveAdditionalContactTarget(
                cubeMob,
                collidedEntity
        );

        if (target != null) {
            dealDamage(target);
        }
    }

    private boolean retold$canDealContactDamage(AbstractCubeMob cubeMob) {
        boolean vanillaDamage = isDealsDamage();
        /*
         * Vanilla makes tiny Slimes harmless. Retold keeps the same dealDamage
         * operation but admits its size-one Cube Mobs once hunger enables combat.
         */
        boolean tinyRetoldCubeMob = cubeMob.isTiny()
                && RetoldMobRules.isSlimeHungry(cubeMob);

        return (vanillaDamage || tinyRetoldCubeMob)
                && !RetoldSlimeHungerCombat.shouldBlockHostility(cubeMob);
    }
}
