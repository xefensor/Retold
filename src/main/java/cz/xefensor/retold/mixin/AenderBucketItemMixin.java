package cz.xefensor.retold.mixin;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class AenderBucketItemMixin {
    @Shadow
    @Final
    public Fluid content;

    @Inject(
            method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void retold$preventAenderLavaPlacement(
            LivingEntity user,
            Level level,
            BlockPos pos,
            BlockHitResult hitResult,
            ItemStack containerItem,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (level.dimension() != RetoldAenderDimensions.AENDER || !content.is(FluidTags.LAVA)) {
            return;
        }

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        RandomSource random = level.getRandom();
        level.playSound(
                user,
                pos,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.5F,
                2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F
        );

        for (int i = 0; i < 8; i++) {
            level.addParticle(
                    ParticleTypes.LARGE_SMOKE,
                    x + random.nextFloat(),
                    y + random.nextFloat(),
                    z + random.nextFloat(),
                    0.0D,
                    0.0D,
                    0.0D
            );
        }

        cir.setReturnValue(true);
    }
}
