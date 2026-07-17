package cz.xefensor.retold.territory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

import java.util.EnumMap;
import java.util.Map;

public final class RetoldTerritoryStateMachine {
    private static final Map<RetoldTerritoryFlowState, FlowState> STATES = createStates();

    private RetoldTerritoryStateMachine() {
    }

    public static void tick(
            ServerLevel level,
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryConfig config,
            Map<PathfinderMob, RetoldTerritoryMobState> mobStates,
            long gameTime,
            boolean decisionTick
    ) {
        RetoldTerritoryStateContext context = new RetoldTerritoryStateContext(
                level,
                mob,
                state,
                config,
                mobStates,
                gameTime,
                decisionTick
        );

        if (state.flowState.continuesOutsideTerritory()) {
            currentState(state).tick(context);
            return;
        }

        state.territoryContext = RetoldTerritoryRules.refreshCachedMatchingContext(
                state,
                level,
                mob,
                config,
                gameTime
        );

        if (state.territoryContext == null) {
            deactivate(mob, state, gameTime);
            return;
        }

        if (state.flowState == RetoldTerritoryFlowState.COOLDOWN) {
            if (decisionTick && RetoldTerritoryCombat.tryAdoptRetaliationTarget(
                    level,
                    mob,
                    state,
                    config,
                    mobStates,
                    gameTime
            )) {
                currentState(state).tick(context);
                return;
            }

            currentState(state).tick(context);
            return;
        }

        if (!decisionTick && RetoldTerritoryCombat.suppressExistingTargetDuringWarning(
                level,
                mob,
                config,
                mobStates,
                gameTime
        )) {
            RetoldWarningPose.stopWarningPose(mob);
            return;
        }

        if (decisionTick) {
            if (tryStartExternalAttack(context)) {
                currentState(state).tick(context);
                return;
            }

            if (RetoldTerritoryCombat.suppressExistingTargetDuringWarning(
                    level,
                    mob,
                    config,
                    mobStates,
                    gameTime
            )) {
                RetoldWarningPose.stopWarningPose(mob);
                return;
            }

            RetoldTerritoryController.updateWarningTarget(
                    level,
                    mob,
                    state,
                    config,
                    mobStates,
                    gameTime
            );
            reconcileWarningState(mob, state, gameTime);
        }

        currentState(state).tick(context);
    }

    public static void transition(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            RetoldTerritoryFlowState nextState,
            long gameTime
    ) {
        RetoldTerritoryFlowState current = state.flowState;

        if (current == nextState) {
            return;
        }

        if (!current.canTransitionTo(nextState)) {
            throw new IllegalStateException("Illegal territory flow transition: " + current + " -> " + nextState);
        }

        STATES.get(current).exit(mob, state, nextState, gameTime);
        state.flowState = nextState;
        state.flowStateEnteredAt = gameTime;
        STATES.get(nextState).enter(mob, state, current, gameTime);
    }

    public static void reconcileWarningState(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        if (state.warningTarget == null || state.territoryContext == null) {
            deactivate(mob, state, gameTime);
            return;
        }

        RetoldWarningLevel warningLevel = RetoldTerritoryReputation.getWarningLevel(
                state.territoryContext,
                state.warningTarget
        );
        transition(mob, state, RetoldTerritoryFlowState.fromWarningLevel(warningLevel), gameTime);
    }

    public static void deactivate(
            PathfinderMob mob,
            RetoldTerritoryMobState state,
            long gameTime
    ) {
        if (state.flowState != RetoldTerritoryFlowState.INACTIVE) {
            transition(mob, state, RetoldTerritoryFlowState.INACTIVE, gameTime);
            return;
        }

        RetoldTerritoryController.clearWarningData(mob, state, gameTime);
    }

    private static boolean tryStartExternalAttack(RetoldTerritoryStateContext context) {
        return RetoldTerritoryCombat.tryAdoptRetaliationTarget(
                context.level(),
                context.mob(),
                context.state(),
                context.config(),
                context.mobStates(),
                context.gameTime()
        ) || RetoldTerritoryCombat.tryAdoptOwnedCombatTarget(
                context.level(),
                context.mob(),
                context.state(),
                context.config(),
                context.mobStates(),
                context.gameTime()
        ) || RetoldTerritoryCombat.tryAdoptExistingAttackTarget(
                context.level(),
                context.mob(),
                context.state(),
                context.config(),
                context.mobStates(),
                context.gameTime()
        );
    }

