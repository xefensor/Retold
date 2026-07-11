package cz.xefensor.retold.behavior;

import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

public final class RetoldPreyTargeting {
    private RetoldPreyTargeting() {
    }

    public static boolean isValidMobRulePrey(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        return isValidNonPlayerPreyCandidate(
                hunter,
                prey
        ) && RetoldMobRules.canHuntPrey(
                hunter,
                prey,
                gameTime
        );
    }

    public static String shortMobRulePreyDecision(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        if (isValidMobRulePrey(
                hunter,
                prey,
                gameTime
        )) {
            return "valid";
        }

        return invalidMobRulePreyReason(
                hunter,
                prey,
                gameTime
        );
    }

    public static String debugMobRulePreyDecision(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        String hunterFaction = factionText(
                hunter
        );
        String preyFaction = factionText(
                prey
        );
        String relation = relationText(
                hunter,
                prey
        );

        return "valid="
                + yesNo(
                isValidMobRulePrey(
                        hunter,
                        prey,
                        gameTime
                )
        )
                + " reason="
                + invalidMobRulePreyReason(
                hunter,
                prey,
                gameTime
        )
                + " hunterFaction="
                + hunterFaction
                + " targetFaction="
                + preyFaction
                + " relation="
                + relation;
    }

    public static boolean isValidNonPlayerPreyCandidate(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        return isValidLivingPreyCandidate(
                hunter,
                prey
        ) && !(prey instanceof Player);
    }

    public static boolean isValidLivingPreyCandidate(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        if (hunter == null || prey == null) {
            return false;
        }

        if (hunter == prey) {
            return false;
        }

        if (!RetoldBehaviorCoordinator.isValidAssignmentTarget(hunter, prey)) {
            return false;
        }

        return !isBlockedByFactionRelationship(
                hunter,
                prey
        );
    }

    public static boolean isTinyWetlandPrey(LivingEntity entity) {
        if (isSmallArthropodPrey(entity)) {
            return true;
        }

        if (!isSlimePrey(entity)) {
            return false;
        }

        return entity.getBbWidth() <= 1.6F
                && entity.getBbHeight() <= 1.6F;
    }

    public static boolean isSmallArthropodPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = RetoldMobRules.getEntityTypePath(entity.getType());

        return path.equals("silverfish")
                || path.equals("endermite");
    }

    public static boolean isSlimePrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = RetoldMobRules.getEntityTypePath(entity.getType());

        return path.equals("slime")
                || path.equals("magma_cube");
    }

    public static boolean isFishPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = RetoldMobRules.getEntityTypePath(entity.getType());

        return path.equals("cod")
                || path.equals("salmon")
                || path.equals("tropical_fish")
                || path.equals("pufferfish");
    }

    public static boolean isSquidPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        String path = RetoldMobRules.getEntityTypePath(entity.getType());

        return path.equals("squid")
                || path.equals("glow_squid");
    }

    public static boolean isDrownedPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return RetoldMobRules.isEntityPath(
                entity,
                "drowned"
        );
    }

    public static boolean isGuardianPrey(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        return RetoldMobRules.isEntityPath(
                entity,
                "guardian"
        );
    }

    private static boolean isBlockedByFactionRelationship(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        RetoldFaction preyFaction = RetoldFactionMembers.getFaction(prey);

        if (preyFaction == null) {
            return false;
        }

        RetoldFaction hunterFaction = RetoldFactionMembers.getFaction(hunter);

        if (hunterFaction == null) {
            return false;
        }

        if (hunterFaction == preyFaction) {
            return true;
        }

        return !RetoldFactionRelations.shouldAttack(
                hunter,
                prey
        );
    }

    private static String invalidMobRulePreyReason(
            PathfinderMob hunter,
            LivingEntity prey,
            long gameTime
    ) {
        if (hunter == null) {
            return "hunter_missing";
        }

        if (prey == null) {
            return "no_target";
        }

        if (hunter == prey) {
            return "self";
        }

        if (!prey.isAlive() || prey.isRemoved()) {
            return "target_dead_or_removed";
        }

        if (hunter.level() != prey.level()) {
            return "different_level";
        }

        if (RetoldBehaviorCoordinator.isInvalidPlayerTarget(prey)) {
            return "untargetable_player";
        }

        if (prey instanceof Player) {
            return "player_not_food_prey";
        }

        RetoldFaction hunterFaction = RetoldFactionMembers.getFaction(hunter);
        RetoldFaction preyFaction = RetoldFactionMembers.getFaction(prey);

        if (hunterFaction != null && preyFaction != null) {
            if (hunterFaction == preyFaction) {
                return "same_faction";
            }

            if (!RetoldFactionRelations.shouldAttack(
                    hunter,
                    prey
            )) {
                return "not_enemy_faction";
            }
        }

        if (!RetoldMobRules.canHuntPrey(
                hunter,
                prey,
                gameTime
        )) {
            return "not_food_prey";
        }

        return "valid";
    }

    private static String relationText(
            PathfinderMob hunter,
            LivingEntity prey
    ) {
        if (hunter == null || prey == null) {
            return "none";
        }

        RetoldFaction hunterFaction = RetoldFactionMembers.getFaction(hunter);
        RetoldFaction preyFaction = RetoldFactionMembers.getFaction(prey);

        if (hunterFaction == null || preyFaction == null) {
            return "unfactioned";
        }

        if (hunterFaction == preyFaction) {
            return "same";
        }

        return RetoldFactionRelations.shouldAttack(
                hunter,
                prey
        ) ? "enemy" : "neutral";
    }

    private static String factionText(LivingEntity entity) {
        if (entity == null) {
            return "none";
        }

        RetoldFaction faction = RetoldFactionMembers.getFaction(entity);

        return faction == null
                ? "none"
                : faction.name();
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }
}
