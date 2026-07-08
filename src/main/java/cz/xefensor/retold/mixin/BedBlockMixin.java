package cz.xefensor.retold.mixin;

import com.mojang.datafixers.util.Either;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.registry.RetoldGameRules;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public abstract class BedBlockMixin {
    private static final ResourceKey<Level> RETOLD_AENDER =
            ResourceKey.create(
                    Registries.DIMENSION,
                    Identifier.fromNamespaceAndPath(Retold.MODID, "aender")
            );

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void retold$explodeBedsInAender(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(RETOLD_AENDER)) {
            return;
        }

        level.removeBlock(pos, false);
        level.explode(
                null,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                5.0F,
                true,
                Level.ExplosionInteraction.BLOCK
        );

        cir.setReturnValue(InteractionResult.SUCCESS_SERVER);
    }

    @Redirect(
            method = "useWithoutItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;startSleepInBed(Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;"
            )
    )
    private Either<Player.BedSleepingProblem, Unit> retold$startSleepWithoutNightSkipping(
            Player player,
            BlockPos bedPos,
            BlockState state,
            Level level,
            BlockPos clickedPos,
            Player samePlayer,
            BlockHitResult hit
    ) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return player.startSleepInBed(bedPos);
        }

        if (RetoldGameRules.doBedNightSkipping(serverLevel)) {
            return player.startSleepInBed(bedPos);
        }

        ServerPlayer.RespawnConfig newRespawnConfig = new ServerPlayer.RespawnConfig(
                LevelData.RespawnData.of(serverLevel.dimension(), bedPos, serverPlayer.getYRot(), 0.0F),
                false
        );

        ServerPlayer.RespawnConfig oldRespawnConfig = serverPlayer.getRespawnConfig();

        boolean alreadySet =
                oldRespawnConfig != null
                        && !oldRespawnConfig.forced()
                        && oldRespawnConfig.isSamePosition(newRespawnConfig);

        if (alreadySet) {
            serverPlayer.sendOverlayMessage(Component.literal("Respawn point already set"));
        } else {
            serverPlayer.setRespawnPosition(newRespawnConfig, false);
            serverPlayer.sendOverlayMessage(Component.literal("Respawn point set"));
        }

        if (!serverPlayer.isSleeping()) {
            serverPlayer.startSleeping(bedPos);
        }

        CriteriaTriggers.SLEPT_IN_BED.trigger(serverPlayer);

        return Either.right(Unit.INSTANCE);
    }
}