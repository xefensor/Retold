package cz.xefensor.retold.client;

import cz.xefensor.retold.aender.RetoldAenderDimensions;
import cz.xefensor.retold.client.render.RetoldEndermanEyesLayer;
import cz.xefensor.retold.client.render.RetoldChronolithBeamClient;
import cz.xefensor.retold.client.sky.RetoldClientEndSky;
import cz.xefensor.retold.client.sky.RetoldEndSkyPatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RetoldClientEvents {
    private static final DustParticleOptions AENDER_GREEN_PARTICLE =
            new DustParticleOptions(0x6CA983, 0.30F);
    private static final DustParticleOptions AENDER_BRIGHT_GREEN_PARTICLE =
            new DustParticleOptions(0x9FC694, 0.34F);
    private static final DustParticleOptions AENDER_CYAN_GREEN_PARTICLE =
            new DustParticleOptions(0x6CB6AA, 0.32F);

    private RetoldClientEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RetoldClientEvents::addEntityRenderLayers);
        RetoldChronolithBeamClient.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(RetoldClientEvents::onClientTick);
    }

    private static void addEntityRenderLayers(EntityRenderersEvent.AddLayers event) {
        EntityRenderer renderer = event.getRenderer(EntityTypes.ENDERMAN);

        if (renderer instanceof EndermanRenderer endermanRenderer) {
            endermanRenderer.addLayer(new RetoldEndermanEyesLayer(endermanRenderer));
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null) {
            return;
        }

        if (minecraft.level.dimension() == Level.END) {
            RetoldClientEndSky.getTexture();
            RetoldEndSkyPatcher.patchSkyRendererTexture(RetoldClientEndSky.getSeed());
        }

        if (minecraft.level.dimension() == RetoldAenderDimensions.AENDER && minecraft.player != null) {
            spawnAenderEnergyParticles(minecraft);
        }
    }

    private static void spawnAenderEnergyParticles(Minecraft minecraft) {
        RandomSource random = minecraft.level.getRandom();
        long gameTime = minecraft.level.getGameTime();

        if ((gameTime & 1L) != 0L) {
            return;
        }

        for (int i = 0; i < 1; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 5.0D + random.nextDouble() * 16.0D;
            double x = minecraft.player.getX() + Math.cos(angle) * distance;
            double y = minecraft.player.getY() + random.nextDouble() * 8.0D - 1.5D;
            double z = minecraft.player.getZ() + Math.sin(angle) * distance;

            double phase = random.nextDouble() * Math.PI * 2.0D;
            double eddy =
                    gameTime * 0.014D +
                            x * 0.047D +
                            y * 0.018D -
                            z * 0.039D +
                            phase;
            double slowWave =
                    gameTime * 0.006D +
                            x * 0.015D +
                            z * 0.017D +
                            phase;

            double velocityX = Math.sin(eddy) * 0.006D + Math.cos(slowWave) * 0.003D;
            double velocityY = Math.sin(slowWave + y * 0.041D) * 0.004D;
            double velocityZ = Math.cos(eddy) * 0.006D + Math.sin(slowWave) * 0.003D;

            minecraft.level.addParticle(
                    pickAenderAmbientParticle(random),
                    x,
                    y,
                    z,
                    velocityX,
                    velocityY,
                    velocityZ
            );
        }
    }

    private static DustParticleOptions pickAenderAmbientParticle(RandomSource random) {
        double roll = random.nextDouble();

        if (roll < 0.12D) {
            return AENDER_CYAN_GREEN_PARTICLE;
        }

        if (roll < 0.36D) {
            return AENDER_BRIGHT_GREEN_PARTICLE;
        }

        return AENDER_GREEN_PARTICLE;
    }
}
