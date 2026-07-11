package cz.xefensor.retold.territory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

public final class RetoldTerritoryDebug {
    private RetoldTerritoryDebug() {
    }

    public static boolean hasWarningDebug(PathfinderMob mob) {
        return RetoldTerritoryMobStates.get(mob) != null
                || RetoldTerritoryConfigs.getForEntity(mob) != null;
    }

    public static String shortWarningText(
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return "unknown";
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);
        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        if (config == null && state == null) {
            return "none";
        }

        RetoldTerritoryContext context = resolveContext(
                level,
                mob,
                config,
                state
        );
        LivingEntity warningTarget = state == null ? null : state.warningTarget;
        RetoldWarningLevel levelName = warningLevel(
                context,
                warningTarget
        );
        int suspicion = suspicion(
                context,
                warningTarget
        );

        return "ctx="
                + contextText(context)
                + " target="
                + entityText(warningTarget)
                + " level="
                + levelName
                + " susp="
                + suspicion
                + "/"
                + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + " attack="
                + yesNo(state != null && state.hasStartedAttack)
                + " pulses="
                + (state == null ? 0 : state.warningPulses)
                + " next="
                + nextTicksText(state == null ? 0L : state.nextWarningPulseAt, gameTime);
    }

    public static String fullWarningText(
            PathfinderMob mob,
            long gameTime
    ) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return "Territory warning: unknown level";
        }

        RetoldTerritoryConfig config = RetoldTerritoryConfigs.getForEntity(mob);
        RetoldTerritoryMobState state = RetoldTerritoryMobStates.get(mob);

        if (config == null && state == null) {
            return "Territory warning: none";
        }

        RetoldTerritoryContext context = resolveContext(
                level,
                mob,
                config,
                state
        );
        LivingEntity warningTarget = state == null ? null : state.warningTarget;
        LivingEntity attackTarget = state == null ? null : state.attackTarget;
        RetoldWarningLevel levelName = warningLevel(
                context,
                warningTarget
        );
        int suspicion = suspicion(
                context,
                warningTarget
        );

        return "Territory warning"
                + "\nConfig faction: " + (config == null ? "none" : config.faction)
                + "\nContext: " + contextText(context)
                + "\nWarning target: " + entityText(warningTarget)
                + "\nAttack target: " + entityText(attackTarget)
                + "\nWarning level: " + levelName
                + "\nSuspicion: " + suspicion + "/" + RetoldTerritoryConstants.REPUTATION_ATTACK_THRESHOLD
                + "\nStarted attack: " + yesNo(state != null && state.hasStartedAttack)
                + "\nWarning pulses: " + (state == null ? 0 : state.warningPulses)
                + "\nWarned intruders: " + (state == null ? 0 : state.warnedIntruders.size())
                + "\nNext warning pulse: "
                + nextTicksText(state == null ? 0L : state.nextWarningPulseAt, gameTime)
                + "\nNext target recheck: "
                + nextTicksText(state == null ? 0L : state.nextTargetRecheckAt, gameTime)
                + "\nFinal warning age: "
                + ageTicksText(state == null ? -1L : state.finalWarningStartedAt, gameTime)
                + "\nPrepared shot fired: "
                + yesNo(state != null && state.firedPreparedWarningShot);
    }

    private static RetoldTerritoryContext resolveContext(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryConfig config,
            RetoldTerritoryMobState state
    ) {
        RetoldTerritoryContext context = state == null ? null : state.territoryContext;

        if (context != null) {
            return context;
        }

        if (config == null) {
            return null;
        }

        return RetoldTerritoryDetector.getContextAt(
                level,
                mob.blockPosition()
        );
    }

    private static RetoldWarningLevel warningLevel(
            RetoldTerritoryContext context,
            LivingEntity warningTarget
    ) {
        if (context == null || warningTarget == null) {
            return RetoldWarningLevel.NONE;
        }

        return RetoldTerritoryReputation.getWarningLevel(
                context,
                warningTarget
        );
    }

    private static int suspicion(
            RetoldTerritoryContext context,
            LivingEntity warningTarget
    ) {
        if (context == null || warningTarget == null) {
            return 0;
        }

        return RetoldTerritoryReputation.getSuspicion(
                context,
                warningTarget
        );
    }

    private static String contextText(RetoldTerritoryContext context) {
        if (context == null) {
            return "none";
        }

        return context.debugName();
    }

    private static String entityText(Entity entity) {
        if (entity == null) {
            return "none";
        }

        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id.getPath() + " #" + entity.getId();
    }

    private static String nextTicksText(
            long timestamp,
            long gameTime
    ) {
        if (timestamp <= 0L) {
            return "none";
        }

        return Math.max(0L, timestamp - gameTime) + "t";
    }

    private static String ageTicksText(
            long timestamp,
            long gameTime
    ) {
        if (timestamp < 0L) {
            return "none";
        }

        return Math.max(0L, gameTime - timestamp) + "t";
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
