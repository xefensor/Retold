package cz.xefensor.retold.behavior;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;
import java.util.UUID;

public final class RetoldBehaviorDebugEvents {
    private static final Set<UUID> DEBUG_PLAYERS = new HashSet<>();

    private static final int DISPLAY_INTERVAL_TICKS = 10;
    private static final double LOOK_DISTANCE_BLOCKS = 24.0D;
    private static final double LOOK_DOT_THRESHOLD = 0.985D;
    private static final int DEFAULT_NEARBY_RADIUS_BLOCKS = 24;
    private static final int MAX_NEARBY_RADIUS_BLOCKS = 96;
    private static final int MAX_NEARBY_MOBS_SHOWN = 32;
    private static final int DEFAULT_SHOW_HOME_RADIUS_BLOCKS = 32;

    private RetoldBehaviorDebugEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("retoldbehavior")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(
                                Commands.literal("toggle")
                                        .executes(RetoldBehaviorDebugEvents::toggleDebug)
                        )
                        .then(
                                Commands.literal("get")
                                        .executes(RetoldBehaviorDebugEvents::printLookedMobDebug)
                        )
                        .then(
                                Commands.literal("home")
                                        .executes(RetoldBehaviorDebugEvents::printLookedMobHomeDebug)
                        )
                        .then(
                                Commands.literal("showhomes")
                                        .executes(context -> showNearbyHomes(
                                                context,
                                                DEFAULT_SHOW_HOME_RADIUS_BLOCKS
                                        ))
                                        .then(
                                                Commands.argument(
                                                                "radius",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        MAX_NEARBY_RADIUS_BLOCKS
                                                                )
                                                        )
                                                        .executes(context -> showNearbyHomes(
                                                                context,
                                                                IntegerArgumentType.getInteger(
                                                                        context,
                                                                        "radius"
                                                                )
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("pack")
                                        .executes(RetoldBehaviorDebugEvents::printLookedMobPackDebug)
                        )
                        .then(
                                Commands.literal("nearby")
                                        .executes(context -> printNearbyBehaviorDebug(
                                                context,
                                                DEFAULT_NEARBY_RADIUS_BLOCKS
                                        ))
                                        .then(
                                                Commands.argument(
                                                                "radius",
                                                                IntegerArgumentType.integer(
                                                                        1,
                                                                        MAX_NEARBY_RADIUS_BLOCKS
                                                                )
                                                        )
                                                        .executes(context -> printNearbyBehaviorDebug(
                                                                context,
                                                                IntegerArgumentType.getInteger(
                                                                        context,
                                                                        "radius"
                                                                )
                                                        ))
                                        )
                        )
                        .then(
                                Commands.literal("sethunger")
                                        .then(
                                                Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(0, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::setLookedMobHunger)
                                        )
                        )
                        .then(
                                Commands.literal("addhunger")
                                        .then(
                                                Commands.argument(
                                                                "amount",
                                                                IntegerArgumentType.integer(-100, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::addLookedMobHunger)
                                        )
                        )
                        .then(
                                Commands.literal("setstress")
                                        .then(
                                                Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(0, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::setLookedMobStress)
                                        )
                        )
                        .then(
                                Commands.literal("addstress")
                                        .then(
                                                Commands.argument(
                                                                "amount",
                                                                IntegerArgumentType.integer(-100, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::addLookedMobStress)
                                        )
                        )
                        .then(
                                Commands.literal("setconfidence")
                                        .then(
                                                Commands.argument(
                                                                "value",
                                                                IntegerArgumentType.integer(0, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::setLookedMobConfidence)
                                        )
                        )
                        .then(
                                Commands.literal("addconfidence")
                                        .then(
                                                Commands.argument(
                                                                "amount",
                                                                IntegerArgumentType.integer(-100, 100)
                                                        )
                                                        .executes(RetoldBehaviorDebugEvents::addLookedMobConfidence)
                                        )
                        )
                        .then(
                                Commands.literal("clearcontrol")
                                        .executes(RetoldBehaviorDebugEvents::clearLookedMobControl)
                        )
                        .then(
                                Commands.literal("clearhome")
                                        .executes(RetoldBehaviorDebugEvents::clearLookedMobHome)
                        )
                        .then(
                                Commands.literal("count")
                                        .executes(RetoldBehaviorDebugEvents::printCounts)
                        )
        );
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        long gameTime = event.getServer().overworld().getGameTime();

        if (gameTime % DISPLAY_INTERVAL_TICKS != 0L) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!DEBUG_PLAYERS.contains(player.getUUID())) {
                continue;
            }

            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }

            PathfinderMob mob = findLookedMob(
                    level,
                    player
            );

            if (mob == null) {
                player.sendSystemMessage(
                        Component.literal("Retold: no mob"),
                        true
                );
                continue;
            }

            player.sendSystemMessage(
                    Component.literal(
                            buildDebugLine(
                                    mob,
                                    level.getGameTime()
                            )
                    ),
                    true
            );
        }
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.literal("This command must be run by a player.")
            );
            return 0;
        }

        UUID playerUuid = player.getUUID();

        if (DEBUG_PLAYERS.contains(playerUuid)) {
            DEBUG_PLAYERS.remove(playerUuid);

            source.sendSuccess(
                    () -> Component.literal("Retold behavior debug: OFF"),
                    false
            );
            return 1;
        }

        DEBUG_PLAYERS.add(playerUuid);

        source.sendSuccess(
                () -> Component.literal("Retold behavior debug: ON"),
                false
        );

        return 1;
    }

    private static int printLookedMobDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        long gameTime = mob.level() instanceof ServerLevel level
                ? level.getGameTime()
                : 0L;

        source.sendSuccess(
                () -> Component.literal(
                        buildFullDebugText(
                                mob,
                                gameTime
                        )
                ),
                false
        );

        return 1;
    }

    private static int printLookedMobHomeDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        long gameTime = mob.level() instanceof ServerLevel level
                ? level.getGameTime()
                : 0L;

        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        source.sendSuccess(
                () -> Component.literal(
                        getEntityName(mob)
                                + " home: "
                                + fullHomeText(
                                mob,
                                home,
                                gameTime
                        )
                ),
                false
        );

        return 1;
    }

    private static int printLookedMobPackDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        RetoldPackHuntingEvents.debugPackText(mob)
                ),
                false
        );

        return 1;
    }

    private static int showNearbyHomes(
            CommandContext<CommandSourceStack> context,
            int radius
    ) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.literal("This command must be run by a player.")
            );
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(
                    Component.literal("Player is not in a server level.")
            );
            return 0;
        }

        double radiusSquared = (double) radius * (double) radius;
        AABB area = player.getBoundingBox().inflate(radius);
        List<PathfinderMob> mobs = level.getEntitiesOfClass(
                PathfinderMob.class,
                area,
                candidate -> candidate.isAlive()
                        && !candidate.isRemoved()
                        && candidate.distanceToSqr(player) <= radiusSquared
                        && RetoldAnimalHomes.get(candidate) != null
        );

        int shown = 0;

        for (PathfinderMob mob : mobs) {
            RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

            if (!RetoldAnimalHomes.isValidFor(level, mob, home)) {
                continue;
            }

            showHomeParticles(
                    level,
                    home.pos()
            );
            shown++;
        }

        int finalShown = shown;

        source.sendSuccess(
                () -> Component.literal("Shown Retold homes: " + finalShown),
                false
        );

        return shown > 0 ? 1 : 0;
    }

    private static int printNearbyBehaviorDebug(
            CommandContext<CommandSourceStack> context,
            int radius
    ) {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(
                    Component.literal("This command must be run by a player.")
            );
            return 0;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            source.sendFailure(
                    Component.literal("Player is not in a server level.")
            );
            return 0;
        }

        long gameTime = level.getGameTime();
        double radiusSquared = (double) radius * (double) radius;
        AABB area = player.getBoundingBox().inflate(radius);
        List<PathfinderMob> mobs = new ArrayList<>(
                level.getEntitiesOfClass(
                        PathfinderMob.class,
                        area,
                        candidate -> candidate.isAlive()
                                && !candidate.isRemoved()
                                && candidate.distanceToSqr(player) <= radiusSquared
                                && isUsefulNearbyDebugMob(candidate)
                )
        );

        mobs.sort(
                Comparator.comparingDouble(candidate -> candidate.distanceToSqr(player))
        );

        source.sendSuccess(
                () -> Component.literal(
                        buildNearbyDebugText(
                                level,
                                player,
                                mobs,
                                radius,
                                gameTime
                        )
                ),
                false
        );

        return 1;
    }

    private static int setLookedMobHunger(CommandContext<CommandSourceStack> context) {
        return setLookedMobStateValue(
                context,
                "value",
                "hunger",
                RetoldMobState::setHunger,
                RetoldMobState::hunger
        );
    }

    private static int addLookedMobHunger(CommandContext<CommandSourceStack> context) {
        return addLookedMobStateValue(
                context,
                "amount",
                "hunger",
                RetoldMobState::addHunger,
                RetoldMobState::hunger
        );
    }

    private static int setLookedMobStress(CommandContext<CommandSourceStack> context) {
        return setLookedMobStateValue(
                context,
                "value",
                "stress",
                RetoldMobState::setStress,
                RetoldMobState::stress
        );
    }

    private static int addLookedMobStress(CommandContext<CommandSourceStack> context) {
        return addLookedMobStateValue(
                context,
                "amount",
                "stress",
                RetoldMobState::addStress,
                RetoldMobState::stress
        );
    }

    private static int setLookedMobConfidence(CommandContext<CommandSourceStack> context) {
        return setLookedMobStateValue(
                context,
                "value",
                "confidence",
                RetoldMobState::setConfidence,
                RetoldMobState::confidence
        );
    }

    private static int addLookedMobConfidence(CommandContext<CommandSourceStack> context) {
        return addLookedMobStateValue(
                context,
                "amount",
                "confidence",
                RetoldMobState::addConfidence,
                RetoldMobState::confidence
        );
    }

    private static int setLookedMobStateValue(
            CommandContext<CommandSourceStack> context,
            String argumentName,
            String label,
            ObjIntConsumer<RetoldMobState> setter,
            ToIntFunction<RetoldMobState> getter
    ) {
        CommandSourceStack source = context.getSource();
        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        RetoldMobState state = getOrCreateDebugState(
                source,
                mob
        );

        if (state == null) {
            return 0;
        }

        int value = IntegerArgumentType.getInteger(
                context,
                argumentName
        );

        setter.accept(
                state,
                value
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Set " + label + " of " + getEntityName(mob) + " to " + getter.applyAsInt(state)
                ),
                true
        );

        return 1;
    }

    private static int addLookedMobStateValue(
            CommandContext<CommandSourceStack> context,
            String argumentName,
            String label,
            ObjIntConsumer<RetoldMobState> adder,
            ToIntFunction<RetoldMobState> getter
    ) {
        CommandSourceStack source = context.getSource();
        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        RetoldMobState state = getOrCreateDebugState(
                source,
                mob
        );

        if (state == null) {
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(
                context,
                argumentName
        );

        adder.accept(
                state,
                amount
        );

        source.sendSuccess(
                () -> Component.literal(
                        "Changed " + label + " of " + getEntityName(mob) + " by " + amount + ". New " + label + ": " + getter.applyAsInt(state)
                ),
                true
        );

        return 1;
    }

    private static int clearLookedMobControl(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        RetoldAiControl.clear(mob);

        source.sendSuccess(
                () -> Component.literal(
                        "Cleared Retold control for " + getEntityName(mob)
                ),
                true
        );

        return 1;
    }

    private static int clearLookedMobHome(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        RetoldAnimalHomes.remove(mob);

        source.sendSuccess(
                () -> Component.literal(
                        "Cleared Retold home for " + getEntityName(mob)
                ),
                true
        );

        return 1;
    }

    private static RetoldMobState getOrCreateDebugState(
            CommandSourceStack source,
            PathfinderMob mob
    ) {
        if (!(mob.level() instanceof ServerLevel level)) {
            source.sendFailure(
                    Component.literal("Mob is not in a server level.")
            );
            return null;
        }

        return RetoldMobStates.getOrCreate(
                mob,
                level.getGameTime()
        );
    }

    private static int printCounts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        source.sendSuccess(
                () -> Component.literal(
                        "Retold behavior states: "
                                + RetoldMobStates.activeCount()
                                + ", controls: "
                                + RetoldAiControl.activeCount()
                ),
                false
        );

        return 1;
    }

    private static PathfinderMob findLookedMobFromSource(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return null;
        }

        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }

        return findLookedMob(
                level,
                player
        );
    }

    private static PathfinderMob findLookedMob(
            ServerLevel level,
            ServerPlayer player
    ) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reach = eye.add(
                look.x * LOOK_DISTANCE_BLOCKS,
                look.y * LOOK_DISTANCE_BLOCKS,
                look.z * LOOK_DISTANCE_BLOCKS
        );

        PathfinderMob bestMob = null;
        double bestScore = Double.MAX_VALUE;

        for (
                PathfinderMob mob : level.getEntitiesOfClass(
                PathfinderMob.class,
                player.getBoundingBox().inflate(LOOK_DISTANCE_BLOCKS),
                candidate -> candidate.isAlive() && !candidate.isRemoved()
        )
        ) {
            Vec3 center = new Vec3(
                    mob.getX(),
                    mob.getY() + mob.getBbHeight() * 0.5D,
                    mob.getZ()
            );

            Vec3 toMob = center.subtract(eye);
            double distance = toMob.length();

            if (distance <= 0.001D || distance > LOOK_DISTANCE_BLOCKS) {
                continue;
            }

            Vec3 direction = toMob.normalize();
            double dot = look.dot(direction);

            if (dot < LOOK_DOT_THRESHOLD) {
                continue;
            }

            double lineDistance = distanceToLineSegment(
                    center,
                    eye,
                    reach
            );

            double score = lineDistance * 16.0D + distance * 0.1D - dot;

            if (score < bestScore) {
                bestScore = score;
                bestMob = mob;
            }
        }

        return bestMob;
    }

    private static double distanceToLineSegment(
            Vec3 point,
            Vec3 start,
            Vec3 end
    ) {
        Vec3 segment = end.subtract(start);
        double lengthSquared = segment.lengthSqr();

        if (lengthSquared <= 0.0001D) {
            return point.distanceTo(start);
        }

        double t = point.subtract(start).dot(segment) / lengthSquared;

        if (t < 0.0D) {
            t = 0.0D;
        }

        if (t > 1.0D) {
            t = 1.0D;
        }

        Vec3 closest = start.add(segment.scale(t));

        return point.distanceTo(closest);
    }

    private static String buildDebugLine(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.get(mob);
        RetoldAiControlMode controlMode = RetoldAiControl.getMode(mob);
        RetoldMobProfileType profileType = RetoldMobRules.profileType(mob);
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        boolean managed = RetoldMobRules.isManagedMob(mob);
        boolean predator = RetoldMobRules.isManagedPredator(mob);

        String hunger = state == null
                ? "-"
                : Integer.toString(state.hunger());

        String target = mob.getTarget() == null
                ? "none"
                : getEntityName(mob.getTarget());

        String thresholds = managed
                ? " eat@" + RetoldMobRules.eatThreshold(mob) + " hunt@" + getSafeHuntThresholdText(mob, state)
                : "";

        return "Retold "
                + getEntityName(mob)
                + " h=" + hunger
                + thresholds
                + " profile=" + profileType
                + " home=" + shortHomeText(home)
                + " rhythm=" + rhythmText(mob)
                + " ctrl=" + controlMode
                + controlReasonText(mob)
                + " managed=" + yesNo(managed)
                + " pred=" + yesNo(predator)
                + " target=" + target;
    }

    private static String buildFullDebugText(
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                gameTime
        );

        LivingEntity target = mob.getTarget();
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        return "\nRetold behavior debug"
                + "\nMob: " + getEntityName(mob) + " #" + mob.getId()
                + "\nProfile: " + RetoldMobRules.profileType(mob)
                + "\nHome: " + fullHomeText(mob, home, gameTime)
                + "\nRhythm: " + rhythmText(mob)
                + "\nManaged: " + yesNo(RetoldMobRules.isManagedMob(mob))
                + "\nPredator: " + yesNo(RetoldMobRules.isManagedPredator(mob))
                + "\nHunger: " + state.hunger()
                + "\nEat threshold: " + RetoldMobRules.eatThreshold(mob)
                + "\nHunt threshold: " + getSafeBaseHuntThreshold(mob)
                + "\nAdjusted hunt threshold: " + getSafeAdjustedHuntThreshold(mob, state)
                + "\nHunger interval: " + RetoldMobRules.hungerInterval(mob) + " ticks"
                + "\nStress: " + state.stress()
                + "\nConfidence: " + state.confidence()
                + "\nControl: " + RetoldAiControl.getMode(mob)
                + "\nControl owner: " + RetoldAiControl.getOwner(mob)
                + "\nControl priority: " + RetoldAiControl.getPriority(mob)
                + "\nControl reason: " + safeControlReason(mob)
                + "\nLast ate: " + ticksAgoText(gameTime, state.lastAteAt())
                + "\nLast danger: " + ticksAgoText(gameTime, state.lastDangerAt())
                + "\nLast flee end: " + ticksAgoText(gameTime, state.lastFleeEndedAt())
                + "\nLast hunt success: " + ticksAgoText(gameTime, state.lastSuccessfulHuntAt())
                + "\nLast hunt fail: " + ticksAgoText(gameTime, state.lastFailedHuntAt())
                + "\nLast hunger tick: " + ticksAgoText(gameTime, state.lastHungerTickAt())
                + "\nTarget: " + (target == null ? "none" : getEntityName(target) + " #" + target.getId());
    }

    private static String buildNearbyDebugText(
            ServerLevel level,
            ServerPlayer player,
            List<PathfinderMob> mobs,
            int radius,
            long gameTime
    ) {
        StringBuilder text = new StringBuilder();

        text.append("\nRetold nearby behavior debug");
        text.append("\nRadius: ").append(radius);
        text.append(" blocks");
        text.append("\nMobs: ").append(mobs.size());

        int shown = Math.min(
                mobs.size(),
                MAX_NEARBY_MOBS_SHOWN
        );

        if (shown <= 0) {
            text.append("\nNo Retold-managed mobs found nearby.");
            return text.toString();
        }

        text.append("\nShowing: ").append(shown);

        if (mobs.size() > shown) {
            text.append("/").append(mobs.size());
        }

        for (int index = 0; index < shown; index++) {
            PathfinderMob mob = mobs.get(index);

            text.append("\n- ");
            text.append(getEntityName(mob));
            text.append(" #").append(mob.getId());
            text.append(" d=").append(Math.round(Math.sqrt(mob.distanceToSqr(player))));
            text.append(" h=").append(hungerText(mob));
            text.append(" mode=").append(RetoldAiControl.getMode(mob));
            text.append(" target=").append(targetText(mob.getTarget()));
            text.append(" home=").append(nearbyHomeText(
                    level,
                    mob,
                    gameTime
            ));
        }

        return text.toString();
    }

    private static boolean isUsefulNearbyDebugMob(PathfinderMob mob) {
        return RetoldMobRules.isManagedMob(mob)
                || RetoldMobStates.get(mob) != null
                || RetoldAnimalHomes.get(mob) != null
                || RetoldAiControl.isControlled(mob);
    }

    private static String nearbyHomeText(
            ServerLevel level,
            PathfinderMob mob,
            long gameTime
    ) {
        RetoldAnimalHomeMemory home = RetoldAnimalHomes.get(mob);

        if (home == null) {
            return "none";
        }

        int currentGroupSize = RetoldAnimalHomes.currentHomeMemberCount(
                level,
                mob,
                home
        );
        int maxGroupSize = RetoldAnimalSocialGroups.maxHomeGroupSize(mob);

        return home.type()
                + "@"
                + home.pos().toShortString()
                + " "
                + currentGroupSize
                + "/"
                + maxGroupSize
                + " valid="
                + RetoldAnimalHomes.invalidReason(
                level,
                mob,
                home
        )
                + " used="
                + ticksAgoText(
                gameTime,
                home.lastUsedAt()
        );
    }

    private static void showHomeParticles(
            ServerLevel level,
            BlockPos pos
    ) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.0D;
        double z = pos.getZ() + 0.5D;

        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                x,
                y,
                z,
                8,
                0.35D,
                0.25D,
                0.35D,
                0.02D
        );
        level.sendParticles(
                ParticleTypes.END_ROD,
                x,
                y + 0.3D,
                z,
                4,
                0.2D,
                0.15D,
                0.2D,
                0.01D
        );
    }

    private static String hungerText(PathfinderMob mob) {
        RetoldMobState state = RetoldMobStates.get(mob);

        if (state == null) {
            return "-";
        }

        return Integer.toString(state.hunger());
    }

    private static String targetText(LivingEntity target) {
        if (target == null) {
            return "none";
        }

        return getEntityName(target) + " #" + target.getId();
    }

    private static String shortHomeText(RetoldAnimalHomeMemory home) {
        if (home == null) {
            return "none";
        }

        return home.type().name();
    }

    private static String fullHomeText(
            PathfinderMob mob,
            RetoldAnimalHomeMemory home,
            long gameTime
    ) {
        if (home == null) {
            return "none";
        }

        String validity = "unknown";
        String chunkLoaded = "unknown";
        String distance = "unknown";
        int maxGroupSize = RetoldAnimalSocialGroups.maxHomeGroupSize(mob);
        int currentGroupSize = 0;
        long separation = Math.round(
                RetoldAnimalSocialGroups.homeSeparationBlocks(mob)
        );

        if (mob.level() instanceof ServerLevel level) {
            validity = RetoldAnimalHomes.invalidReason(
                    level,
                    mob,
                    home
            );
            chunkLoaded = yesNo(
                    RetoldAnimalHomes.isChunkLoaded(
                            level,
                            home
                    )
            );

            double distanceSquared = RetoldAnimalHomes.distanceSquaredToHome(
                    mob,
                    home
            );
            currentGroupSize = RetoldAnimalHomes.currentHomeMemberCount(
                    level,
                    mob,
                    home
            );

            if (distanceSquared >= 0.0D) {
                distance = Long.toString(
                        Math.round(
                                Math.sqrt(distanceSquared)
                        )
                );
            }
        }

        return home.type()
                + " @ "
                + home.pos().toShortString()
                + " valid="
                + validity
                + " dist="
                + distance
                + " loaded="
                + chunkLoaded
                + " members="
                + currentGroupSize
                + "/"
                + maxGroupSize
                + " separate="
                + separation
                + " used "
                + ticksAgoText(gameTime, home.lastUsedAt());
    }

    private static String controlReasonText(PathfinderMob mob) {
        String reason = RetoldAiControl.getReason(mob);

        if (reason == null || reason.isBlank()) {
            return "";
        }

        return "(" + reason + ")";
    }

    private static String safeControlReason(PathfinderMob mob) {
        String reason = RetoldAiControl.getReason(mob);

        if (reason == null || reason.isBlank()) {
            return "none";
        }

        return reason;
    }

    private static String rhythmText(PathfinderMob mob) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return "unknown";
        }

        return RetoldAnimalDailyRhythm.isActive(level, mob)
                ? "active"
                : "home";
    }

    private static String getSafeHuntThresholdText(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        int base = getSafeBaseHuntThreshold(mob);
        int adjusted = getSafeAdjustedHuntThreshold(
                mob,
                state
        );

        if (base == adjusted) {
            return Integer.toString(base);
        }

        return base + "/" + adjusted;
    }

    private static int getSafeBaseHuntThreshold(PathfinderMob mob) {
        try {
            return RetoldMobRules.huntThreshold(mob);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int getSafeAdjustedHuntThreshold(
            PathfinderMob mob,
            RetoldMobState state
    ) {
        try {
            return RetoldMobRules.adjustedHuntThreshold(
                    mob,
                    state
            );
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String ticksAgoText(
            long gameTime,
            long timestamp
    ) {
        if (timestamp <= 0L) {
            return "never";
        }

        long ticksAgo = Math.max(
                0L,
                gameTime - timestamp
        );

        return ticksAgo + "t ago";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String getEntityName(Entity entity) {
        if (entity == null) {
            return "none";
        }

        return RetoldMobRules.getEntityTypePath(entity.getType());
    }
}
