package cz.xefensor.retold.behavior.profiles;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import net.minecraft.world.entity.Entity;

public record RetoldMobIdentity(
        String species,
        RetoldFaction faction,
        RetoldFaction alignedFaction,
        RetoldMobProfile profile,
        RetoldMobState state
) {
    public static RetoldMobIdentity of(
            Entity entity,
            RetoldMobState state
    ) {
        RetoldMobProfile profile = RetoldMobProfiles.get(entity);

        return new RetoldMobIdentity(
                species(entity),
                RetoldFactionMembers.getFaction(entity),
                RetoldFactionMembers.getFactionOrLooseAllyFaction(entity),
                profile,
                state
        );
    }

    public RetoldMobProfileType profileType() {
        return profile == null
                ? RetoldMobProfileType.NONE
                : profile.type();
    }

    public boolean isProfile(RetoldMobProfileType type) {
        return type != null && profileType() == type;
    }

    public RetoldHungerStage hungerStage() {
        return RetoldMobRules.hungerStage(state);
    }

    public boolean managed() {
        return profile != null && profile.managed();
    }

    public boolean predator() {
        return profile != null && profile.predator();
    }

    public String factionText() {
        if (faction == null && alignedFaction == null) {
            return "none";
        }

        if (faction == alignedFaction || alignedFaction == null) {
            return name(faction);
        }

        if (faction == null) {
            return "loose:" + name(alignedFaction);
        }

        return name(faction) + "+ally:" + name(alignedFaction);
    }

    private static String species(Entity entity) {
        if (entity == null) {
            return "";
        }

        return RetoldMobRules.getEntityTypePath(entity.getType());
    }

    private static String name(RetoldFaction faction) {
        return faction == null ? "none" : faction.name();
    }
}
