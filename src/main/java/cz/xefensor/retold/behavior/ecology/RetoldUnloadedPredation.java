package cz.xefensor.retold.behavior.ecology;

import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.hunting.RetoldPreyTargeting;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Resolves bounded real-prey transactions for unloaded catch-up. This never
 * synthesizes combat: only currently loaded, reachable wild prey can be
 * removed, and removal deliberately creates no loot or experience.
 */
public final class RetoldUnloadedPredation {
    private static final double PREY_RADIUS = 18.0D;
    private static final int MAX_PATH_PROBES_PER_TASK = 8;

    private RetoldUnloadedPredation() {
    }

    public static FindResult findReachablePrey(
            ServerLevel level,
            PathfinderMob hunter,
            long gameTime,
            int maximumPrey
    ) {
        if (!canHuntOffline(level, hunter) || maximumPrey <= 0) {
            return FindResult.complete(List.of());
        }

        RetoldAiScanCache.FreshScanResult<LivingEntity> scan =
                RetoldAiScanCache.freshNearby(
                        level,
                        hunter,
                        LivingEntity.class,
                        PREY_RADIUS,
                        gameTime
                );

        if (scan.deferred()) {
            return FindResult.deferredResult();
        }

        List<LivingEntity> candidates = new ArrayList<>();

        for (LivingEntity prey : scan.entities()) {
            if (isEligiblePrey(hunter, prey, gameTime)) {
                candidates.add(prey);
            }
        }

        candidates.sort(Comparator.comparingDouble(hunter::distanceToSqr));
        List<LivingEntity> reachable = new ArrayList<>();
        int probes = 0;

        for (LivingEntity prey : candidates) {
            if (reachable.size() >= maximumPrey
                    || probes >= MAX_PATH_PROBES_PER_TASK) {
                break;
            }

            probes++;
            RetoldBehaviorMovement.ReachabilityResult reachability =
                    RetoldBehaviorMovement.probeReachability(
                            hunter,
                            prey,
                            gameTime
                    );

            if (reachability == RetoldBehaviorMovement.ReachabilityResult.DEFERRED) {
                return FindResult.deferredResult();
            }

            if (reachability == RetoldBehaviorMovement.ReachabilityResult.REACHABLE) {
                reachable.add(prey);
            }
        }

        return FindResult.complete(reachable);
    }

    public static int consumePrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long simulatedTime
    ) {
        if (hunter == null
                || !(hunter.level() instanceof ServerLevel level)
                || !canHuntOffline(level, hunter)
                || !isEligiblePrey(hunter, prey, simulatedTime)) {
            return 0;
        }

        int relief = RetoldMobRules.preyRelief(hunter, prey);

        if (relief <= 0) {
            return 0;
        }

        prey.discard();
        return relief;
    }

    private static boolean canHuntOffline(
            ServerLevel level,
            PathfinderMob hunter
    ) {
        return level != null
                && hunter != null
                && hunter.level() == level
                && hunter.isAlive()
                && RetoldMobRules.canUseNaturalPreyHuntingSystems(hunter)
                && (!(hunter instanceof TamableAnimal tamable) || !tamable.isTame());
    }

    private static boolean isEligiblePrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        return hunter != null
                && prey != null
                && !prey.hasCustomName()
                && (!(prey instanceof TamableAnimal tamable) || !tamable.isTame())
                && (!(prey instanceof AbstractHorse horse) || !horse.isTamed())
                && !prey.isPassenger()
                && !prey.isVehicle()
                && RetoldPreyTargeting.isValidMobRulePrey(
                        hunter,
                        prey,
                        gameTime
                );
    }

    public record FindResult(
            List<LivingEntity> prey,
            boolean deferred
    ) {
        public FindResult {
            prey = List.copyOf(prey);
        }

        private static FindResult complete(List<LivingEntity> prey) {
            return new FindResult(prey, false);
        }

        private static FindResult deferredResult() {
            return new FindResult(List.of(), true);
        }
    }
}
