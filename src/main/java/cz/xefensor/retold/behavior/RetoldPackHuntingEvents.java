package cz.xefensor.retold.behavior;

import cz.xefensor.retold.combat.RetoldFactionTargetGuards;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RetoldPackHuntingEvents {
    private static final Map<PathfinderMob, HuntingParty> PARTIES_BY_LEADER = new WeakHashMap<>();
    private static final Map<PathfinderMob, PathfinderMob> LEADER_BY_MEMBER = new WeakHashMap<>();

    private static final int PACK_THINK_INTERVAL_TICKS = 10;

    private static final int PARTY_SEARCH_CONTROL_TICKS = 20 * 6;
    private static final int PARTY_HUNT_CONTROL_TICKS = 20 * 5;
    private static final int PARTY_RETURN_CONTROL_TICKS = 20 * 6;
    private static final int PARTY_FEED_CONTROL_TICKS = 20 * 8;

    private static final int PARTY_SEARCH_DIRECTION_LIFE_TICKS = 20 * 45;

    private static final double WOLF_PACK_RADIUS_BLOCKS = 30.0D;
    private static final double WOLF_PACK_RADIUS_SQUARED =
            WOLF_PACK_RADIUS_BLOCKS * WOLF_PACK_RADIUS_BLOCKS;

    private static final double DOLPHIN_PACK_RADIUS_BLOCKS = 36.0D;
    private static final double DOLPHIN_PACK_RADIUS_SQUARED =
            DOLPHIN_PACK_RADIUS_BLOCKS * DOLPHIN_PACK_RADIUS_BLOCKS;

    private static final int MAX_WOLF_HUNTING_PARTY_SIZE = 4;
    private static final int MAX_DOLPHIN_HUNTING_PARTY_SIZE = 5;

    private static final int MIN_WOLF_HUNTING_PARTY_SIZE = 2;
    private static final int MIN_DOLPHIN_HUNTING_PARTY_SIZE = 2;

    private static final double WOLF_SEARCH_SPEED = 0.98D;
    private static final double DOLPHIN_SEARCH_SPEED = 1.08D;

    private static final double WOLF_HUNT_SPEED = 1.30D;
    private static final double DOLPHIN_HUNT_SPEED = 1.36D;

    private static final double RETURN_TO_PACK_SPEED = 0.86D;

    private static final double SEARCH_FORWARD_BLOCKS = 14.0D;
    private static final double SEARCH_SIDE_BLOCKS = 5.5D;

    private static final double PACK_RETURN_DISTANCE_BLOCKS = 7.0D;
    private static final double PACK_RETURN_DISTANCE_SQUARED =
            PACK_RETURN_DISTANCE_BLOCKS * PACK_RETURN_DISTANCE_BLOCKS;

    private static final double PACK_MEMBER_TOO_FAR_BLOCKS = 52.0D;
    private static final double PACK_MEMBER_TOO_FAR_SQUARED =
            PACK_MEMBER_TOO_FAR_BLOCKS * PACK_MEMBER_TOO_FAR_BLOCKS;

    private static final double PARTY_PREY_SENSE_RADIUS_BLOCKS = 24.0D;
    private static final double PARTY_PREY_SENSE_RADIUS_SQUARED =
            PARTY_PREY_SENSE_RADIUS_BLOCKS * PARTY_PREY_SENSE_RADIUS_BLOCKS;

    private static final double PARTY_SIGHT_RADIUS_BLOCKS = 24.0D;
    private static final double PARTY_SIGHT_RADIUS_SQUARED =
            PARTY_SIGHT_RADIUS_BLOCKS * PARTY_SIGHT_RADIUS_BLOCKS;

    private static final double PARTY_HEARING_RADIUS_BLOCKS = 9.0D;
    private static final double PARTY_HEARING_RADIUS_SQUARED =
            PARTY_HEARING_RADIUS_BLOCKS * PARTY_HEARING_RADIUS_BLOCKS;

    private static final double PARTY_SMELL_RADIUS_BLOCKS = 6.0D;
    private static final double PARTY_SMELL_RADIUS_SQUARED =
            PARTY_SMELL_RADIUS_BLOCKS * PARTY_SMELL_RADIUS_BLOCKS;

    private static final double AUDIBLE_MOVEMENT_THRESHOLD_SQUARED = 0.0016D;

    /*
     * Food sharing:
     * if any party member sees edible meat/food, hungry party members are sent to eat.
     * The party is NOT dissolved here. It stays linked until everyone is fed and returns.
     */
    private static final double PARTY_FOOD_RADIUS_BLOCKS = 16.0D;
    private static final double PARTY_FOOD_RADIUS_SQUARED =
            PARTY_FOOD_RADIUS_BLOCKS * PARTY_FOOD_RADIUS_BLOCKS;

    private static final double PARTY_FEED_SPEED = 0.95D;

    private RetoldPackHuntingEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PathfinderMob mob)) {
            return;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }

        if (!isPackHunter(mob)) {
            return;
        }

        long gameTime = level.getGameTime();

        if (!shouldThink(mob, gameTime)) {
            return;
        }

        PathfinderMob currentLeader = LEADER_BY_MEMBER.get(mob);

        if (currentLeader != null && currentLeader != mob) {
            HuntingParty memberParty = PARTIES_BY_LEADER.get(currentLeader);

            if (memberParty == null || !isValidPackAnimal(currentLeader)) {
                releaseMember(mob);
                return;
            }

            updateMember(
                    level,
                    currentLeader,
                    mob,
                    memberParty,
                    gameTime
            );

            return;
        }

        HuntingParty party = PARTIES_BY_LEADER.get(mob);

        if (party != null) {
            updateLeaderParty(
                    level,
                    mob,
                    party,
                    gameTime
            );

            return;
        }

        if (shouldStartHuntingParty(mob)) {
            createHuntingParty(
                    level,
                    mob,
                    gameTime
            );
        }
    }

    private static boolean shouldThink(
            PathfinderMob mob,
            long gameTime
    ) {
        int offset = Math.floorMod(
                mob.getId(),
                PACK_THINK_INTERVAL_TICKS
        );

        return (gameTime + offset) % PACK_THINK_INTERVAL_TICKS == 0L;
    }

    private static boolean shouldStartHuntingParty(PathfinderMob leader) {
        if (!isValidPackAnimal(leader)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(leader);

        return mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT;
    }

    private static void createHuntingParty(
            ServerLevel level,
            PathfinderMob leader,
            long gameTime
    ) {
        String path = getPath(leader);

        double radius = getPackRadius(path);
        double radiusSquared = radius * radius;
        int maxPartySize = getMaxPartySize(path);

        AABB area = leader.getBoundingBox().inflate(radius);

        List<PathfinderMob> nearbyPack = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isNormalPackCandidate(
                        leader,
                        candidate,
                        radiusSquared
                )
        );

        nearbyPack.sort(
                Comparator.comparingDouble(candidate -> leader.distanceToSqr(candidate))
        );

        BlockPos packCenter = calculatePackCenter(
                leader,
                nearbyPack
        );

        List<PathfinderMob> selectedMembers = new ArrayList<>();

        for (PathfinderMob candidate : nearbyPack) {
            if (candidate == leader) {
                continue;
            }

            if (selectedMembers.size() >= maxPartySize - 1) {
                break;
            }

            if (!canBeSelectedForHuntingParty(candidate)) {
                continue;
            }

            selectedMembers.add(candidate);
        }

        int totalPartySize = selectedMembers.size() + 1;

        if (totalPartySize < getMinPartySize(path)) {
            return;
        }

        HuntingParty party = new HuntingParty(
                packCenter,
                gameTime
        );

        party.members.addAll(selectedMembers);

        PARTIES_BY_LEADER.put(
                leader,
                party
        );

        LEADER_BY_MEMBER.put(
                leader,
                leader
        );

        for (PathfinderMob member : selectedMembers) {
            LEADER_BY_MEMBER.put(
                    member,
                    leader
            );
        }

        updateLeaderParty(
                level,
                leader,
                party,
                gameTime
        );
    }

    private static void updateLeaderParty(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        if (!isValidPackAnimal(leader)) {
            dissolveParty(
                    leader,
                    party,
                    false
            );
            return;
        }

        cleanPartyMembers(
                leader,
                party
        );

        /*
         * Highest priority:
         * if any party member sees edible food, hungry party members go eat.
         * The party stays alive so they can regroup afterward.
         */
        if (feedHungryPartyMembersFromSharedFood(level, leader, party, gameTime)) {
            return;
        }

        recruitPartyIfOpen(
                level,
                leader,
                party,
                gameTime
        );

        RetoldAiControlMode leaderMode = RetoldAiControl.getMode(leader);

        if (leaderMode == RetoldAiControlMode.HUNT) {
            LivingEntity prey = leader.getTarget();

            if (isValidPartyPrey(leader, prey, gameTime)) {
                updatePartyHunt(
                        leader,
                        party,
                        prey,
                        gameTime
                );
                return;
            }
        }

        LivingEntity preySeenByParty = findPreySensedByAnyPartyMember(
                level,
                leader,
                party,
                gameTime
        );

        if (preySeenByParty != null) {
            forcePartyHunt(
                    leader,
                    party,
                    preySeenByParty,
                    gameTime
            );
            return;
        }

        if (leaderMode == RetoldAiControlMode.SEARCH) {
            updatePartySearch(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        PathfinderMob hungryLeader = findHungryAvailablePartyLeader(
                leader,
                party,
                gameTime
        );

        if (hungryLeader != null) {
            if (hungryLeader != leader) {
                transferLeadership(
                        leader,
                        hungryLeader,
                        party
                );

                HuntingParty transferredParty = PARTIES_BY_LEADER.get(hungryLeader);

                if (transferredParty != null) {
                    startLeaderSearch(
                            hungryLeader,
                            transferredParty,
                            gameTime
                    );

                    updateLeaderParty(
                            level,
                            hungryLeader,
                            transferredParty,
                            gameTime
                    );
                }

                return;
            }

            startLeaderSearch(
                    leader,
                    party,
                    gameTime
            );

            updatePartySearch(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        if (hasAnyHungryPartyMember(leader, party, gameTime)) {
            holdPartyTogetherNearLeader(
                    leader,
                    party,
                    gameTime
            );
            return;
        }

        updatePartyReturn(
                level,
                leader,
                party,
                gameTime
        );
    }

    private static void updateMember(
            ServerLevel level,
            PathfinderMob leader,
            PathfinderMob member,
            HuntingParty party,
            long gameTime
    ) {
        if (!isValidPackAnimal(member)) {
            releaseMember(member);
            return;
        }

        /*
         * Member-side food broadcast:
         * if this member sees meat, hungry party members are sent to eat.
         */
        if (feedHungryPartyMembersFromSharedFood(level, leader, party, gameTime)) {
            return;
        }

        RetoldAiControlMode leaderMode = RetoldAiControl.getMode(leader);

        if (leaderMode == RetoldAiControlMode.SEARCH) {
            LivingEntity prey = findSensedPreyForSensor(
                    level,
                    member,
                    gameTime
            );

            if (prey != null) {
                forcePartyHunt(
                        leader,
                        party,
                        prey,
                        gameTime
                );
                return;
            }

            moveMemberInSearchFormation(
                    leader,
                    member,
                    party,
                    gameTime
            );
            return;
        }

        if (leaderMode == RetoldAiControlMode.HUNT) {
            LivingEntity prey = leader.getTarget();

            if (isValidPartyPrey(member, prey, gameTime)) {
                moveMemberInHunt(
                        member,
                        prey,
                        getPath(leader),
                        gameTime
                );
                return;
            }
        }

        if (hasAnyHungryPartyMember(leader, party, gameTime)) {
            holdMemberNearLeader(
                    leader,
                    member,
                    gameTime
            );
            return;
        }

        moveMemberBackToPack(
                member,
                party.packCenter,
                gameTime
        );
    }

    private static boolean feedHungryPartyMembersFromSharedFood(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        ItemEntity seenFood = findBestFoodSeenByParty(
                level,
                leader,
                party
        );

        if (seenFood == null) {
            return false;
        }

        boolean sentSomeoneToFood = false;

        if (isHungryEnoughToEat(leader, gameTime)) {
            sentSomeoneToFood |= sendMemberToFood(
                    level,
                    leader,
                    seenFood,
                    gameTime
            );
        }

        for (PathfinderMob member : party.members) {
            if (!isHungryEnoughToEat(member, gameTime)) {
                continue;
            }

            sentSomeoneToFood |= sendMemberToFood(
                    level,
                    member,
                    seenFood,
                    gameTime
            );
        }

        return sentSomeoneToFood;
    }

    private static boolean sendMemberToFood(
            ServerLevel level,
            PathfinderMob member,
            ItemEntity seenFood,
            long gameTime
    ) {
        if (level == null || member == null || seenFood == null) {
            return false;
        }

        if (!member.isAlive() || member.isRemoved()) {
            return false;
        }

        if (!seenFood.isAlive() || seenFood.isRemoved() || seenFood.getItem().isEmpty()) {
            return false;
        }

        if (!RetoldMobRules.canEatDroppedItem(member, seenFood.getItem())) {
            return false;
        }

        if (!canOverrideMemberForFood(member)) {
            return false;
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                false
        );

        RetoldPredatorStrike.clear(member);

        member.setSprinting(false);
        member.getNavigation().stop();

        RetoldAiControl.claim(
                member,
                RetoldAiControlMode.FEED,
                gameTime,
                PARTY_FEED_CONTROL_TICKS
        );

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    seenFood,
                    PARTY_FEED_SPEED
            );
        });

        return true;
    }

    private static ItemEntity findBestFoodSeenByParty(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party
    ) {
        ItemEntity best = findBestFoodSeenBySensor(
                level,
                leader
        );

        double bestDistance = best == null
                ? Double.MAX_VALUE
                : leader.distanceToSqr(best);

        for (PathfinderMob member : party.members) {
            ItemEntity food = findBestFoodSeenBySensor(
                    level,
                    member
            );

            if (food == null) {
                continue;
            }

            double distance = member.distanceToSqr(food);

            if (distance < bestDistance) {
                best = food;
                bestDistance = distance;
            }
        }

        return best;
    }

    private static ItemEntity findBestFoodSeenBySensor(
            ServerLevel level,
            PathfinderMob sensor
    ) {
        if (level == null || sensor == null || !sensor.isAlive() || sensor.isRemoved()) {
            return null;
        }

        AABB area = sensor.getBoundingBox().inflate(PARTY_FOOD_RADIUS_BLOCKS);

        List<ItemEntity> foods = level.getEntitiesOfClass(
                ItemEntity.class,
                area,
                item -> isObservablePartyFood(
                        sensor,
                        item
                )
        );

        ItemEntity bestFood = null;
        double bestDistance = Double.MAX_VALUE;

        for (ItemEntity food : foods) {
            double distanceSquared = sensor.distanceToSqr(food);

            if (distanceSquared > PARTY_FOOD_RADIUS_SQUARED) {
                continue;
            }

            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                bestFood = food;
            }
        }

        return bestFood;
    }

    private static boolean isObservablePartyFood(
            PathfinderMob sensor,
            ItemEntity item
    ) {
        if (sensor == null || item == null) {
            return false;
        }

        if (!item.isAlive() || item.isRemoved()) {
            return false;
        }

        if (item.getItem().isEmpty()) {
            return false;
        }

        if (sensor.distanceToSqr(item) > PARTY_FOOD_RADIUS_SQUARED) {
            return false;
        }

        /*
         * Food can be shared if one party member can actually notice it.
         */
        if (!sensor.hasLineOfSight(item) && sensor.distanceToSqr(item) > 25.0D) {
            return false;
        }

        return RetoldMobRules.canEatDroppedItem(
                sensor,
                item.getItem()
        );
    }

    private static boolean isHungryEnoughToEat(
            PathfinderMob member,
            long gameTime
    ) {
        if (!isValidPackAnimal(member)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                member,
                gameTime
        );

        return state.hunger() >= RetoldMobRules.eatThreshold(member);
    }

    private static boolean canOverrideMemberForFood(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP
                || mode == RetoldAiControlMode.FEED;
    }

    private static void forcePartyHunt(
            PathfinderMob leader,
            HuntingParty party,
            LivingEntity prey,
            long gameTime
    ) {
        if (!isValidPartyPrey(leader, prey, gameTime)) {
            return;
        }

        RetoldAiControl.claim(
                leader,
                RetoldAiControlMode.HUNT,
                gameTime,
                PARTY_HUNT_CONTROL_TICKS
        );

        leader.setSprinting(true);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                leader,
                prey
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                leader,
                true
        );

        leader.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            leader.getNavigation().moveTo(
                    prey,
                    getHuntSpeed(getPath(leader))
            );
        });

        updatePartyHunt(
                leader,
                party,
                prey,
                gameTime
        );
    }

    private static LivingEntity findPreySensedByAnyPartyMember(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        LivingEntity bestPrey = findSensedPreyForSensor(
                level,
                leader,
                gameTime
        );

        double bestScore = bestPrey == null
                ? Double.MAX_VALUE
                : leader.distanceToSqr(bestPrey);

        for (PathfinderMob member : party.members) {
            LivingEntity prey = findSensedPreyForSensor(
                    level,
                    member,
                    gameTime
            );

            if (prey == null) {
                continue;
            }

            double score = member.distanceToSqr(prey);

            if (score < bestScore) {
                bestScore = score;
                bestPrey = prey;
            }
        }

        return bestPrey;
    }

    private static LivingEntity findSensedPreyForSensor(
            ServerLevel level,
            PathfinderMob sensor,
            long gameTime
    ) {
        if (!isValidPackAnimal(sensor)) {
            return null;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(sensor);

        if (
                mode == RetoldAiControlMode.FEED
                        || mode == RetoldAiControlMode.FLEE
                        || mode == RetoldAiControlMode.ATTACK
                        || mode == RetoldAiControlMode.TERRITORY
                        || mode == RetoldAiControlMode.SHELTER
        ) {
            return null;
        }

        AABB area = sensor.getBoundingBox().inflate(PARTY_PREY_SENSE_RADIUS_BLOCKS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class,
                area,
                candidate -> isValidSensedPartyPrey(
                        sensor,
                        candidate,
                        gameTime
                )
        );

        LivingEntity bestPrey = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double distanceSquared = sensor.distanceToSqr(candidate);

            if (distanceSquared > PARTY_PREY_SENSE_RADIUS_SQUARED) {
                continue;
            }

            double score = distanceSquared;

            if (sensor.hasLineOfSight(candidate)) {
                score -= 24.0D;
            }

            if (RetoldMobRules.isSmallFoodPrey(candidate)) {
                score -= 10.0D;
            }

            if (score < bestScore) {
                bestScore = score;
                bestPrey = candidate;
            }
        }

        return bestPrey;
    }

    private static boolean isValidSensedPartyPrey(
            PathfinderMob sensor,
            LivingEntity prey,
            long gameTime
    ) {
        if (!isValidPartyPrey(sensor, prey, gameTime)) {
            return false;
        }

        if (sensor.distanceToSqr(prey) > PARTY_PREY_SENSE_RADIUS_SQUARED) {
            return false;
        }

        return canPartySensorSensePrey(
                sensor,
                prey
        );
    }

    private static boolean canPartySensorSensePrey(
            PathfinderMob sensor,
            LivingEntity prey
    ) {
        double distanceSquared = sensor.distanceToSqr(prey);

        if (
                distanceSquared <= PARTY_SIGHT_RADIUS_SQUARED
                        && sensor.hasLineOfSight(prey)
        ) {
            return true;
        }

        if (
                distanceSquared <= PARTY_HEARING_RADIUS_SQUARED
                        && isAudible(prey)
        ) {
            return true;
        }

        return distanceSquared <= PARTY_SMELL_RADIUS_SQUARED;
    }

    private static boolean isAudible(LivingEntity entity) {
        Vec3 movement = entity.getDeltaMovement();
        double horizontalMovementSquared = movement.x * movement.x + movement.z * movement.z;

        return horizontalMovementSquared >= AUDIBLE_MOVEMENT_THRESHOLD_SQUARED;
    }

    private static void updatePartySearch(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        cleanPartyMembers(
                leader,
                party
        );

        getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        int index = 0;

        for (PathfinderMob member : party.members) {
            moveMemberInSearchFormation(
                    leader,
                    member,
                    party,
                    gameTime,
                    index
            );

            index++;
        }
    }

    private static void updatePartyHunt(
            PathfinderMob leader,
            HuntingParty party,
            LivingEntity prey,
            long gameTime
    ) {
        cleanPartyMembers(
                leader,
                party
        );

        String path = getPath(leader);

        for (PathfinderMob member : party.members) {
            if (!isValidPartyPrey(member, prey, gameTime)) {
                holdMemberNearLeader(
                        leader,
                        member,
                        gameTime
                );
                continue;
            }

            moveMemberInHunt(
                    member,
                    prey,
                    path,
                    gameTime
            );
        }
    }

    private static void updatePartyReturn(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        cleanPartyMembers(
                leader,
                party
        );

        boolean leaderReturned = moveLeaderBackToPackIfFree(
                leader,
                party.packCenter,
                gameTime
        );

        boolean allMembersReturned = true;

        for (PathfinderMob member : party.members) {
            boolean memberReturned = moveMemberBackToPack(
                    member,
                    party.packCenter,
                    gameTime
            );

            if (!memberReturned) {
                allMembersReturned = false;
            }
        }

        if (leaderReturned && allMembersReturned) {
            dissolveParty(
                    leader,
                    party,
                    true
            );
        }
    }

    private static PathfinderMob findHungryAvailablePartyLeader(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        PathfinderMob best = null;
        int bestHunger = -1;

        if (isHungryEnoughToContinueParty(leader, gameTime) && canLeadContinuedSearch(leader)) {
            best = leader;
            bestHunger = RetoldMobStates.getOrCreate(
                    leader,
                    gameTime
            ).hunger();
        }

        for (PathfinderMob member : party.members) {
            if (!isHungryEnoughToContinueParty(member, gameTime)) {
                continue;
            }

            if (!canLeadContinuedSearch(member)) {
                continue;
            }

            int hunger = RetoldMobStates.getOrCreate(
                    member,
                    gameTime
            ).hunger();

            if (hunger > bestHunger) {
                best = member;
                bestHunger = hunger;
            }
        }

        return best;
    }

    private static boolean hasAnyHungryPartyMember(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        if (isHungryEnoughToContinueParty(leader, gameTime)) {
            return true;
        }

        for (PathfinderMob member : party.members) {
            if (isHungryEnoughToContinueParty(member, gameTime)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isHungryEnoughToContinueParty(
            PathfinderMob member,
            long gameTime
    ) {
        if (!isValidPackAnimal(member)) {
            return false;
        }

        RetoldMobState state = RetoldMobStates.getOrCreate(
                member,
                gameTime
        );

        return state.hunger() >= RetoldMobRules.huntThreshold(member);
    }

    private static boolean canLeadContinuedSearch(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP;
    }

    private static void startLeaderSearch(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        RetoldAiControl.claim(
                leader,
                RetoldAiControlMode.SEARCH,
                gameTime,
                PARTY_SEARCH_CONTROL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                leader,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                leader,
                false
        );

        RetoldPredatorStrike.clear(leader);

        leader.setSprinting(false);

        Vec3 direction = getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        Vec3 target = leader.position()
                .add(direction.scale(SEARCH_FORWARD_BLOCKS));

        leader.getLookControl().setLookAt(
                target.x,
                target.y + 0.5D,
                target.z,
                25.0F,
                25.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            leader.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    getSearchSpeed(getPath(leader))
            );
        });
    }

    private static void holdPartyTogetherNearLeader(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        int index = 0;

        for (PathfinderMob member : party.members) {
            holdMemberNearLeader(
                    leader,
                    member,
                    gameTime,
                    index
            );

            index++;
        }
    }

    private static void holdMemberNearLeader(
            PathfinderMob leader,
            PathfinderMob member,
            long gameTime
    ) {
        int index = Math.max(
                0,
                member.getId() % 4
        );

        holdMemberNearLeader(
                leader,
                member,
                gameTime,
                index
        );
    }

    private static void holdMemberNearLeader(
            PathfinderMob leader,
            PathfinderMob member,
            long gameTime,
            int index
    ) {
        if (!canOverrideMemberMode(member)) {
            return;
        }

        RetoldAiControl.claim(
                member,
                RetoldAiControlMode.REGROUP,
                gameTime,
                PARTY_RETURN_CONTROL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                false
        );

        RetoldPredatorStrike.clear(member);

        member.setSprinting(false);

        Vec3 direction = getLeaderDirection(
                leader
        );

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double sideOffset = switch (index % 4) {
            case 0 -> 3.0D;
            case 1 -> -3.0D;
            case 2 -> 5.0D;
            default -> -5.0D;
        };

        Vec3 target = leader.position()
                .add(side.scale(sideOffset));

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    target.x,
                    member.getY(),
                    target.z,
                    RETURN_TO_PACK_SPEED
            );
        });
    }

    private static void transferLeadership(
            PathfinderMob oldLeader,
            PathfinderMob newLeader,
            HuntingParty party
    ) {
        PARTIES_BY_LEADER.remove(oldLeader);
        LEADER_BY_MEMBER.remove(oldLeader);

        party.members.remove(newLeader);

        if (
                oldLeader != newLeader
                        && isValidPackAnimal(oldLeader)
                        && !party.members.contains(oldLeader)
        ) {
            party.members.add(oldLeader);
        }

        PARTIES_BY_LEADER.put(
                newLeader,
                party
        );

        LEADER_BY_MEMBER.put(
                newLeader,
                newLeader
        );

        for (PathfinderMob member : party.members) {
            LEADER_BY_MEMBER.put(
                    member,
                    newLeader
            );
        }
    }

    private static void moveMemberInSearchFormation(
            PathfinderMob leader,
            PathfinderMob member,
            HuntingParty party,
            long gameTime
    ) {
        int index = party.members.indexOf(member);

        if (index < 0) {
            index = 0;
        }

        moveMemberInSearchFormation(
                leader,
                member,
                party,
                gameTime,
                index
        );
    }

    private static void moveMemberInSearchFormation(
            PathfinderMob leader,
            PathfinderMob member,
            HuntingParty party,
            long gameTime,
            int index
    ) {
        if (!canOverrideMemberMode(member)) {
            return;
        }

        RetoldAiControl.claim(
                member,
                RetoldAiControlMode.SEARCH,
                gameTime,
                PARTY_SEARCH_CONTROL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                false
        );

        member.setSprinting(false);

        Vec3 target = getSearchFormationPoint(
                leader,
                member,
                party,
                gameTime,
                index
        );

        member.getLookControl().setLookAt(
                target.x,
                target.y + 0.5D,
                target.z,
                25.0F,
                25.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    target.x,
                    target.y,
                    target.z,
                    getSearchSpeed(getPath(leader))
            );
        });
    }

    private static void moveMemberInHunt(
            PathfinderMob member,
            LivingEntity prey,
            String leaderPath,
            long gameTime
    ) {
        if (!canOverrideMemberMode(member)) {
            return;
        }

        RetoldAiControl.claim(
                member,
                RetoldAiControlMode.HUNT,
                gameTime,
                PARTY_HUNT_CONTROL_TICKS
        );

        member.setSprinting(true);

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                prey
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                true
        );

        member.getLookControl().setLookAt(
                prey,
                35.0F,
                35.0F
        );

        RetoldAiControl.withNavigationBypass(() -> {
            member.getNavigation().moveTo(
                    prey,
                    getHuntSpeed(leaderPath)
            );
        });
    }

    private static boolean moveLeaderBackToPackIfFree(
            PathfinderMob leader,
            BlockPos packCenter,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(leader);

        /*
         * Do not dissolve while the leader is still feeding or otherwise busy.
         * Once FEED clears, the leader will physically return to pack center.
         */
        if (
                mode == RetoldAiControlMode.FEED
                        || mode == RetoldAiControlMode.FLEE
                        || mode == RetoldAiControlMode.ATTACK
                        || mode == RetoldAiControlMode.TERRITORY
                        || mode == RetoldAiControlMode.SHELTER
        ) {
            return false;
        }

        return moveBackToPack(
                leader,
                packCenter,
                gameTime
        );
    }

    private static boolean moveMemberBackToPack(
            PathfinderMob member,
            BlockPos packCenter,
            long gameTime
    ) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        if (
                mode == RetoldAiControlMode.FEED
                        || mode == RetoldAiControlMode.FLEE
                        || mode == RetoldAiControlMode.ATTACK
                        || mode == RetoldAiControlMode.TERRITORY
                        || mode == RetoldAiControlMode.SHELTER
        ) {
            return false;
        }

        return moveBackToPack(
                member,
                packCenter,
                gameTime
        );
    }

    private static boolean moveBackToPack(
            PathfinderMob mob,
            BlockPos packCenter,
            long gameTime
    ) {
        if (mob.blockPosition().distSqr(packCenter) <= PACK_RETURN_DISTANCE_SQUARED) {
            if (
                    RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.REGROUP)
                            || RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.SEARCH)
                            || RetoldAiControl.isControlledAs(mob, RetoldAiControlMode.HUNT)
            ) {
                RetoldAiControl.clear(mob);
            }

            RetoldFactionTargetGuards.setTargetIgnoringGuard(
                    mob,
                    null
            );

            RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                    mob,
                    false
            );

            RetoldPredatorStrike.clear(mob);
            mob.setSprinting(false);
            mob.getNavigation().stop();
            return true;
        }

        RetoldAiControl.claim(
                mob,
                RetoldAiControlMode.REGROUP,
                gameTime,
                PARTY_RETURN_CONTROL_TICKS
        );

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                false
        );

        RetoldPredatorStrike.clear(mob);

        mob.setSprinting(false);

        RetoldAiControl.withNavigationBypass(() -> {
            mob.getNavigation().moveTo(
                    packCenter.getX() + 0.5D,
                    packCenter.getY(),
                    packCenter.getZ() + 0.5D,
                    RETURN_TO_PACK_SPEED
            );
        });

        return false;
    }

    private static Vec3 getSearchFormationPoint(
            PathfinderMob leader,
            PathfinderMob member,
            HuntingParty party,
            long gameTime,
            int index
    ) {
        Vec3 direction = getPartySearchDirection(
                leader,
                party,
                gameTime
        );

        Vec3 side = new Vec3(
                -direction.z,
                0.0D,
                direction.x
        );

        double sideOffset = switch (index % 6) {
            case 0 -> SEARCH_SIDE_BLOCKS;
            case 1 -> -SEARCH_SIDE_BLOCKS;
            case 2 -> SEARCH_SIDE_BLOCKS * 0.5D;
            case 3 -> -SEARCH_SIDE_BLOCKS * 0.5D;
            case 4 -> SEARCH_SIDE_BLOCKS * 1.45D;
            default -> -SEARCH_SIDE_BLOCKS * 1.45D;
        };

        double forwardOffset = SEARCH_FORWARD_BLOCKS + (index / 2) * 2.0D;

        Vec3 target = leader.position()
                .add(direction.scale(forwardOffset))
                .add(side.scale(sideOffset));

        return new Vec3(
                target.x,
                member.getY(),
                target.z
        );
    }

    private static Vec3 getPartySearchDirection(
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        if (
                party.searchDirection == null
                        || party.searchDirection.lengthSqr() <= 0.0001D
                        || gameTime > party.searchDirectionExpiresAt
        ) {
            party.searchDirection = getLeaderDirection(
                    leader
            );

            party.searchDirectionExpiresAt = gameTime + PARTY_SEARCH_DIRECTION_LIFE_TICKS;
        }

        return safeDirection(
                leader,
                party.searchDirection
        );
    }

    private static Vec3 getLeaderDirection(PathfinderMob leader) {
        Vec3 movement = leader.getDeltaMovement();

        Vec3 horizontalMovement = new Vec3(
                movement.x,
                0.0D,
                movement.z
        );

        if (horizontalMovement.lengthSqr() > 0.0001D) {
            return horizontalMovement.normalize();
        }

        Vec3 look = leader.getLookAngle();

        Vec3 horizontalLook = new Vec3(
                look.x,
                0.0D,
                look.z
        );

        if (horizontalLook.lengthSqr() > 0.0001D) {
            return horizontalLook.normalize();
        }

        double angle = leader.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private static Vec3 safeDirection(
            PathfinderMob mob,
            Vec3 direction
    ) {
        if (direction != null && direction.lengthSqr() > 0.0001D) {
            return new Vec3(
                    direction.x,
                    0.0D,
                    direction.z
            ).normalize();
        }

        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;

        return new Vec3(
                Math.cos(angle),
                0.0D,
                Math.sin(angle)
        );
    }

    private static void recruitPartyIfOpen(
            ServerLevel level,
            PathfinderMob leader,
            HuntingParty party,
            long gameTime
    ) {
        if (level == null || leader == null || party == null) {
            return;
        }

        if (!isValidPackAnimal(leader)) {
            return;
        }

        String path = getPath(leader);

        int maxPartySize = getMaxPartySize(path);
        int currentPartySize = 1 + party.members.size();

        if (currentPartySize >= maxPartySize) {
            return;
        }

        double radius = getPackRadius(path);
        double radiusSquared = radius * radius;

        LivingEntity currentPrey = null;

        if (RetoldAiControl.isControlledAs(leader, RetoldAiControlMode.HUNT)) {
            currentPrey = leader.getTarget();
        }

        LivingEntity finalCurrentPrey = currentPrey;

        AABB area = leader.getBoundingBox().inflate(radius);

        List<PathfinderMob> candidates = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> isLateJoinCandidate(
                        leader,
                        candidate,
                        party,
                        finalCurrentPrey,
                        gameTime,
                        radiusSquared
                )
        );

        candidates.sort(
                Comparator.comparingDouble(candidate -> leader.distanceToSqr(candidate))
        );

        int openSlots = maxPartySize - currentPartySize;
        int joined = 0;

        for (PathfinderMob candidate : candidates) {
            if (joined >= openSlots) {
                return;
            }

            party.members.add(candidate);

            LEADER_BY_MEMBER.put(
                    candidate,
                    leader
            );

            joined++;
        }
    }

    private static boolean isLateJoinCandidate(
            PathfinderMob leader,
            PathfinderMob candidate,
            HuntingParty party,
            LivingEntity currentPrey,
            long gameTime,
            double radiusSquared
    ) {
        if (leader == null || candidate == null || party == null) {
            return false;
        }

        if (leader == candidate) {
            return false;
        }

        if (party.members.contains(candidate)) {
            return false;
        }

        if (LEADER_BY_MEMBER.containsKey(candidate)) {
            return false;
        }

        if (!isNormalPackCandidate(
                leader,
                candidate,
                radiusSquared
        )) {
            return false;
        }

        if (!canBeSelectedForHuntingParty(candidate)) {
            return false;
        }

        if (currentPrey != null) {
            return isValidPartyPrey(
                    candidate,
                    currentPrey,
                    gameTime
            );
        }

        return true;
    }

    private static boolean isNormalPackCandidate(
            PathfinderMob leader,
            PathfinderMob candidate,
            double radiusSquared
    ) {
        if (leader == null || candidate == null) {
            return false;
        }

        if (!isValidPackAnimal(candidate)) {
            return false;
        }

        if (leader.level() != candidate.level()) {
            return false;
        }

        if (!getPath(leader).equals(getPath(candidate))) {
            return false;
        }

        return leader.distanceToSqr(candidate) <= radiusSquared;
    }

    private static boolean canBeSelectedForHuntingParty(PathfinderMob candidate) {
        if (candidate == null) {
            return false;
        }

        if (LEADER_BY_MEMBER.containsKey(candidate)) {
            return false;
        }

        RetoldAiControlMode mode = RetoldAiControl.getMode(candidate);

        if (
                mode != RetoldAiControlMode.NONE
                        && mode != RetoldAiControlMode.SEARCH
                        && mode != RetoldAiControlMode.REGROUP
        ) {
            return false;
        }

        return candidate.getTarget() == null || !candidate.getTarget().isAlive();
    }

    private static boolean canOverrideMemberMode(PathfinderMob member) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        return mode == RetoldAiControlMode.NONE
                || mode == RetoldAiControlMode.SEARCH
                || mode == RetoldAiControlMode.HUNT
                || mode == RetoldAiControlMode.REGROUP;
    }

    private static void cleanPartyMembers(
            PathfinderMob leader,
            HuntingParty party
    ) {
        Iterator<PathfinderMob> iterator = party.members.iterator();

        while (iterator.hasNext()) {
            PathfinderMob member = iterator.next();

            if (!isValidPackAnimal(member)) {
                LEADER_BY_MEMBER.remove(member);
                iterator.remove();
                continue;
            }

            if (leader.distanceToSqr(member) > PACK_MEMBER_TOO_FAR_SQUARED) {
                releaseMember(member);
                iterator.remove();
            }
        }
    }

    private static void dissolveParty(
            PathfinderMob leader,
            HuntingParty party,
            boolean clearControlledMembers
    ) {
        PARTIES_BY_LEADER.remove(leader);
        LEADER_BY_MEMBER.remove(leader);

        if (clearControlledMembers) {
            clearPartyControlIfOwnedByParty(leader);
        }

        for (PathfinderMob member : party.members) {
            LEADER_BY_MEMBER.remove(member);

            if (clearControlledMembers) {
                clearPartyControlIfOwnedByParty(member);
            }
        }

        party.members.clear();
    }

    private static void releaseMember(PathfinderMob member) {
        LEADER_BY_MEMBER.remove(member);

        RetoldAiControlMode mode = RetoldAiControl.getMode(member);

        if (
                mode == RetoldAiControlMode.SEARCH
                        || mode == RetoldAiControlMode.HUNT
                        || mode == RetoldAiControlMode.REGROUP
        ) {
            RetoldAiControl.clear(member);
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                member,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                member,
                false
        );

        RetoldPredatorStrike.clear(member);
        member.setSprinting(false);
    }

    private static void clearPartyControlIfOwnedByParty(PathfinderMob mob) {
        RetoldAiControlMode mode = RetoldAiControl.getMode(mob);

        if (
                mode == RetoldAiControlMode.SEARCH
                        || mode == RetoldAiControlMode.HUNT
                        || mode == RetoldAiControlMode.REGROUP
        ) {
            RetoldAiControl.clear(mob);
        }

        RetoldFactionTargetGuards.setTargetIgnoringGuard(
                mob,
                null
        );

        RetoldFactionTargetGuards.setAggressiveIgnoringGuard(
                mob,
                false
        );

        RetoldPredatorStrike.clear(mob);
        mob.setSprinting(false);
    }

    private static BlockPos calculatePackCenter(
            PathfinderMob leader,
            List<PathfinderMob> nearbyPack
    ) {
        double x = leader.getX();
        double y = leader.getY();
        double z = leader.getZ();
        int count = 1;

        for (PathfinderMob member : nearbyPack) {
            if (member == leader) {
                continue;
            }

            x += member.getX();
            y += member.getY();
            z += member.getZ();
            count++;
        }

        return new BlockPos(
                (int) Math.floor(x / count),
                (int) Math.floor(y / count),
                (int) Math.floor(z / count)
        ).immutable();
    }

    private static boolean isValidPartyPrey(
            PathfinderMob predator,
            LivingEntity prey,
            long gameTime
    ) {
        if (predator == null || prey == null) {
            return false;
        }

        if (!prey.isAlive() || prey.isRemoved()) {
            return false;
        }

        if (predator.level() != prey.level()) {
            return false;
        }

        if (prey instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        return RetoldMobRules.canHuntPrey(
                predator,
                prey,
                gameTime
        );
    }

    private static boolean isValidPackAnimal(PathfinderMob mob) {
        if (mob == null || !mob.isAlive() || mob.isRemoved()) {
            return false;
        }

        if (!isPackHunter(mob)) {
            return false;
        }

        return !(mob instanceof TamableAnimal tamableAnimal && tamableAnimal.isTame());
    }

    private static boolean isPackHunter(PathfinderMob mob) {
        String path = getPath(mob);

        return path.equals("wolf")
                || path.equals("dolphin");
    }

    private static double getPackRadius(String path) {
        if (path.equals("dolphin")) {
            return DOLPHIN_PACK_RADIUS_BLOCKS;
        }

        return WOLF_PACK_RADIUS_BLOCKS;
    }

    private static int getMaxPartySize(String path) {
        if (path.equals("dolphin")) {
            return MAX_DOLPHIN_HUNTING_PARTY_SIZE;
        }

        return MAX_WOLF_HUNTING_PARTY_SIZE;
    }

    private static int getMinPartySize(String path) {
        if (path.equals("dolphin")) {
            return MIN_DOLPHIN_HUNTING_PARTY_SIZE;
        }

        return MIN_WOLF_HUNTING_PARTY_SIZE;
    }

    private static double getSearchSpeed(String path) {
        if (path.equals("dolphin")) {
            return DOLPHIN_SEARCH_SPEED;
        }

        return WOLF_SEARCH_SPEED;
    }

    private static double getHuntSpeed(String path) {
        if (path.equals("dolphin")) {
            return DOLPHIN_HUNT_SPEED;
        }

        return WOLF_HUNT_SPEED;
    }

    private static String getPath(PathfinderMob mob) {
        return RetoldMobRules.getEntityTypePath(
                mob.getType()
        );
    }

    private static final class HuntingParty {
        private final BlockPos packCenter;
        private final long createdAt;
        private final List<PathfinderMob> members = new ArrayList<>();

        private Vec3 searchDirection;
        private long searchDirectionExpiresAt;

        private HuntingParty(
                BlockPos packCenter,
                long createdAt
        ) {
            this.packCenter = packCenter;
            this.createdAt = createdAt;
        }
    }
}