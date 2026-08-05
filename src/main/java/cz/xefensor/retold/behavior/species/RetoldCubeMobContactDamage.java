package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.combat.RetoldMobTargetPolicy;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;

public final class RetoldCubeMobContactDamage {
    private RetoldCubeMobContactDamage() {
    }

    public static LivingEntity resolveAdditionalContactTarget(
            AbstractCubeMob cubeMob,
            Entity collidedEntity
    ) {
        if (cubeMob == null
                || !(collidedEntity instanceof LivingEntity livingTarget)
                || collidedEntity instanceof Player
                || collidedEntity instanceof IronGolem
                || cubeMob.getTarget() != livingTarget
                || RetoldMobTargetPolicy.shouldBlockDeliberateHostility(
                        cubeMob,
                        livingTarget
                )
                || !RetoldFactionRelations.shouldAttack(cubeMob, livingTarget)) {
            return null;
        }

        return livingTarget;
    }
}
