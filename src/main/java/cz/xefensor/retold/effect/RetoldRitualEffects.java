package cz.xefensor.retold.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class RetoldRitualEffects {
    private RetoldRitualEffects() {
    }

    public static void playDragonEggStage3Ritual(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;

        level.playSound(
                null,
                pos,
                SoundEvents.END_PORTAL_SPAWN,
                SoundSource.BLOCKS,
                1.5F,
                1.0F
        );

        level.playSound(
                null,
                pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.0F,
                0.7F
        );

        level.sendParticles(
                ParticleTypes.PORTAL,
                x,
                y,
                z,
                120,
                1.0,
                1.0,
                1.0,
                0.25
        );

        level.sendParticles(
                ParticleTypes.END_ROD,
                x,
                y + 0.4,
                z,
                60,
                0.6,
                0.8,
                0.6,
                0.05
        );

        level.sendParticles(
                ParticleTypes.ENCHANT,
                x,
                y,
                z,
                80,
                1.2,
                0.7,
                1.2,
                0.4
        );
    }

    public static void playDragonEggElementAccepted(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.8;
        double z = pos.getZ() + 0.5;

        level.playSound(
                null,
                pos,
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS,
                1.0F,
                1.35F
        );

        level.playSound(
                null,
                pos,
                SoundEvents.TURTLE_EGG_CRACK,
                SoundSource.BLOCKS,
                0.8F,
                0.65F
        );

        level.levelEvent(
                2001,
                pos,
                Block.getId(Blocks.DRAGON_EGG.defaultBlockState())
        );

        level.sendParticles(
                ParticleTypes.SPLASH,
                x,
                y,
                z,
                40,
                0.7,
                0.5,
                0.7,
                0.08
        );

        level.sendParticles(
                ParticleTypes.ENCHANT,
                x,
                y + 0.2,
                z,
                70,
                1.0,
                0.6,
                1.0,
                0.35
        );
    }
}
