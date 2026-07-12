package cz.xefensor.retold.territory;

import cz.xefensor.retold.faction.RetoldFaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RetoldTerritoryMobState {
    public RetoldTerritoryContext territoryContext;
    public long nextTerritoryContextRecheckAt;

    public List<PathfinderMob> nearbyFactionMobs = List.of();
    public RetoldFaction nearbyFactionMobsFaction;
    public long nextNearbyFactionMobsRecheckAt;

    public LivingEntity warningTarget;
    public LivingEntity attackTarget;

    public boolean hasStartedAttack;
    public int warningPulses;

    public long nextWarningPulseAt;
    public long nextTargetRecheckAt;
    public long nextFormationRecheckAt;
    public long nextAttackRefreshAt;

    public long finalWarningStartedAt = -1L;

    public long lastSawWarningTargetAt;
    public double lastKnownWarningTargetX;
    public double lastKnownWarningTargetY;
    public double lastKnownWarningTargetZ;

    public int warningFormationSlot;
    public double warningAnchorAngle;

    public boolean hasWarningMoveTarget;
    public double warningMoveTargetX;
    public double warningMoveTargetY;
    public double warningMoveTargetZ;
    public double warningMoveTargetSourceX;
    public double warningMoveTargetSourceZ;
    public int warningMoveTargetSlot = Integer.MIN_VALUE;
    public long nextWarningPathRefreshAt;

    public final Set<UUID> warnedIntruders = new HashSet<>();
}
