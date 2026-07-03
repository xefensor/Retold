package cz.xefensor.retold.event;

import cz.xefensor.retold.stage.RetoldWorldData;
import cz.xefensor.retold.stage.RetoldWorldStage;
import cz.xefensor.retold.undead.RetoldUndeadCleansing;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class RetoldPiglinEvents {
    private static final Identifier PIGLIN = Identifier.fromNamespaceAndPath("minecraft", "piglin");
    private static final Identifier PIGLIN_BRUTE = Identifier.fromNamespaceAndPath("minecraft", "piglin_brute");
    private static final Identifier ZOMBIFIED_PIGLIN = Identifier.fromNamespaceAndPath("minecraft", "zombified_piglin");

    private RetoldPiglinEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            return;
        }

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();

        if (stage != RetoldWorldStage.STAGE_3) {
            return;
        }

        if (isZombifiedPiglin(entity)) {
            RetoldUndeadCleansing.cleanse(entity);
            return;
        }

        makeImmuneToZombification(entity);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        ServerLevel serverLevel = event.getLevel().getLevel();
        RetoldWorldStage stage = RetoldWorldData.get(serverLevel).getStage();
        Entity entity = event.getEntity();

        if (stage == RetoldWorldStage.STAGE_3) {
            if (isZombifiedPiglin(entity) && shouldBlockZombifiedPiglinSpawn(event.getSpawnType())) {
                event.setSpawnCancelled(true);
                return;
            }

            makeImmuneToZombification(entity);
            return;
        }

        // Stage 1 and 2: block piglins/brutes from natural + structure/worldgen spawning.
        if (isLivingPiglin(entity) && shouldBlockPreStage3PiglinSpawn(event.getSpawnType())) {
            event.setSpawnCancelled(true);
        }
    }

    private static boolean shouldBlockPreStage3PiglinSpawn(EntitySpawnReason spawnReason) {
        return spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.CHUNK_GENERATION
                || spawnReason == EntitySpawnReason.STRUCTURE;
    }

    private static boolean shouldBlockZombifiedPiglinSpawn(EntitySpawnReason spawnReason) {
        return spawnReason == EntitySpawnReason.NATURAL
                || spawnReason == EntitySpawnReason.CHUNK_GENERATION
                || spawnReason == EntitySpawnReason.STRUCTURE;
    }

    private static boolean isLivingPiglin(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return PIGLIN.equals(id)
                || PIGLIN_BRUTE.equals(id);
    }

    private static boolean isZombifiedPiglin(Entity entity) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());

        return ZOMBIFIED_PIGLIN.equals(id);
    }

    private static void makeImmuneToZombification(Entity entity) {
        if (entity instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
    }
}