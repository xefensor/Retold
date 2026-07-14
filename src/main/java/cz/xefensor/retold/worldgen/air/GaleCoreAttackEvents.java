package cz.xefensor.retold.worldgen.air;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.BreezeWindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class GaleCoreAttackEvents {
    private static final double SPLASH_RADIUS = 2.65D;
    private static final double SPLASH_RADIUS_SQ = SPLASH_RADIUS * SPLASH_RADIUS;
    private static final int MAX_BLOCKS_PER_IMPACT = 12;
    private static final int BREAK_PROGRESS_MAX = 10;
    private static final int CRACK_REFRESH_INTERVAL_TICKS = 40;
    private static final int CRACK_DECAY_TICKS = 220;
    private static final int CRACK_BREAKER_SALT = 0x5A17C0DE;
    private static final float MAX_BREAKABLE_HARDNESS = 8.0F;
    private static final Map<CrackKey, CrackState> CRACKS = new HashMap<>();

    private GaleCoreAttackEvents() {
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof BreezeWindCharge charge)) {
            return;
        }

        if (!(charge.getOwner() instanceof GaleCore)) {
            return;
        }

        if (!(charge.level() instanceof ServerLevel level)) {
            return;
        }

        crackSplash(level, event.getRayTraceResult().getLocation(), charge);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();

        if (source.getDirectEntity() instanceof BreezeWindCharge && source.getEntity() instanceof GaleCore) {
            event.setAmount(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (CRACKS.isEmpty()) {
            return;
        }

        ServerLevel overworld = event.getServer().overworld();

        if (overworld.getGameTime() % CRACK_REFRESH_INTERVAL_TICKS != 0) {
            return;
        }

        Iterator<Map.Entry<CrackKey, CrackState>> iterator = CRACKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<CrackKey, CrackState> entry = iterator.next();
            CrackKey key = entry.getKey();
            ServerLevel level = event.getServer().getLevel(key.dimension());

            if (level == null) {
                iterator.remove();
                continue;
            }

            CrackState crack = entry.getValue();

            if (level.getGameTime() - crack.lastHitTick() > CRACK_DECAY_TICKS || !canCrackBlock(level, key.pos())) {
                level.destroyBlockProgress(breakerId(key), key.pos(), -1);
                iterator.remove();
            }
        }
    }

    private static void crackSplash(ServerLevel level, Vec3 center, Entity breaker) {
        BlockPos origin = BlockPos.containing(center);
        List<BlockImpact> impacts = new ArrayList<>();
        int radius = (int) Math.ceil(SPLASH_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius))) {
            BlockPos immutablePos = pos.immutable();
            double distanceSq = Vec3.atCenterOf(immutablePos).distanceToSqr(center);

            if (distanceSq > SPLASH_RADIUS_SQ || !canCrackBlock(level, immutablePos)) {
                continue;
            }

            impacts.add(new BlockImpact(immutablePos, distanceSq));
        }

        impacts.sort((first, second) -> Double.compare(first.distanceSq(), second.distanceSq()));

        int affected = 0;

        for (BlockImpact impact : impacts) {
            crackBlock(level, impact.pos(), impact.distanceSq(), breaker);

            if (++affected >= MAX_BLOCKS_PER_IMPACT) {
                return;
            }
        }
    }

    private static void crackBlock(ServerLevel level, BlockPos pos, double distanceSq, Entity breaker) {
        CrackKey key = new CrackKey(level.dimension(), pos);
        CrackState current = CRACKS.getOrDefault(key, new CrackState(0, level.getGameTime()));
        int progress = current.progress() + progressGain(level, pos, distanceSq);

        if (progress >= BREAK_PROGRESS_MAX) {
            level.destroyBlockProgress(breakerId(key), pos, -1);
            CRACKS.remove(key);
            level.destroyBlock(pos, false, breaker);
            return;
        }

        CRACKS.put(key, new CrackState(progress, level.getGameTime()));
        level.destroyBlockProgress(breakerId(key), pos, Math.max(0, Math.min(9, progress)));
    }

    private static int progressGain(ServerLevel level, BlockPos pos, double distanceSq) {
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        int gain = hardness <= 1.5F ? 4 : hardness <= 3.0F ? 3 : 2;

        if (distanceSq <= 1.25D) {
            gain++;
        }

        return gain;
    }

    private static boolean canCrackBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !level.getFluidState(pos).isEmpty() || state.hasBlockEntity()) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        return hardness >= 0.0F && hardness <= MAX_BREAKABLE_HARDNESS;
    }

    private static int breakerId(CrackKey key) {
        long mixed = key.pos().asLong() ^ key.dimension().identifier().hashCode() ^ CRACK_BREAKER_SALT;
        return (int) (mixed ^ mixed >>> 32);
    }

    private record BlockImpact(BlockPos pos, double distanceSq) {
    }

    private record CrackKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record CrackState(int progress, long lastHitTick) {
    }
}
