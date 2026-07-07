package cz.xefensor.retold.behavior;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RetoldBehaviorDebugEvents {
    private static final Set<UUID> DEBUG_PLAYERS = new HashSet<>();

    private static final int DISPLAY_INTERVAL_TICKS = 10;
    private static final double LOOK_DISTANCE_BLOCKS = 24.0D;
    private static final double LOOK_DOT_THRESHOLD = 0.985D;

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
                                Commands.literal("clearcontrol")
                                        .executes(RetoldBehaviorDebugEvents::clearLookedMobControl)
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

    private static int setLookedMobHunger(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            source.sendFailure(
                    Component.literal("Mob is not in a server level.")
            );
            return 0;
        }

        int value = IntegerArgumentType.getInteger(
                context,
                "value"
        );

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                level.getGameTime()
        );

        state.setHunger(value);

        source.sendSuccess(
                () -> Component.literal(
                        "Set hunger of " + getEntityName(mob) + " to " + state.hunger()
                ),
                true
        );

        return 1;
    }

    private static int addLookedMobHunger(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        PathfinderMob mob = findLookedMobFromSource(source);

        if (mob == null) {
            source.sendFailure(
                    Component.literal("No mob found in view.")
            );
            return 0;
        }

        if (!(mob.level() instanceof ServerLevel level)) {
            source.sendFailure(
                    Component.literal("Mob is not in a server level.")
            );
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(
                context,
                "amount"
        );

        RetoldMobState state = RetoldMobStates.getOrCreate(
                mob,
                level.getGameTime()
        );

        state.addHunger(amount);

        source.sendSuccess(
                () -> Component.literal(
                        "Changed hunger of " + getEntityName(mob) + " by " + amount + ". New hunger: " + state.hunger()
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

        boolean managed = RetoldMobRules.isManagedMob(mob);
        boolean predator = RetoldMobRules.isManagedPredator(mob);

        String hunger = state == null
                ? "-"
                : Integer.toString(state.hunger());

        String target = mob.getTarget() == null
                ? "none"
                : getEntityName(mob.getTarget());

        String thresholds = managed
                ? " eat@" + RetoldMobRules.eatThreshold(mob) + " hunt@" + getSafeHuntThreshold(mob)
                : "";

        return "Retold "
                + getEntityName(mob)
                + " h=" + hunger
                + thresholds
                + " ctrl=" + controlMode
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

        return "\nRetold behavior debug"
                + "\nMob: " + getEntityName(mob) + " #" + mob.getId()
                + "\nManaged: " + yesNo(RetoldMobRules.isManagedMob(mob))
                + "\nPredator: " + yesNo(RetoldMobRules.isManagedPredator(mob))
                + "\nHunger: " + state.hunger()
                + "\nEat threshold: " + RetoldMobRules.eatThreshold(mob)
                + "\nHunt threshold: " + getSafeHuntThreshold(mob)
                + "\nHunger interval: " + RetoldMobRules.hungerInterval(mob) + " ticks"
                + "\nStress: " + state.stress()
                + "\nConfidence: " + state.confidence()
                + "\nControl: " + RetoldAiControl.getMode(mob)
                + "\nLast ate: " + ticksAgoText(gameTime, state.lastAteAt())
                + "\nLast hunger tick: " + ticksAgoText(gameTime, state.lastHungerTickAt())
                + "\nTarget: " + (target == null ? "none" : getEntityName(target) + " #" + target.getId());
    }

    private static int getSafeHuntThreshold(PathfinderMob mob) {
        try {
            return RetoldMobRules.huntThreshold(mob);
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

        return getEntityTypePath(entity.getType());
    }

    private static String getEntityTypePath(EntityType<?> entityType) {
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        int separator = id.indexOf(':');

        if (separator < 0 || separator + 1 >= id.length()) {
            return id;
        }

        return id.substring(separator + 1);
    }
}