    private static FlowState currentState(RetoldTerritoryMobState state) {
        return STATES.get(state.flowState);
    }

    private static Map<RetoldTerritoryFlowState, FlowState> createStates() {
        EnumMap<RetoldTerritoryFlowState, FlowState> states = new EnumMap<>(RetoldTerritoryFlowState.class);
        states.put(RetoldTerritoryFlowState.INACTIVE, new InactiveState());
        states.put(RetoldTerritoryFlowState.OBSERVING, new WarningState());
        states.put(RetoldTerritoryFlowState.WARNING, new WarningState());
        states.put(RetoldTerritoryFlowState.FINAL_WARNING, new FinalWarningState());
        states.put(RetoldTerritoryFlowState.ATTACKING, new AttackingState());
        states.put(RetoldTerritoryFlowState.COOLDOWN, new CooldownState());
        return Map.copyOf(states);
    }

    private interface FlowState {
        default void enter(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState previousState,
                long gameTime
        ) {
        }

        void tick(RetoldTerritoryStateContext context);

        default void exit(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState nextState,
                long gameTime
        ) {
        }
    }

    private static final class InactiveState implements FlowState {
        @Override
        public void enter(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState previousState,
                long gameTime
        ) {
            RetoldTerritoryController.clearWarningData(mob, state, gameTime);
            state.cooldownUntil = 0L;
        }

        @Override
        public void tick(RetoldTerritoryStateContext context) {
        }
    }

    private static class WarningState implements FlowState {
        @Override
        public void tick(RetoldTerritoryStateContext context) {
            RetoldTerritoryController.tickWarningState(context);
        }
    }

    private static final class FinalWarningState extends WarningState {
        @Override
        public void enter(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState previousState,
                long gameTime
        ) {
            if (state.finalWarningStartedAt < 0L) {
                state.finalWarningStartedAt = gameTime;
            }
        }

        @Override
        public void exit(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState nextState,
                long gameTime
        ) {
            if (nextState != RetoldTerritoryFlowState.ATTACKING) {
                state.finalWarningStartedAt = -1L;
            }
        }
    }

    private static final class AttackingState implements FlowState {
        @Override
        public void enter(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState previousState,
                long gameTime
        ) {
            RetoldWarningPose.stopWarningPose(mob);
            state.warningPulses = 0;
            state.nextAttackRefreshAt = gameTime + 20L;
            state.cooldownUntil = 0L;
        }

        @Override
        public void tick(RetoldTerritoryStateContext context) {
            if (!context.decisionTick()) {
                return;
            }

            boolean attackActive = RetoldTerritoryCombat.updateAttackState(
                    context.level(),
                    context.mob(),
                    context.state(),
                    context.config(),
                    context.mobStates(),
                    context.gameTime()
            );

            if (!attackActive) {
                transition(
                        context.mob(),
                        context.state(),
                        RetoldTerritoryFlowState.COOLDOWN,
                        context.gameTime()
                );
            }
        }

        @Override
        public void exit(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState nextState,
                long gameTime
        ) {
            RetoldTerritoryCombat.releaseTerritoryAttack(mob, state);
        }
    }

    private static final class CooldownState implements FlowState {
        @Override
        public void enter(
                PathfinderMob mob,
                RetoldTerritoryMobState state,
                RetoldTerritoryFlowState previousState,
                long gameTime
        ) {
            RetoldTerritoryController.clearWarningData(mob, state, gameTime);
            state.cooldownUntil = gameTime + RetoldTerritoryConstants.ATTACK_COOLDOWN_TICKS;
        }

        @Override
        public void tick(RetoldTerritoryStateContext context) {
            if (context.gameTime() >= context.state().cooldownUntil) {
                transition(
                        context.mob(),
                        context.state(),
                        RetoldTerritoryFlowState.INACTIVE,
                        context.gameTime()
                );
            }
        }
    }
}
