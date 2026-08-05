package cz.xefensor.retold.mixin;

import cz.xefensor.retold.event.RetoldMobAvailability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
    @Redirect(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"
            )
    )
    private Entity retold$filterEndermiteCreation(
            EntityType<?> entityType,
            Level level,
            EntitySpawnReason spawnReason
    ) {
        return RetoldMobAvailability.createEndermiteIfAvailable(
                entityType,
                level,
                spawnReason
        );
    }
}
