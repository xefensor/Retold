package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.control.RetoldAiPriorities;
import cz.xefensor.retold.behavior.core.RetoldBehaviorMovement;
import cz.xefensor.retold.behavior.core.RetoldBehaviorCoordinator;
import cz.xefensor.retold.behavior.food.RetoldFeedingAnimations;
import cz.xefensor.retold.behavior.food.RetoldFeedingPose;
import cz.xefensor.retold.behavior.food.RetoldStarvationBehavior;
import cz.xefensor.retold.behavior.home.RetoldAnimalDailyRhythm;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeMemory;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomeType;
import cz.xefensor.retold.behavior.home.RetoldAnimalHomes;
import cz.xefensor.retold.behavior.performance.RetoldAiScanCache;
import cz.xefensor.retold.behavior.performance.RetoldBlockTargetSearch;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.behavior.profiles.RetoldMobState;
import cz.xefensor.retold.behavior.profiles.RetoldMobStates;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class RetoldBatColonyEvents {
    private static final RetoldAiControlOwner CONTROL_OWNER =
            RetoldAiControlOwner.BAT_COLONY;

    private static final String REASON_HUNT = "bat_arthropod_hunt";
    private static final String REASON_SEARCH = "bat_food_search";
    private static final String REASON_ROOST_BIAS = "bat_roost_bias";
    private static final String REASON_PANIC = "bat_colony_panic";

    private static final int ROOST_SCAN_CACHE_TICKS = 20 * 10;
    private static final int COLONY_SCAN_CACHE_TICKS = 20;
    private static final int SPACING_SCAN_CACHE_TICKS = 4;
    private static final int PREY_SCAN_CACHE_TICKS = 12;
    private static final int FOOD_SCAN_CACHE_TICKS = 12;
    private static final int CONTROL_TICKS = 20;
    private static final int SEARCH_TICKS = 20 * 5;
    private static final int ROOST_ROUTE_TICKS = 20 * 5;
    private static final int PANIC_TICKS = 20 * 10;
    private static final int BITE_COOLDOWN_TICKS = 12;
    private static final int MIN_COMBAT_EVASION_TICKS = 8;
    private static final int COMBAT_EVASION_VARIANCE_TICKS = 12;
    private static final int MIN_DAYTIME_SETTLE_DELAY_TICKS = 8;
    private static final int DAYTIME_SETTLE_DELAY_VARIANCE_TICKS = 32;
    private static final int HUNTING_PARTY_DIRECTION_TICKS = 20 * 20;
    private static final int HUNTING_PARTY_THINK_TICKS = 8;
    private static final int HUNTING_PARTY_RECRUIT_TICKS = 20 * 2;
    private static final int AMBIENT_INSECT_INTERVAL_TICKS = 20 * 30;
    private static final int AMBIENT_INSECT_HUNGER_RELIEF = 8;

    private static final int ROOST_HORIZONTAL_RADIUS = 6;
    private static final int ROOST_VERTICAL_RADIUS = 32;
    private static final int HANGING_SLOT_HORIZONTAL_RADIUS = 8;
    private static final int ROOST_ZONE_HORIZONTAL_RADIUS = 16;
    private static final int ROOST_ZONE_VERTICAL_RADIUS = 8;
    private static final int MAX_ROOST_MEMBERS = 12;
    private static final int MAX_HUNTING_PARTY_SIZE = 5;
    private static final int SEARCH_PATH_ATTEMPTS = 2;
    private static final int SEARCH_DESTINATION_ATTEMPTS = 24;

    private static final double COLONY_RADIUS_BLOCKS = 18.0D;
    private static final double PREY_RADIUS_BLOCKS = 18.0D;
    private static final double FOOD_RADIUS_BLOCKS = 14.0D;
    private static final double HUNT_ABANDON_RADIUS_BLOCKS = 32.0D;
    private static final double HUNT_ABANDON_RADIUS_SQUARED =
            HUNT_ABANDON_RADIUS_BLOCKS * HUNT_ABANDON_RADIUS_BLOCKS;
    private static final double BITE_RADIUS_BLOCKS = 1.25D;
    private static final double BITE_RADIUS_SQUARED =
            BITE_RADIUS_BLOCKS * BITE_RADIUS_BLOCKS;
    private static final double EAT_ITEM_RADIUS_BLOCKS = 1.25D;
    private static final double EAT_ITEM_RADIUS_SQUARED =
            EAT_ITEM_RADIUS_BLOCKS * EAT_ITEM_RADIUS_BLOCKS;
    private static final double PANIC_DISTANCE_BLOCKS = 13.0D;
    private static final double COMBAT_EVASION_DISTANCE_BLOCKS = 4.0D;
    private static final double FLIGHT_SPACING_RADIUS_BLOCKS = 1.6D;
    private static final double SEARCH_ROOST_LEASH_BLOCKS = 32.0D;
    private static final double SEARCH_ROOST_LEASH_SQUARED =
            SEARCH_ROOST_LEASH_BLOCKS * SEARCH_ROOST_LEASH_BLOCKS;
    private static final double HUNTING_PARTY_RECRUIT_RADIUS_BLOCKS = 18.0D;
    private static final double HUNTING_PARTY_SEARCH_FORWARD_BLOCKS = 12.0D;
    private static final double HUNTING_PARTY_SEARCH_SIDE_BLOCKS = 1.75D;
    private static final double HANGING_SLOT_REACHED_SQUARED = 1.5D * 1.5D;
    private static final double ROOST_BIAS_ACCELERATION = 0.025D;
    private static final double ROOST_BIAS_COLLISION_LOOKAHEAD = 0.75D;
    private static final double ROOST_BIAS_CLEARANCE_DISTANCE = 8.0D;

    private static final float BITE_DAMAGE = 1.0F;
    private static final int HUNT_HUNGER_RELIEF = 28;

    private static final Map<Bat, FlightDirective> FLIGHT_DIRECTIVES =
            new WeakHashMap<>();
    private static final Map<Bat, Long> NEXT_BITE_AT =
            new WeakHashMap<>();
    private static final Map<Bat, CombatEvasion> COMBAT_EVASIONS =
            new WeakHashMap<>();
    private static final Map<Bat, PendingPanic> PENDING_PANICS =
            new WeakHashMap<>();
    private static final Map<Bat, RoostBias> ROOST_BIASES =
            new WeakHashMap<>();
    private static final Map<Bat, Long> DAYTIME_SETTLE_READY_AT =
            new WeakHashMap<>();
    private static final Map<Bat, FlightSpacing> FLIGHT_SPACING =
            new WeakHashMap<>();
    private static final Map<Bat, BatHuntingParty> HUNTING_PARTIES =
            new WeakHashMap<>();

    private RetoldBatColonyEvents() {
    }

    public static void tick(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        if (!isUsableBat(level, bat)) {
            releaseAllOwnedBehavior(bat);
            return;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                bat,
                gameTime
        );
        if (!tickHunger(level, bat, state, gameTime)) {
            return;
        }
        processPendingPanic(bat, gameTime);

        RetoldAnimalHomeMemory roost = getOrCreateRoost(
                level,
                bat,
                gameTime
        );

        if (isPanicking(bat)) {
            bat.setResting(false);
            DAYTIME_SETTLE_READY_AT.remove(bat);
            return;
        }

        releaseExpiredPanic(bat);

        if (!RetoldAnimalDailyRhythm.isNight(level)) {
            leaveHuntingParty(bat);
            clearOwnedHunt(bat);
            releaseOwnedFeeding(bat);
            releaseOwnedSearch(bat);
            yieldDuplicateDaytimeRoost(level, bat, gameTime);
            updateDaytimeRoostBias(level, bat, roost, gameTime);
            encourageDaytimeRest(level, bat, gameTime);
            return;
        }

        DAYTIME_SETTLE_READY_AT.remove(bat);
        releaseOwnedReturn(bat);

        if (handleDroppedFood(
                level,
                bat,
                state,
                gameTime
        )) {
            return;
        }

        releaseOwnedFeeding(bat);

        if (!RetoldMobRules.hasProfileHuntDrive(bat, state)) {
            leaveHuntingParty(bat);
            clearOwnedHunt(bat);
            releaseOwnedSearch(bat);
            return;
        }

        updateNightHunt(
                level,
                bat,
                state,
                roost,
                gameTime
        );
    }

    @SubscribeEvent
    public static void onBatDamaged(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Bat bat)
                || event.getHealthDamage() <= 0.0F
                || !(bat.level() instanceof ServerLevel level)) {
            return;
        }

        Entity threat = event.getSource().getEntity();
        long gameTime = level.getGameTime();

        if (isArthropod(threat) && isOwnedValidPrey(
                bat,
                (LivingEntity) threat
        )) {
            beginCombatEvasion(
                    bat,
                    threat.position(),
                    gameTime
            );
            return;
        }

        sharePanic(level, bat, threat, gameTime);
    }

    static int sharePanic(
            ServerLevel level,
            Bat alarmBat,
            Entity threat,
            long gameTime
    ) {
        if (!isUsableBat(level, alarmBat)) {
            return 0;
        }

        Vec3 threatPosition = threat == null
                ? alarmBat.position()
                : threat.position();
        int responders = 0;

        beginPanic(
                alarmBat,
                threatPosition,
                gameTime
        );
        responders++;

        for (Bat colonyMember : RetoldAiScanCache.nearby(
                level,
                alarmBat,
                Bat.class,
                COLONY_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (colonyMember == alarmBat || !isUsableBat(level, colonyMember)) {
                continue;
            }

            if (shouldSharePanic(alarmBat, colonyMember)) {
                queuePanic(
                        colonyMember,
                        threatPosition,
                        gameTime
                );
                responders++;
            }
        }

        return responders;
    }

    public static boolean applyOwnedFlightStep(
            ServerLevel level,
            Bat bat
    ) {
        if (level == null || bat == null) {
            return false;
        }

        if (DAYTIME_SETTLE_READY_AT.containsKey(bat)) {
            boolean mayFinishSettling = !RetoldAiControl.isControlled(bat)
                    || RetoldAiControl.isControlledAsBy(
                    bat,
                    RetoldAiControlMode.SHELTER,
                    CONTROL_OWNER
            );

            if (mayFinishSettling
                    && holdAtDaytimeRoost(level, bat, level.getGameTime())) {
                return true;
            }

            DAYTIME_SETTLE_READY_AT.remove(bat);
        }

        FlightDirective directive = FLIGHT_DIRECTIVES.get(bat);
        long gameTime = level.getGameTime();

        if (directive == null && isPanicking(bat)) {
            bat.setResting(false);
            return true;
        }

        if (directive == null
                || gameTime > directive.expiresAt()
                || !RetoldAiControl.isControlledAsBy(
                bat,
                directive.mode(),
                CONTROL_OWNER
        )) {
            FLIGHT_DIRECTIVES.remove(bat);
            boolean keepPanicAwake = isPanicking(bat);

            if (keepPanicAwake) {
                bat.setResting(false);
            }

            return keepPanicAwake;
        }

        if (directive.mode() == RetoldAiControlMode.SHELTER
                && holdAtDaytimeRoost(level, bat, gameTime)) {
            return true;
        }

        if (directive.mode() == RetoldAiControlMode.SHELTER
                && bat.position().distanceToSqr(directive.destination())
                <= HANGING_SLOT_REACHED_SQUARED
                && hasSafeFlightStep(
                level,
                bat,
                directive.destination()
        )) {
            flyFinalShelterApproach(
                    bat,
                    directive.destination(),
                    directive.speed()
            );
            return true;
        }

        bat.setResting(false);
        Vec3 waypoint = RetoldBehaviorMovement.nextFlyingWaypoint(bat);

        if (waypoint == null) {
            if (directive.mode() == RetoldAiControlMode.SHELTER
                    && hasSafeFlightStep(
                    level,
                    bat,
                    directive.destination()
            )) {
                flyFinalShelterApproach(
                        bat,
                        directive.destination(),
                        directive.speed()
                );
                return true;
            }

            return directive.mode() == RetoldAiControlMode.FLEE
                    && isPanicking(bat);
        }

        if (!hasSafeFlightStep(level, bat, waypoint)) {
            RetoldBehaviorMovement.clearFlyingPath(bat);
            return directive.mode() == RetoldAiControlMode.FLEE
                    && isPanicking(bat);
        }

        Vec3 spacedWaypoint = applyFlightSpacing(
                level,
                bat,
                waypoint,
                gameTime
        );

        flyToward(
                bat,
                hasSafeFlightStep(level, bat, spacedWaypoint)
                        ? spacedWaypoint
                        : waypoint,
                directive.speed()
        );
        return true;
    }

    public static boolean applyDaytimeRoostBiasStep(
            ServerLevel level,
            Bat bat
    ) {
        if (level == null || bat == null) {
            return false;
        }

        RoostBias bias = ROOST_BIASES.get(bat);
        long gameTime = level.getGameTime();

        if (bias == null
                || gameTime > bias.expiresAt()
                || RetoldAnimalDailyRhythm.isNight(level)
                || bat.isResting()
                || !RetoldAiControl.isControlledAsByWithReason(
                bat,
                RetoldAiControlMode.SHELTER,
                CONTROL_OWNER,
                REASON_ROOST_BIAS
        )) {
            releaseOwnedReturn(bat);
            return false;
        }

        Vec3 waypoint = RetoldBehaviorMovement.nextFlyingWaypoint(bat);

        if (waypoint == null) {
            releaseOwnedReturn(bat);
            return false;
        }

        Vec3 towardRoost = waypoint.subtract(bat.position());

        if (towardRoost.lengthSqr() <= 0.0001D) {
            releaseOwnedReturn(bat);
            return false;
        }

        Vec3 direction = towardRoost.normalize();
        Vec3 lookahead = direction.scale(Math.min(
                ROOST_BIAS_COLLISION_LOOKAHEAD,
                towardRoost.length()
        ));

        if (!level.noCollision(
                bat,
                bat.getBoundingBox().move(lookahead)
        )) {
            releaseOwnedReturn(bat);
            return false;
        }

        bat.setDeltaMovement(
                bat.getDeltaMovement().add(
                        direction.scale(ROOST_BIAS_ACCELERATION)
                )
        );
        return true;
    }

    public static boolean isValidBatPrey(
            Bat bat,
            LivingEntity prey
    ) {
        return bat != null
                && prey != null
                && prey != bat
                && prey.isAlive()
                && !prey.isRemoved()
                && prey.level() == bat.level()
                && prey.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)
                && bat.canAttack(prey);
    }

    public static void onVanillaRoostDisturbed(
            ServerLevel level,
            Bat bat
    ) {
        if (!isUsableBat(level, bat)) {
            return;
        }

        Entity threat = level.getNearestPlayer(bat, 6.0D);
        beginPanic(
                bat,
                threat == null ? bat.position() : threat.position(),
                level.getGameTime()
        );
    }

    private static boolean isUsableBat(
            ServerLevel level,
            Bat bat
    ) {
        return level != null
                && bat != null
                && bat.level() == level
                && bat.isAlive()
                && !bat.isRemoved()
                && RetoldMobRules.isBatColony(bat);
    }

    private static boolean tickHunger(
            ServerLevel level,
            Bat bat,
            RetoldMobState state,
            long gameTime
    ) {
        int interval = RetoldMobRules.hungerInterval(bat);

        if (interval <= 0
                || gameTime - state.lastHungerTickAt() < interval) {
            return true;
        }

        state.addHunger(1);
        state.markHungerTick(gameTime);
        return RetoldStarvationBehavior.applyCriticalHunger(
                level,
                bat,
                state,
                gameTime
        );
    }

    private static RetoldAnimalHomeMemory getOrCreateRoost(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        RetoldAnimalHomeMemory roost = RetoldAnimalHomes.get(bat);

        if (isValidRoostArea(level, bat, roost)) {
            return roost;
        }

        if (roost != null) {
            BlockPos staleAnchor = roost.pos();
            boolean canRepairAroundStaleAnchor =
                    RetoldAnimalHomes.invalidReason(
                            level,
                            bat,
                            roost
                    ).equals(RetoldAnimalHomes.VALID);

            RetoldAnimalHomes.remove(bat);

            if (canRepairAroundStaleAnchor) {
                BlockPos repairedRoostPos = RetoldBlockTargetSearch.findBatRoost(
                        level,
                        bat,
                        staleAnchor,
                        HANGING_SLOT_HORIZONTAL_RADIUS,
                        ROOST_VERTICAL_RADIUS,
                        gameTime,
                        ROOST_SCAN_CACHE_TICKS
                );

                if (repairedRoostPos != null) {
                    RetoldAnimalHomeMemory repairedRoost =
                            RetoldAnimalHomes.rememberSingleHome(
                                    level,
                                    bat,
                                    repairedRoostPos,
                                    gameTime
                            );
                    assignNearbyRoostMembers(
                            level,
                            bat,
                            repairedRoost,
                            gameTime
                    );
                    return repairedRoost;
                }
            }
        }

        roost = adoptNearbyRoost(
                level,
                bat,
                gameTime
        );

        if (roost != null) {
            return roost;
        }

        BlockPos roostPos = bat.isResting()
                && RetoldBlockTargetSearch.isBatRoostAt(
                level,
                bat.blockPosition()
        )
                ? bat.blockPosition().immutable()
                : null;

        if (roostPos == null && !RetoldAnimalDailyRhythm.isNight(level)) {
            roostPos = RetoldBlockTargetSearch.findBatRoost(
                    level,
                    bat,
                    ROOST_HORIZONTAL_RADIUS,
                    ROOST_VERTICAL_RADIUS,
                    gameTime,
                    ROOST_SCAN_CACHE_TICKS
            );
        }

        if (roostPos == null) {
            return null;
        }

        roost = RetoldAnimalHomes.rememberSingleHome(
                level,
                bat,
                roostPos,
                gameTime
        );
        assignNearbyRoostMembers(
                level,
                bat,
                roost,
                gameTime
        );
        return roost;
    }

    private static RetoldAnimalHomeMemory adoptNearbyRoost(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        for (Bat candidate : RetoldAiScanCache.nearby(
                level,
                bat,
                Bat.class,
                COLONY_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (candidate == bat) {
                continue;
            }

            RetoldAnimalHomeMemory candidateRoost = RetoldAnimalHomes.get(candidate);

            if (!isValidRoostArea(level, candidate, candidateRoost)
                    || countNearbyRoostMembers(
                    level,
                    candidate,
                    candidateRoost,
                    gameTime
            ) >= MAX_ROOST_MEMBERS) {
                continue;
            }

            return RetoldAnimalHomes.rememberSingleHome(
                    level,
                    bat,
                    candidateRoost.pos(),
                    gameTime
            );
        }

        return null;
    }

    private static void assignNearbyRoostMembers(
            ServerLevel level,
            Bat founder,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        if (roost == null) {
            return;
        }

        int members = 1;

        for (Bat candidate : RetoldAiScanCache.nearby(
                level,
                founder,
                Bat.class,
                COLONY_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (members >= MAX_ROOST_MEMBERS) {
                return;
            }

            if (candidate == founder) {
                continue;
            }

            RetoldAnimalHomeMemory existing = RetoldAnimalHomes.get(candidate);

            if (isValidRoostArea(level, candidate, existing)) {
                continue;
            }

            RetoldAnimalHomes.rememberSingleHome(
                    level,
                    candidate,
                    roost.pos(),
                    gameTime
            );
            members++;
        }
    }

    private static int countNearbyRoostMembers(
            ServerLevel level,
            Bat center,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        int count = 0;

        for (Bat candidate : RetoldAiScanCache.nearby(
                level,
                center,
                Bat.class,
                COLONY_RADIUS_BLOCKS * 2.0D,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (hasSameRoost(level, candidate, roost)) {
                count++;
            }
        }

        return count;
    }

    private static boolean hasSameRoost(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost
    ) {
        return isValidRoostArea(level, bat, roost)
                && RetoldAnimalHomes.hasSameValidHomeAs(
                level,
                bat,
                roost
        );
    }

    private static boolean isValidRoostArea(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost
    ) {
        if (roost == null
                || roost.type() != RetoldAnimalHomeType.BAT_ROOST
                || !RetoldAnimalHomes.isValidFor(level, bat, roost)) {
            return false;
        }

        return !RetoldAnimalHomes.isChunkLoaded(level, roost)
                || RetoldBlockTargetSearch.isBatRoostAt(level, roost.pos());
    }

    private static void updateDaytimeRoostBias(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        if (!isValidRoostArea(level, bat, roost)
                || bat.isResting()) {
            releaseOwnedReturn(bat);
            return;
        }

        if (RetoldBlockTargetSearch.isBatRoostAt(
                level,
                bat.blockPosition()
        )) {
            releaseOwnedReturn(bat);
            holdAtDaytimeRoost(level, bat, gameTime);
            return;
        }

        RoostBias existing = ROOST_BIASES.get(bat);

        if (existing != null
                && gameTime <= existing.expiresAt()
                && RetoldBlockTargetSearch.isBatRoostAt(
                level,
                BlockPos.containing(existing.destination())
        )
                && RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.SHELTER,
                CONTROL_OWNER,
                RetoldAiPriorities.REST,
                REASON_ROOST_BIAS,
                gameTime,
                ROOST_ROUTE_TICKS
        ) && setFlightDirective(
                bat,
                RetoldAiControlMode.SHELTER,
                existing.destination(),
                1.0D,
                gameTime,
                gameTime + ROOST_ROUTE_TICKS
        )) {
            return;
        }

        if (existing != null) {
            releaseOwnedReturn(bat);
        }

        BlockPos hangingSlot = findDaytimeHangingSlot(
                level,
                bat,
                roost,
                gameTime
        );

        if (hangingSlot == null) {
            releaseOwnedReturn(bat);
            return;
        }

        Vec3 destination = hangingPosition(hangingSlot);

        if (!RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.SHELTER,
                CONTROL_OWNER,
                RetoldAiPriorities.REST,
                REASON_ROOST_BIAS,
                gameTime,
                ROOST_ROUTE_TICKS
        )) {
            releaseOwnedReturn(bat);
            return;
        }

        if (!setFlightDirective(
                bat,
                RetoldAiControlMode.SHELTER,
                destination,
                1.0D,
                gameTime,
                gameTime + ROOST_ROUTE_TICKS
        )) {
            releaseOwnedReturn(bat);
            return;
        }

        ROOST_BIASES.put(
                bat,
                new RoostBias(
                        destination,
                        gameTime + ROOST_ROUTE_TICKS
                )
        );
    }

    private static Vec3 hangingPosition(BlockPos hangingSlot) {
        /*
         * A flying Bat is 0.9 blocks tall. Approach slightly below vanilla's
         * final one-tenth alignment so collision checks retain clearance;
         * Bat.tick snaps it to the exact hanging height once it is resting.
         */
        return Vec3.atBottomCenterOf(hangingSlot).add(0.0D, 0.05D, 0.0D);
    }

    private static BlockPos findDaytimeHangingSlot(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        BlockPos currentPos = bat.blockPosition();

        if (RetoldBlockTargetSearch.isBatRoostAt(level, currentPos)) {
            return findUnreservedHangingSlot(
                    level,
                    bat,
                    roost,
                    currentPos,
                    gameTime
            );
        }

        if (RetoldBlockTargetSearch.isBatRoostAt(level, roost.pos())) {
            return findUnreservedHangingSlot(
                    level,
                    bat,
                    roost,
                    roost.pos(),
                    gameTime
            );
        }

        BlockPos slot = RetoldBlockTargetSearch.findBatRoost(
                level,
                bat,
                roost.pos(),
                HANGING_SLOT_HORIZONTAL_RADIUS,
                ROOST_VERTICAL_RADIUS,
                gameTime,
                ROOST_SCAN_CACHE_TICKS
        );

        if (slot != null) {
            return findUnreservedHangingSlot(
                    level,
                    bat,
                    roost,
                    slot,
                    gameTime
            );
        }

        return RetoldBlockTargetSearch.isBatRoostAt(level, roost.pos())
                ? roost.pos()
                : null;
    }

    private static BlockPos findUnreservedHangingSlot(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            BlockPos preferred,
            long gameTime
    ) {
        Set<BlockPos> reservedSlots = collectReservedHangingSlots(
                level,
                bat,
                roost,
                gameTime
        );

        if (!reservedSlots.contains(preferred)) {
            return preferred;
        }

        BlockPos.MutableBlockPos candidate = new BlockPos.MutableBlockPos();

        for (int ring = 1; ring <= HANGING_SLOT_HORIZONTAL_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }

                    candidate.set(
                            preferred.getX() + dx,
                            preferred.getY(),
                            preferred.getZ() + dz
                    );

                    if (!isInsideRoostZone(candidate, roost.pos())
                            || !RetoldBlockTargetSearch.isBatRoostAt(
                            level,
                            candidate
                    )
                            || reservedSlots.contains(candidate)) {
                        continue;
                    }

                    return candidate.immutable();
                }
            }
        }

        return null;
    }

    private static Set<BlockPos> collectReservedHangingSlots(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        Set<BlockPos> reservedSlots = new HashSet<>();

        for (Map.Entry<Bat, RoostBias> entry : ROOST_BIASES.entrySet()) {
            Bat other = entry.getKey();
            RoostBias bias = entry.getValue();

            if (other == null
                    || other == bat
                    || other.level() != level
                    || !other.isAlive()
                    || gameTime > bias.expiresAt()) {
                continue;
            }

            BlockPos destination = BlockPos.containing(bias.destination());

            if (isInsideRoostZone(destination, roost.pos())) {
                reservedSlots.add(destination);
            }
        }

        for (Bat other : RetoldAiScanCache.nearby(
                level,
                bat,
                Bat.class,
                COLONY_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (other == bat
                    || !other.isAlive()
                    || (!other.isResting()
                    && !DAYTIME_SETTLE_READY_AT.containsKey(other))) {
                continue;
            }

            BlockPos occupiedSlot = other.blockPosition();

            if (isInsideRoostZone(occupiedSlot, roost.pos())
                    && RetoldBlockTargetSearch.isBatRoostAt(
                    level,
                    occupiedSlot
            )) {
                reservedSlots.add(occupiedSlot.immutable());
            }
        }

        return reservedSlots;
    }

    private static void yieldDuplicateDaytimeRoost(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        if (!bat.isResting()) {
            return;
        }

        BlockPos occupiedSlot = bat.blockPosition();

        for (Bat other : RetoldAiScanCache.nearby(
                level,
                bat,
                Bat.class,
                FLIGHT_SPACING_RADIUS_BLOCKS,
                gameTime,
                SPACING_SCAN_CACHE_TICKS
        )) {
            if (other == bat
                    || !other.isAlive()
                    || !other.isResting()
                    || other.getId() >= bat.getId()
                    || !other.blockPosition().equals(occupiedSlot)) {
                continue;
            }

            /*
             * Keep one deterministic occupant and wake later arrivals. This
             * also repairs colonies that were already stacked before occupied
             * roost cells became part of slot reservation.
            */
            bat.setResting(false);
            bat.setPos(
                    bat.getX(),
                    bat.getY() - 0.25D,
                    bat.getZ()
            );
            bat.setDeltaMovement(Vec3.ZERO);
            DAYTIME_SETTLE_READY_AT.remove(bat);
            return;
        }
    }

    private static void encourageDaytimeRest(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        if (bat.isResting()) {
            DAYTIME_SETTLE_READY_AT.remove(bat);
            return;
        }

        if (ROOST_BIASES.containsKey(bat)
                || RetoldBehaviorMovement.hasFlyingPath(bat)
                || !RetoldBlockTargetSearch.isBatRoostAt(
                level,
                bat.blockPosition()
        )) {
            DAYTIME_SETTLE_READY_AT.remove(bat);
            return;
        }

        holdAtDaytimeRoost(level, bat, gameTime);
    }

    private static boolean holdAtDaytimeRoost(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        if (RetoldAnimalDailyRhythm.isNight(level)
                || !RetoldBlockTargetSearch.isBatRoostAt(
                level,
                bat.blockPosition()
        )) {
            DAYTIME_SETTLE_READY_AT.remove(bat);
            return false;
        }

        /*
         * Keep vanilla Bat AI from selecting another flight target during the
         * short arrival delay. Without this hold, an awake Bat can leave a
         * valid ceiling cell in the tick between finishing its Retold route
         * and being marked as resting.
         */
        bat.setDeltaMovement(Vec3.ZERO);

        long readyAt = DAYTIME_SETTLE_READY_AT.computeIfAbsent(
                bat,
                ignored -> gameTime
                        + MIN_DAYTIME_SETTLE_DELAY_TICKS
                        + bat.getRandom().nextInt(
                        DAYTIME_SETTLE_DELAY_VARIANCE_TICKS + 1
                )
        );

        if (gameTime < readyAt) {
            return true;
        }

        releaseOwnedReturn(bat);
        bat.setResting(true);
        DAYTIME_SETTLE_READY_AT.remove(bat);
        return true;
    }

    static boolean hasClearRoostBiasPath(
            ServerLevel level,
            Bat bat,
            Vec3 destination
    ) {
        Vec3 start = bat.getEyePosition();
        Vec3 towardRoost = destination.subtract(start);

        if (towardRoost.lengthSqr() <= 1.0D) {
            return true;
        }

        Vec3 clearanceEnd = start.add(
                towardRoost.normalize().scale(
                        Math.min(
                                ROOST_BIAS_CLEARANCE_DISTANCE,
                                towardRoost.length()
                        )
                )
        );
        HitResult hit = level.clip(new ClipContext(
                start,
                clearanceEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                bat
        ));

        return hit.getType() == HitResult.Type.MISS;
    }

    static boolean isInsideRoostZone(
            BlockPos position,
            BlockPos zoneCenter
    ) {
        int dx = position.getX() - zoneCenter.getX();
        int dy = Math.abs(position.getY() - zoneCenter.getY());
        int dz = position.getZ() - zoneCenter.getZ();

        return dy <= ROOST_ZONE_VERTICAL_RADIUS
                && dx * dx + dz * dz
                <= ROOST_ZONE_HORIZONTAL_RADIUS * ROOST_ZONE_HORIZONTAL_RADIUS;
    }

    private static void updateNightHunt(
            ServerLevel level,
            Bat bat,
            RetoldMobState state,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        BatHuntingParty party = getOrCreateHuntingParty(
                level,
                bat,
                roost,
                gameTime
        );
        LivingEntity prey = bat.getTarget();

        if (gameTime < party.nextThinkAt) {
            if (isOwnedValidPrey(bat, prey)) {
                tryBite(level, bat, prey, state, gameTime);
            }
            return;
        }

        party.nextThinkAt = gameTime + HUNTING_PARTY_THINK_TICKS;
        List<Bat> members = huntingPartyMembers(
                level,
                bat,
                party,
                gameTime
        );

        if (!isOwnedValidPrey(bat, prey)) {
            clearOwnedHunt(bat);
            prey = findPartyPrey(
                    level,
                    bat,
                    members,
                    gameTime
            );
        }

        if (prey == null) {
            clearOwnedHunt(bat);

            if (tryCatchAmbientCaveInsects(bat, state, gameTime)) {
                return;
            }

            updateNightFoodSearch(
                    level,
                    bat,
                    roost,
                    party,
                    members,
                    gameTime
            );
            return;
        }

        coordinatePartyHunt(
                members,
                prey,
                gameTime
        );

        if (bat.getTarget() == prey) {
            tryBite(
                    level,
                    bat,
                    prey,
                    state,
                    gameTime
            );
        }
    }

    private static boolean tryCatchAmbientCaveInsects(
            Bat bat,
            RetoldMobState state,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(bat)
                || !RetoldMobRules.hasEatDrive(bat, state)
                || state.lastAteAt() > 0L
                && gameTime - state.lastAteAt() < AMBIENT_INSECT_INTERVAL_TICKS) {
            return false;
        }

        releaseOwnedSearch(bat);
        state.addHunger(-AMBIENT_INSECT_HUNGER_RELIEF);
        state.markFed(gameTime);
        RetoldFeedingAnimations.play(bat);
        RetoldFeedingPose.begin(
                bat,
                bat.position().add(bat.getLookAngle()),
                gameTime
        );
        return true;
    }

    private static void coordinatePartyHunt(
            List<Bat> members,
            LivingEntity prey,
            long gameTime
    ) {
        for (Bat member : members) {
            RetoldMobState memberState = RetoldMobStates.getOrCreate(
                    member,
                    gameTime
            );

            if (!RetoldMobRules.hasProfileHuntDrive(member, memberState)
                    || !isValidBatPrey(member, prey)
                    || member.distanceToSqr(prey) > HUNT_ABANDON_RADIUS_SQUARED) {
                continue;
            }

            releaseOwnedSearch(member);

            if (!RetoldAiControl.tryClaim(
                    member,
                    RetoldAiControlMode.HUNT,
                    CONTROL_OWNER,
                    RetoldAiPriorities.HUNT,
                    REASON_HUNT,
                    gameTime,
                    CONTROL_TICKS
            )) {
                continue;
            }

            if (member.getTarget() != prey
                    && !RetoldCombatTargets.applyAttackTarget(
                    member,
                    prey,
                    RetoldTargetSource.BEHAVIOR_COMBAT
            )) {
                clearOwnedHunt(member);
                continue;
            }

            member.setResting(false);
            Vec3 destination = activeCombatEvasionDestination(
                    member,
                    prey,
                    gameTime
            );

            if (destination == null) {
                destination = combatApproachPosition(member, prey);
            }

            setFlightDirective(
                    member,
                    RetoldAiControlMode.HUNT,
                    destination,
                    1.12D,
                    gameTime,
                    gameTime + CONTROL_TICKS
            );
        }
    }

    private static LivingEntity findPartyPrey(
            ServerLevel level,
            Bat sensor,
            List<Bat> members,
            long gameTime
    ) {
        for (Bat member : members) {
            LivingEntity retained = member.getTarget();

            if (isOwnedValidPrey(member, retained)) {
                return retained;
            }

            LivingEntity detected = findNearestPrey(
                    level,
                    member,
                    gameTime
            );

            if (detected != null) {
                return detected;
            }
        }

        return findNearestPrey(level, sensor, gameTime);
    }

    private static BatHuntingParty getOrCreateHuntingParty(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        BatHuntingParty party = HUNTING_PARTIES.get(bat);

        if (party == null) {
            party = new BatHuntingParty(bat.getId());
            HUNTING_PARTIES.put(bat, party);
        }

        int currentSize = huntingPartySize(party);

        if (currentSize >= MAX_HUNTING_PARTY_SIZE
                || gameTime < party.nextRecruitAt) {
            return party;
        }

        party.nextRecruitAt = gameTime + HUNTING_PARTY_RECRUIT_TICKS;

        List<Bat> candidates = new ArrayList<>(RetoldAiScanCache.nearby(
                level,
                bat,
                Bat.class,
                HUNTING_PARTY_RECRUIT_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        ));
        candidates.removeIf(candidate -> !isHuntingPartyCandidate(
                level,
                bat,
                candidate,
                roost,
                gameTime
        ));
        candidates.sort(
                Comparator.comparingDouble(
                        (Bat candidate) -> bat.distanceToSqr(candidate)
                )
                        .thenComparingInt(Bat::getId)
        );

        for (Bat candidate : candidates) {
            if (currentSize >= MAX_HUNTING_PARTY_SIZE) {
                break;
            }

            HUNTING_PARTIES.put(candidate, party);
            currentSize++;
        }

        return party;
    }

    private static boolean isHuntingPartyCandidate(
            ServerLevel level,
            Bat recruiter,
            Bat candidate,
            RetoldAnimalHomeMemory roost,
            long gameTime
    ) {
        if (candidate == recruiter
                || !isUsableBat(level, candidate)
                || HUNTING_PARTIES.containsKey(candidate)) {
            return false;
        }

        if (isValidRoostArea(level, recruiter, roost)
                && !hasSameRoost(level, candidate, roost)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        if (mode != RetoldAiControlMode.NONE
                && mode != RetoldAiControlMode.SEARCH
                && mode != RetoldAiControlMode.HUNT) {
            return false;
        }

        if (mode != RetoldAiControlMode.NONE
                && !RetoldAiControl.isControlledAsBy(
                candidate,
                mode,
                CONTROL_OWNER
        )) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                candidate,
                gameTime
        );

        return RetoldMobRules.hasProfileHuntDrive(candidate, state);
    }

    private static List<Bat> huntingPartyMembers(
            ServerLevel level,
            Bat reference,
            BatHuntingParty party,
            long gameTime
    ) {
        List<Bat> members = new ArrayList<>();

        if (party == null) {
            return members;
        }

        if (HUNTING_PARTIES.get(reference) == party
                && isUsableBat(level, reference)) {
            members.add(reference);
        }

        for (Bat candidate : RetoldAiScanCache.nearby(
                level,
                reference,
                Bat.class,
                HUNT_ABANDON_RADIUS_BLOCKS,
                gameTime,
                COLONY_SCAN_CACHE_TICKS
        )) {
            if (candidate != reference
                    && HUNTING_PARTIES.get(candidate) == party
                    && isUsableBat(level, candidate)) {
                members.add(candidate);
            }
        }

        members.sort(Comparator.comparingInt(Bat::getId));
        return members;
    }

    private static int huntingPartySize(BatHuntingParty party) {
        int size = 0;

        for (BatHuntingParty candidate : HUNTING_PARTIES.values()) {
            if (candidate == party) {
                size++;
            }
        }

        return size;
    }

    private static void leaveHuntingParty(Bat bat) {
        if (bat != null) {
            HUNTING_PARTIES.remove(bat);
        }
    }

    static int huntingPartySize(Bat bat) {
        BatHuntingParty party = HUNTING_PARTIES.get(bat);
        return party == null ? 0 : huntingPartySize(party);
    }

    static Vec3 huntingPartyDirection(Bat bat) {
        BatHuntingParty party = HUNTING_PARTIES.get(bat);
        return party == null ? null : party.searchDirection;
    }

    static long huntingPartyNextThinkAt(Bat bat) {
        BatHuntingParty party = HUNTING_PARTIES.get(bat);
        return party == null ? 0L : party.nextThinkAt;
    }

    static Vec3 daytimeRoostDestination(Bat bat) {
        RoostBias bias = ROOST_BIASES.get(bat);
        return bias == null ? null : bias.destination();
    }

    static Vec3 flightDestination(Bat bat) {
        FlightDirective directive = FLIGHT_DIRECTIVES.get(bat);
        return directive == null ? null : directive.destination();
    }

    private static void directPartySearch(
            ServerLevel level,
            Bat sensor,
            RetoldAnimalHomeMemory roost,
            BatHuntingParty party,
            List<Bat> members,
            long gameTime
    ) {
        Vec3 direction = ensurePartySearchDirection(
                sensor,
                members,
                party,
                gameTime
        );

        for (int index = 0; index < members.size(); index++) {
            startOrContinuePartySearch(
                    level,
                    members.get(index),
                    roost,
                    party,
                    direction,
                    members.size(),
                    index,
                    gameTime
            );
        }
    }

    private static Vec3 ensurePartySearchDirection(
            Bat fallbackLeader,
            List<Bat> members,
            BatHuntingParty party,
            long gameTime
    ) {
        if (party.searchDirection != null
                && party.searchDirection.lengthSqr() > 0.0001D
                && gameTime <= party.directionExpiresAt) {
            return party.searchDirection;
        }

        Bat leader = members.stream()
                .filter(member -> member.getId() == party.leaderId)
                .findFirst()
                .orElse(members.isEmpty() ? fallbackLeader : members.getFirst());
        Vec3 previous = party.searchDirection;
        Vec3 direction;

        if (previous == null || previous.horizontalDistanceSqr() <= 0.0001D) {
            Vec3 movement = leader.getDeltaMovement();
            direction = new Vec3(movement.x(), 0.0D, movement.z());

            if (direction.lengthSqr() <= 0.0001D) {
                Vec3 look = leader.getLookAngle();
                direction = new Vec3(look.x(), 0.0D, look.z());
            }

            if (direction.lengthSqr() <= 0.0001D) {
                double angle = leader.getRandom().nextDouble() * Math.PI * 2.0D;
                direction = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            }
        } else {
            double turn = Math.toRadians(
                    70.0D + leader.getRandom().nextDouble() * 90.0D
            );

            if (leader.getRandom().nextBoolean()) {
                turn = -turn;
            }

            double cos = Math.cos(turn);
            double sin = Math.sin(turn);
            direction = new Vec3(
                    previous.x() * cos - previous.z() * sin,
                    0.0D,
                    previous.x() * sin + previous.z() * cos
            );
        }

        party.searchDirection = direction.normalize();
        party.directionExpiresAt = gameTime + HUNTING_PARTY_DIRECTION_TICKS;
        party.pathFailures = 0;
        return party.searchDirection;
    }

    private static void startOrContinuePartySearch(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            BatHuntingParty party,
            Vec3 direction,
            int partySize,
            int index,
            long gameTime
    ) {
        FlightDirective existing = FLIGHT_DIRECTIVES.get(bat);

        if (existing != null
                && existing.mode() == RetoldAiControlMode.SEARCH
                && gameTime <= existing.expiresAt()
                && RetoldBehaviorMovement.hasFlyingPath(bat)
                && RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER,
                RetoldAiPriorities.SEARCH,
                REASON_SEARCH,
                gameTime,
                CONTROL_TICKS
        )) {
            return;
        }

        releaseOwnedSearch(bat);

        if (!RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.SEARCH,
                CONTROL_OWNER,
                RetoldAiPriorities.SEARCH,
                REASON_SEARCH,
                gameTime,
                CONTROL_TICKS
        )) {
            return;
        }

        for (int attempt = 0; attempt < SEARCH_PATH_ATTEMPTS; attempt++) {
            Vec3 destination = partySearchDestination(
                    level,
                    bat,
                    roost,
                    direction,
                    partySize,
                    index,
                    attempt
            );

            if (destination != null && setFlightDirective(
                    bat,
                    RetoldAiControlMode.SEARCH,
                    destination,
                    0.9D,
                    gameTime,
                    gameTime + SEARCH_TICKS
            )) {
                bat.setResting(false);
                return;
            }
        }

        party.pathFailures++;

        if (party.pathFailures >= Math.max(2, partySize)) {
            party.directionExpiresAt = gameTime - 1L;
            party.pathFailures = 0;
        }

        releaseOwnedSearch(bat);
    }

    private static boolean handleDroppedFood(
            ServerLevel level,
            Bat bat,
            RetoldMobState state,
            long gameTime
    ) {
        if (!RetoldMobRules.hasEatDrive(bat, state)) {
            return false;
        }

        ItemEntity food = findNearestDroppedFood(
                level,
                bat,
                gameTime
        );

        if (food == null) {
            return false;
        }

        leaveHuntingParty(bat);
        clearOwnedHunt(bat);
        releaseOwnedSearch(bat);

        if (bat.distanceToSqr(food) <= EAT_ITEM_RADIUS_SQUARED) {
            consumeDroppedFood(
                    bat,
                    state,
                    food,
                    gameTime
            );
            return true;
        }

        if (!RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.FEED,
                CONTROL_OWNER,
                RetoldAiPriorities.FEED,
                "bat_dropped_food",
                gameTime,
                CONTROL_TICKS
        )) {
            return false;
        }

        bat.setResting(false);
        setFlightDirective(
                bat,
                RetoldAiControlMode.FEED,
                food.position(),
                1.0D,
                gameTime,
                gameTime + CONTROL_TICKS
        );
        return true;
    }

    private static void updateNightFoodSearch(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            BatHuntingParty party,
            List<Bat> members,
            long gameTime
    ) {
        directPartySearch(level, bat, roost, party, members, gameTime);
    }

    private static Vec3 partySearchDestination(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            Vec3 direction,
            int partySize,
            int index,
            int pathAttempt
    ) {
        Vec3 origin = bat.position();
        Vec3 side = new Vec3(-direction.z(), 0.0D, direction.x());
        double centeredIndex = index - (partySize - 1) * 0.5D;
        double baseSideOffset = centeredIndex * HUNTING_PARTY_SEARCH_SIDE_BLOCKS;

        for (int attempt = 0; attempt < SEARCH_DESTINATION_ATTEMPTS; attempt++) {
            int combinedAttempt = attempt + pathAttempt * SEARCH_DESTINATION_ATTEMPTS;
            double forward = HUNTING_PARTY_SEARCH_FORWARD_BLOCKS
                    - combinedAttempt % 4 * 1.5D;
            int sideBand = combinedAttempt / 4 % 5 - 2;
            double sideOffset = baseSideOffset + sideBand * 1.25D;
            int y = Mth.clamp(
                    Mth.floor(origin.y())
                            + Math.floorMod(bat.getId() + combinedAttempt, 5)
                            - 2,
                    level.getMinY() + 1,
                    level.getMaxY() - 2
            );
            Vec3 preferred = origin
                    .add(direction.scale(forward))
                    .add(side.scale(sideOffset));
            BlockPos candidate = BlockPos.containing(
                    preferred.x(),
                    y,
                    preferred.z()
            );

            if (!isUsableSearchDestination(level, bat, roost, candidate)) {
                continue;
            }

            return Vec3.atCenterOf(candidate);
        }

        return null;
    }

    private static boolean isUsableSearchDestination(
            ServerLevel level,
            Bat bat,
            RetoldAnimalHomeMemory roost,
            BlockPos candidate
    ) {
        if (!level.hasChunkAt(candidate)
                || !level.getBlockState(candidate).isAir()
                || !level.getFluidState(candidate).isEmpty()) {
            return false;
        }

        if (isValidRoostArea(level, bat, roost)) {
            int dy = Math.abs(candidate.getY() - roost.pos().getY());
            int dx = candidate.getX() - roost.pos().getX();
            int dz = candidate.getZ() - roost.pos().getZ();

            if (dy > ROOST_ZONE_VERTICAL_RADIUS * 2
                    || dx * dx + dz * dz > SEARCH_ROOST_LEASH_SQUARED) {
                return false;
            }
        }

        Vec3 destination = Vec3.atCenterOf(candidate);

        return level.noCollision(
                bat,
                bat.getBoundingBox().move(
                        destination.subtract(bat.position())
                )
        );
    }

    private static ItemEntity findNearestDroppedFood(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        ItemEntity best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (ItemEntity candidate : RetoldAiScanCache.nearby(
                level,
                bat,
                ItemEntity.class,
                FOOD_RADIUS_BLOCKS,
                gameTime,
                FOOD_SCAN_CACHE_TICKS
        )) {
            if (!isValidDroppedFood(bat, candidate)) {
                continue;
            }

            double distanceSquared = bat.distanceToSqr(candidate);

            if (distanceSquared < bestDistanceSquared) {
                best = candidate;
                bestDistanceSquared = distanceSquared;
            }
        }

        return best;
    }

    private static boolean isValidDroppedFood(
            Bat bat,
            ItemEntity food
    ) {
        if (food == null || !food.isAlive() || food.isRemoved()) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                bat,
                food.getItem()
        );
    }

    private static void consumeDroppedFood(
            Bat bat,
            RetoldMobState state,
            ItemEntity food,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(bat)) {
            return;
        }

        Vec3 foodSource = food.position();
        ItemStack stack = food.getItem();

        state.addHunger(-RetoldMobRules.foodRelief(bat, stack));
        state.markFed(gameTime);
        bat.ate();
        stack.shrink(1);

        if (stack.isEmpty()) {
            food.discard();
        } else {
            food.setItem(stack);
        }

        releaseOwnedFeeding(bat);
        RetoldFeedingPose.begin(bat, foodSource, gameTime);
    }

    private static LivingEntity findNearestPrey(
            ServerLevel level,
            Bat bat,
            long gameTime
    ) {
        LivingEntity best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (LivingEntity candidate : RetoldAiScanCache.nearby(
                level,
                bat,
                LivingEntity.class,
                PREY_RADIUS_BLOCKS,
                gameTime,
                PREY_SCAN_CACHE_TICKS
        )) {
            if (!isValidBatPrey(bat, candidate)) {
                continue;
            }

            double distanceSquared = bat.distanceToSqr(candidate);

            if (distanceSquared < bestDistanceSquared) {
                best = candidate;
                bestDistanceSquared = distanceSquared;
            }
        }

        return best;
    }

    private static boolean isOwnedValidPrey(
            Bat bat,
            LivingEntity prey
    ) {
        return isValidBatPrey(bat, prey)
                && RetoldFactionTargetMemory.isOwnedByAny(
                bat,
                prey,
                RetoldTargetSource.BEHAVIOR_COMBAT
        );
    }

    private static void tryBite(
            ServerLevel level,
            Bat bat,
            LivingEntity prey,
            RetoldMobState state,
            long gameTime
    ) {
        if (!RetoldBehaviorCoordinator.canCompleteMeal(bat)
                || bat.distanceToSqr(prey) > BITE_RADIUS_SQUARED
                || gameTime < NEXT_BITE_AT.getOrDefault(bat, 0L)) {
            return;
        }

        NEXT_BITE_AT.put(
                bat,
                gameTime + BITE_COOLDOWN_TICKS
        );

        if (!prey.hurtServer(
                level,
                level.damageSources().mobAttack(bat),
                BITE_DAMAGE
        ) || prey.isAlive()) {
            return;
        }

        Vec3 foodSource = prey.position();
        state.addHunger(-HUNT_HUNGER_RELIEF);
        state.markFed(gameTime);
        state.markSuccessfulHunt(gameTime);
        bat.heal(1.0F);
        clearOwnedHunt(bat);
        RetoldFeedingPose.begin(bat, foodSource, gameTime);
    }

    private static void beginPanic(
            Bat bat,
            Vec3 threatPosition,
            long gameTime
    ) {
        leaveHuntingParty(bat);
        clearOwnedHunt(bat);
        releaseOwnedFeeding(bat);
        releaseOwnedSearch(bat);
        DAYTIME_SETTLE_READY_AT.remove(bat);
        releaseOwnedReturn(bat);

        if (!RetoldAiControl.tryClaim(
                bat,
                RetoldAiControlMode.FLEE,
                CONTROL_OWNER,
                RetoldAiPriorities.above(RetoldAiPriorities.FLEE, 2),
                REASON_PANIC,
                gameTime,
                PANIC_TICKS
        )) {
            return;
        }

        Vec3 away = bat.position().subtract(threatPosition);

        if (away.horizontalDistanceSqr() <= 0.0001D) {
            double angle = bat.getRandom().nextDouble() * Math.PI * 2.0D;
            away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        } else {
            away = new Vec3(away.x(), 0.0D, away.z()).normalize();
        }

        Vec3 destination = bat.position()
                .add(away.scale(PANIC_DISTANCE_BLOCKS))
                .add(0.0D, 4.0D, 0.0D);

        bat.setResting(false);
        RetoldMobStates.getOrCreate(bat, gameTime).markDanger(gameTime);
        setFlightDirective(
                bat,
                RetoldAiControlMode.FLEE,
                destination,
                1.3D,
                gameTime,
                gameTime + PANIC_TICKS
        );
    }

    private static boolean isArthropod(Entity entity) {
        return entity instanceof LivingEntity living
                && living.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD);
    }

    private static void beginCombatEvasion(
            Bat bat,
            Vec3 threatPosition,
            long gameTime
    ) {
        int duration = MIN_COMBAT_EVASION_TICKS
                + bat.getRandom().nextInt(COMBAT_EVASION_VARIANCE_TICKS + 1);

        PENDING_PANICS.remove(bat);
        COMBAT_EVASIONS.put(
                bat,
                new CombatEvasion(
                        threatPosition,
                        gameTime + duration
                )
        );
    }

    private static Vec3 activeCombatEvasionDestination(
            Bat bat,
            LivingEntity prey,
            long gameTime
    ) {
        CombatEvasion evasion = COMBAT_EVASIONS.get(bat);

        if (evasion == null) {
            return null;
        }

        if (gameTime > evasion.expiresAt()) {
            COMBAT_EVASIONS.remove(bat);
            return null;
        }

        Vec3 away = horizontalAwayVector(
                bat,
                evasion.threatPosition()
        );
        double side = Math.floorMod(bat.getId(), 2) == 0 ? 1.0D : -1.0D;
        Vec3 lateral = new Vec3(-away.z(), 0.0D, away.x()).scale(side * 1.5D);

        return bat.position()
                .add(away.scale(COMBAT_EVASION_DISTANCE_BLOCKS))
                .add(lateral)
                .add(0.0D, Math.min(2.0D, prey.getBbHeight()), 0.0D);
    }

    private static Vec3 combatApproachPosition(
            Bat bat,
            LivingEntity prey
    ) {
        double angle = Math.floorMod(bat.getId() * 137, 360)
                * Math.PI / 180.0D;
        double radius = 0.7D + Math.floorMod(bat.getId(), 3) * 0.12D;
        double height = Math.min(1.0D, prey.getBbHeight() * 0.45D)
                + (Math.floorMod(bat.getId(), 3) - 1) * 0.18D;

        return prey.position().add(
                Math.cos(angle) * radius,
                height,
                Math.sin(angle) * radius
        );
    }

    private static Vec3 applyFlightSpacing(
            ServerLevel level,
            Bat bat,
            Vec3 destination,
            long gameTime
    ) {
        FlightSpacing cached = FLIGHT_SPACING.get(bat);

        if (cached != null && gameTime < cached.expiresAt()) {
            return destination.add(cached.offset());
        }

        Vec3 separation = Vec3.ZERO;

        for (Bat neighbor : RetoldAiScanCache.nearby(
                level,
                bat,
                Bat.class,
                FLIGHT_SPACING_RADIUS_BLOCKS,
                gameTime,
                SPACING_SCAN_CACHE_TICKS
        )) {
            if (neighbor == bat || !neighbor.isAlive()) {
                continue;
            }

            Vec3 away = bat.position().subtract(neighbor.position());
            double distance = away.length();

            if (distance >= FLIGHT_SPACING_RADIUS_BLOCKS) {
                continue;
            }

            if (distance < 0.01D) {
                double angle = Math.floorMod(
                        bat.getId() * 137 + neighbor.getId() * 53,
                        360
                ) * Math.PI / 180.0D;
                away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
                distance = 0.0D;
            } else {
                away = away.normalize();
            }

            double strength = (FLIGHT_SPACING_RADIUS_BLOCKS - distance)
                    / FLIGHT_SPACING_RADIUS_BLOCKS;
            separation = separation.add(away.scale(strength * 1.6D));
        }

        FLIGHT_SPACING.put(
                bat,
                new FlightSpacing(
                        separation,
                        gameTime + SPACING_SCAN_CACHE_TICKS
                )
        );
        return destination.add(separation);
    }

    private static Vec3 horizontalAwayVector(
            Bat bat,
            Vec3 threatPosition
    ) {
        Vec3 away = bat.position().subtract(threatPosition);

        if (away.horizontalDistanceSqr() <= 0.0001D) {
            double angle = bat.getRandom().nextDouble() * Math.PI * 2.0D;
            return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }

        return new Vec3(away.x(), 0.0D, away.z()).normalize();
    }

    private static boolean shouldSharePanic(
            Bat alarmBat,
            Bat colonyMember
    ) {
        double distanceSquared = alarmBat.distanceToSqr(colonyMember);
        int chance = distanceSquared <= 36.0D ? 55
                : distanceSquared <= 144.0D ? 35 : 20;
        int roll = Math.floorMod(
                colonyMember.getId() * 31 + alarmBat.getId() * 17,
                100
        );

        return roll < chance;
    }

    private static void queuePanic(
            Bat bat,
            Vec3 threatPosition,
            long gameTime
    ) {
        int delay = 4 + Math.floorMod(bat.getId() * 11, 17);

        PENDING_PANICS.put(
                bat,
                new PendingPanic(
                        threatPosition,
                        gameTime + delay
                )
        );
    }

    private static void processPendingPanic(
            Bat bat,
            long gameTime
    ) {
        PendingPanic pending = PENDING_PANICS.get(bat);

        if (pending == null || gameTime < pending.startsAt()) {
            return;
        }

        PENDING_PANICS.remove(bat);
        beginPanic(bat, pending.threatPosition(), gameTime);
    }

    private static boolean isPanicking(Bat bat) {
        return RetoldAiControl.isControlledAsByWithReason(
                bat,
                RetoldAiControlMode.FLEE,
                CONTROL_OWNER,
                REASON_PANIC
        );
    }

    private static void releaseExpiredPanic(Bat bat) {
        if (!RetoldAiControl.isControlledAsBy(
                bat,
                RetoldAiControlMode.FLEE,
                CONTROL_OWNER
        )) {
            FlightDirective directive = FLIGHT_DIRECTIVES.get(bat);

            if (directive != null && directive.mode() == RetoldAiControlMode.FLEE) {
                FLIGHT_DIRECTIVES.remove(bat);
                RetoldBehaviorMovement.clearFlyingPath(bat);
            }
        }
    }

    private static void clearOwnedHunt(Bat bat) {
        LivingEntity target = bat == null ? null : bat.getTarget();

        if (target != null && RetoldFactionTargetMemory.isOwnedByAny(
                bat,
                target,
                RetoldTargetSource.BEHAVIOR_COMBAT
        )) {
            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    bat,
                    target,
                    false
            );
        }

        RetoldAiControl.clearIfControlledAsByAny(
                bat,
                CONTROL_OWNER,
                RetoldAiControlMode.HUNT
        );
        removeFlightDirective(bat, RetoldAiControlMode.HUNT);
    }

    private static void releaseOwnedReturn(Bat bat) {
        boolean ownedReturn = RetoldAiControl.isControlledAsByWithReason(
                bat,
                RetoldAiControlMode.SHELTER,
                CONTROL_OWNER,
                REASON_ROOST_BIAS
        ) || ROOST_BIASES.containsKey(bat);

        RetoldAiControl.clearIfControlledAsByAny(
                bat,
                CONTROL_OWNER,
                RetoldAiControlMode.SHELTER
        );
        removeFlightDirective(bat, RetoldAiControlMode.SHELTER);
        ROOST_BIASES.remove(bat);

        if (ownedReturn) {
            RetoldBehaviorMovement.clearFlyingPath(bat);
        }
    }

    private static void releaseOwnedSearch(Bat bat) {
        RetoldAiControl.clearIfControlledAsByAny(
                bat,
                CONTROL_OWNER,
                RetoldAiControlMode.SEARCH
        );
        removeFlightDirective(bat, RetoldAiControlMode.SEARCH);
    }

    private static void releaseOwnedFeeding(Bat bat) {
        RetoldAiControl.clearIfControlledAsByAny(
                bat,
                CONTROL_OWNER,
                RetoldAiControlMode.FEED
        );
        removeFlightDirective(bat, RetoldAiControlMode.FEED);
    }

    private static void releaseAllOwnedBehavior(Bat bat) {
        if (bat == null) {
            return;
        }

        clearOwnedHunt(bat);
        releaseOwnedReturn(bat);
        releaseOwnedFeeding(bat);
        releaseOwnedSearch(bat);
        RetoldAiControl.clearIfOwnedBy(bat, CONTROL_OWNER);
        FLIGHT_DIRECTIVES.remove(bat);
        RetoldBehaviorMovement.clearFlyingPath(bat);
        NEXT_BITE_AT.remove(bat);
        COMBAT_EVASIONS.remove(bat);
        PENDING_PANICS.remove(bat);
        ROOST_BIASES.remove(bat);
        DAYTIME_SETTLE_READY_AT.remove(bat);
        FLIGHT_SPACING.remove(bat);
        leaveHuntingParty(bat);
    }

    private static boolean setFlightDirective(
            Bat bat,
            RetoldAiControlMode mode,
            Vec3 destination,
            double speed,
            long gameTime,
            long expiresAt
    ) {
        FlightDirective existing = FLIGHT_DIRECTIVES.get(bat);
        boolean canFinishShelterApproach = mode == RetoldAiControlMode.SHELTER
                && bat.position().distanceToSqr(destination)
                <= HANGING_SLOT_REACHED_SQUARED;

        if (existing != null
                && existing.mode() == mode
                && gameTime <= existing.expiresAt()
                && existing.destination().distanceToSqr(destination)
                <= 1.5D * 1.5D
                && (RetoldBehaviorMovement.hasFlyingPath(bat)
                || canFinishShelterApproach)) {
            FLIGHT_DIRECTIVES.put(
                    bat,
                    new FlightDirective(
                            mode,
                            destination,
                            speed,
                            expiresAt
                    )
            );
            return true;
        }

        if (!RetoldBehaviorMovement.requestFlyingPath(
                bat,
                destination,
                gameTime,
                6,
                1.5D * 1.5D
        )) {
            return false;
        }

        FLIGHT_DIRECTIVES.put(
                bat,
                new FlightDirective(
                        mode,
                        destination,
                        speed,
                        expiresAt
                )
        );
        return true;
    }

    private static void removeFlightDirective(
            Bat bat,
            RetoldAiControlMode mode
    ) {
        FlightDirective directive = FLIGHT_DIRECTIVES.get(bat);

        if (directive != null && directive.mode() == mode) {
            FLIGHT_DIRECTIVES.remove(bat);
            RetoldBehaviorMovement.clearFlyingPath(bat);
        }
    }

    private static boolean hasSafeFlightStep(
            ServerLevel level,
            Bat bat,
            Vec3 destination
    ) {
        Vec3 direction = destination.subtract(bat.position());

        if (direction.lengthSqr() <= 0.0001D) {
            return true;
        }

        return level.noCollision(
                bat,
                bat.getBoundingBox().move(
                        direction.normalize().scale(
                                Math.min(
                                        ROOST_BIAS_COLLISION_LOOKAHEAD,
                                        direction.length()
                                )
                        )
                )
        );
    }

    private static void flyToward(
            Bat bat,
            Vec3 destination,
            double speed
    ) {
        double dx = destination.x() - bat.getX();
        double dy = destination.y() - bat.getY();
        double dz = destination.z() - bat.getZ();
        Vec3 direction = new Vec3(dx, dy, dz);

        if (direction.lengthSqr() > 1.0D) {
            direction = direction.normalize();
        }

        Vec3 movement = bat.getDeltaMovement();
        double horizontalGoal = 0.5D * speed;
        double verticalGoal = 0.7D * speed;
        Vec3 nextMovement = movement.add(
                (direction.x() * horizontalGoal - movement.x()) * 0.12D,
                (direction.y() * verticalGoal - movement.y()) * 0.12D,
                (direction.z() * horizontalGoal - movement.z()) * 0.12D
        );

        bat.setDeltaMovement(nextMovement);
        float desiredYaw = (float) (Mth.atan2(
                nextMovement.z(),
                nextMovement.x()
        ) * 180.0F / Math.PI) - 90.0F;
        bat.setYRot(
                bat.getYRot() + Mth.wrapDegrees(desiredYaw - bat.getYRot())
        );
    }

    private static void flyFinalShelterApproach(
            Bat bat,
            Vec3 destination,
            double speed
    ) {
        Vec3 towardDestination = destination.subtract(bat.position());

        if (towardDestination.lengthSqr() <= 0.0001D) {
            return;
        }

        /*
         * Preserve enough upward control to overcome Bat flight damping near
         * the ceiling. Proportional slowing alone reaches an equilibrium just
         * below the target cell and never lets blockPosition enter the roost.
         */
        flyToward(
                bat,
                bat.position().add(towardDestination.normalize()),
                speed
        );
    }

    private record FlightDirective(
            RetoldAiControlMode mode,
            Vec3 destination,
            double speed,
            long expiresAt
    ) {
    }

    private record CombatEvasion(
            Vec3 threatPosition,
            long expiresAt
    ) {
    }

    private record PendingPanic(
            Vec3 threatPosition,
            long startsAt
    ) {
    }

    private record RoostBias(
            Vec3 destination,
            long expiresAt
    ) {
    }

    private record FlightSpacing(
            Vec3 offset,
            long expiresAt
    ) {
    }

    private static final class BatHuntingParty {
        private final int leaderId;

        private Vec3 searchDirection;
        private long directionExpiresAt;
        private long nextThinkAt;
        private long nextRecruitAt;
        private int pathFailures;

        private BatHuntingParty(int leaderId) {
            this.leaderId = leaderId;
        }
    }
}
