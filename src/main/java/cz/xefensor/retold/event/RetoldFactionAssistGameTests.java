package cz.xefensor.retold.event;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionRelations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldFactionAssistGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldFactionAssistGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        event.registerTest(
                Identifier.fromNamespaceAndPath(
                        Retold.MODID,
                        "successful_attacks_recruit_only_social_allies"
                ),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true),
                        RetoldFactionAssistGameTests::successfulAttacksRecruitOnlySocialAllies
                )
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(
                        Retold.MODID,
                        "visible_attack_intent_triggers_pre_hit_response"
                ),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 80, 0, true),
                        RetoldFactionAssistGameTests::visibleAttackIntentTriggersPreHitResponse
                )
        );
    }

    private static void successfulAttacksRecruitOnlySocialAllies(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        Wolf wolfVictim = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 1, 2, 1);
        Wolf wolfAlly = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 3, 2, 1);
        Wolf busyWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 4, 2, 1);
        Wolf babyWolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 5, 2, 1);
        PathfinderMob polarBearVictim = helper.spawnWithNoFreeWill(
                EntityTypes.POLAR_BEAR,
                7,
                2,
                1
        );
        PathfinderMob polarBearAlly = helper.spawnWithNoFreeWill(
                EntityTypes.POLAR_BEAR,
                9,
                2,
                1
        );
        PathfinderMob wolfAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.ZOMBIE,
                2,
                2,
                3
        );
        PathfinderMob factionVictim = helper.spawnWithNoFreeWill(
                EntityTypes.PIGLIN,
                1,
                2,
                7
        );
        PathfinderMob factionAlly = helper.spawnWithNoFreeWill(
                EntityTypes.BLAZE,
                3,
                2,
                7
        );
        PathfinderMob factionAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.SKELETON,
                2,
                2,
                9
        );
        PathfinderMob neutralVictim = helper.spawnWithNoFreeWill(
                EntityTypes.COW,
                7,
                2,
                7
        );
        Mob excludedFactionAlly = helper.spawnWithNoFreeWill(
                EntityTypes.GHAST,
                9,
                4,
                7
        );
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player otherOwner = helper.makeMockPlayer(GameType.SURVIVAL);
        Wolf tamedVictim = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 1, 2, 11);
        Wolf sameOwnerAlly = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 3, 2, 11);
        Wolf sittingAlly = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 5, 2, 11);
        Wolf otherOwnerAlly = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 7, 2, 11);
        PathfinderMob declaredAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.PILLAGER,
                1,
                2,
                15
        );
        PathfinderMob declaredVictim = helper.spawnWithNoFreeWill(
                EntityTypes.PIGLIN,
                3,
                2,
                15
        );
        PathfinderMob declaredDefender = helper.spawnWithNoFreeWill(
                EntityTypes.BLAZE,
                5,
                2,
                15
        );
        PathfinderMob offensiveAlly = helper.spawnWithNoFreeWill(
                EntityTypes.VINDICATOR,
                1,
                2,
                17
        );
        babyWolf.setBaby(true);
        tame(tamedVictim, owner, false);
        tame(sameOwnerAlly, owner, false);
        tame(sittingAlly, owner, true);
        tame(otherOwnerAlly, otherOwner, false);
        List<Mob> mobs = List.of(
                wolfVictim,
                wolfAlly,
                busyWolf,
                babyWolf,
                polarBearVictim,
                polarBearAlly,
                wolfAttacker,
                factionVictim,
                factionAlly,
                factionAttacker,
                neutralVictim,
                excludedFactionAlly,
                tamedVictim,
                sameOwnerAlly,
                sittingAlly,
                otherOwnerAlly,
                declaredAttacker,
                declaredVictim,
                declaredDefender,
                offensiveAlly
        );

        try {
            helper.assertTrue(
                    RetoldMobRules.isSharedDefenseSpecies(wolfVictim),
                    "Wolves must be opted into exact-species shared defense"
            );
            helper.assertTrue(
                    RetoldMobRules.isSharedDefenseSpecies(polarBearVictim),
                    "Polar Bears must be opted into exact-species shared defense"
            );
            helper.assertFalse(
                    RetoldMobRules.isSharedDefenseSpecies(neutralVictim),
                    "Ordinary passive herds must flee together rather than gain attack assistance"
            );
            helper.assertTrue(
                    RetoldFactionRelations.supportsSharedDefense(
                            RetoldFaction.NETHER_REMNANTS
                    ),
                    "Nether Remnants must retain cross-species faction defense"
            );
            helper.assertFalse(
                    RetoldFactionRelations.supportsSharedDefense(RetoldFaction.UNDEAD),
                    "The broad Undead identity must defer to its bounded family specialists"
            );

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            busyWolf,
                            factionAttacker,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The occupied-Wolf fixture must retain a different live threat"
            );
            assertDamageApplied(helper, level, wolfVictim, wolfAttacker);
            assertAssistTarget(
                    helper,
                    wolfAlly,
                    wolfAttacker,
                    "An idle nearby Wolf must protect an attacked Wolf"
            );
            helper.assertFalse(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            wolfVictim,
                            wolfAttacker,
                            RetoldTargetSource.FACTION_ASSIST
                    ),
                    "A victim must not recruit itself as its own assisting ally"
            );
            helper.assertTrue(
                    busyWolf.getTarget() == factionAttacker,
                    "Shared defense must not overwrite an ally already fighting another live threat"
            );
            helper.assertTrue(
                    babyWolf.getTarget() == null,
                    "Baby Wolves must not be recruited into shared combat"
            );
            helper.assertTrue(
                    RetoldAiControl.isControlledAs(
                            wolfAlly,
                            RetoldAiControlMode.ATTACK
                    ),
                    "Wolf shared defense must enter owned controlled combat"
            );

            assertDamageApplied(helper, level, polarBearVictim, wolfAttacker);
            assertAssistTarget(
                    helper,
                    polarBearAlly,
                    wolfAttacker,
                    "An idle nearby Polar Bear must protect an attacked Polar Bear"
            );

            assertDamageApplied(helper, level, tamedVictim, factionAttacker);
            assertAssistTarget(
                    helper,
                    sameOwnerAlly,
                    factionAttacker,
                    "A standing tamed Wolf must protect another Wolf with the same owner"
            );
            helper.assertTrue(
                    sittingAlly.getTarget() == null,
                    "A sitting tamed Wolf must not answer a shared-defense call"
            );
            helper.assertTrue(
                    otherOwnerAlly.getTarget() == null,
                    "A tamed Wolf must not join another owner's pack defense"
            );

            assertDamageApplied(helper, level, factionVictim, factionAttacker);
            assertAssistTarget(
                    helper,
                    factionAlly,
                    factionAttacker,
                    "A Blaze must protect an attacked Piglin through Nether Remnant alignment"
            );

            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    factionAlly,
                    factionAttacker,
                    true
            );
            assertDamageApplied(helper, level, neutralVictim, factionVictim);
            helper.assertTrue(
                    factionAlly.getTarget() == null,
                    "An attacker's faction must not rally merely because its member attacked"
            );
            helper.assertTrue(
                    excludedFactionAlly.getTarget() == null,
                    "Specialist Undead members must not join unrelated generic defense calls"
            );

            LivingEntity previousDeclaredDefenderTarget = declaredDefender.getTarget();

            if (previousDeclaredDefenderTarget != null) {
                RetoldCombatTargets.clearTargetReferencesAndAggression(
                        declaredDefender,
                        previousDeclaredDefenderTarget,
                        true
                );
                RetoldAiControl.clear(declaredDefender);
            }

            helper.assertTrue(
                    RetoldCombatTargets.applyAttackTarget(
                            declaredAttacker,
                            declaredVictim,
                            RetoldTargetSource.BEHAVIOR_COMBAT
                    ),
                    "The declared-attack fixture must begin with a real owned target"
            );
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(declaredAttacker)
            );
            assertAssistTarget(
                    helper,
                    declaredDefender,
                    declaredAttacker,
                    "A faction ally must protect a member that an enemy is actively targeting"
            );
            helper.assertTrue(
                    offensiveAlly.getTarget() == null,
                    "Selecting an attack target must not rally the attacker's faction"
            );
            helper.succeed();
        } finally {
            for (Mob mob : mobs) {
                RetoldAiControl.clear(mob);
                mob.discard();
            }
            owner.discard();
            otherOwner.discard();
        }
    }

    private static void visibleAttackIntentTriggersPreHitResponse(
            GameTestHelper helper
    ) {
        ServerLevel level = helper.getLevel();
        PathfinderMob preyAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.ZOMBIE,
                1,
                2,
                1
        );
        PathfinderMob preyVictim = helper.spawnWithNoFreeWill(
                EntityTypes.COW,
                6,
                2,
                1
        );
        PathfinderMob wolfAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.PILLAGER,
                1,
                2,
                6
        );
        Wolf wolfVictim = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 6, 2, 6);
        Wolf wolfAlly = helper.spawnWithNoFreeWill(EntityTypes.WOLF, 8, 2, 6);
        PathfinderMob factionAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.PILLAGER,
                1,
                2,
                11
        );
        PathfinderMob factionVictim = helper.spawnWithNoFreeWill(
                EntityTypes.PIGLIN,
                6,
                2,
                11
        );
        PathfinderMob offensiveAlly = helper.spawnWithNoFreeWill(
                EntityTypes.VINDICATOR,
                3,
                2,
                11
        );
        PathfinderMob hiddenAttacker = helper.spawnWithNoFreeWill(
                EntityTypes.ZOMBIE,
                1,
                2,
                16
        );
        PathfinderMob hiddenVictim = helper.spawnWithNoFreeWill(
                EntityTypes.COW,
                6,
                2,
                16
        );
        for (int y = 2; y <= 4; y++) {
            for (int z = 15; z <= 17; z++) {
                helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            }
        }
        List<Mob> mobs = List.of(
                preyAttacker,
                preyVictim,
                wolfAttacker,
                wolfVictim,
                wolfAlly,
                factionAttacker,
                factionVictim,
                offensiveAlly,
                hiddenAttacker,
                hiddenVictim
        );

        try {
            float preyHealth = preyVictim.getHealth();
            assignAttackIntent(helper, preyAttacker, preyVictim);
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(preyAttacker)
            );
            helper.assertTrue(
                    preyVictim.getHealth() == preyHealth
                            && RetoldAiControl.isControlledAs(
                            preyVictim,
                            RetoldAiControlMode.FLEE
                    ),
                    "Visible active attack intent must make passive prey flee before damage"
            );

            float wolfHealth = wolfVictim.getHealth();
            assignAttackIntent(helper, wolfAttacker, wolfVictim);
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(wolfAttacker)
            );
            helper.assertTrue(
                    wolfVictim.getHealth() == wolfHealth,
                    "Preemptive defense must not require or synthesize damage"
            );
            assertThreatResponseTarget(
                    helper,
                    wolfVictim,
                    wolfAttacker,
                    "A healthy combat-capable victim must defend against visible attack intent"
            );
            assertAssistTarget(
                    helper,
                    wolfAlly,
                    wolfAttacker,
                    "A social ally that witnesses the intent must protect the intended victim"
            );
            helper.assertTrue(
                    wolfVictim.hurtServer(
                            level,
                            level.damageSources().mobAttack(wolfAttacker),
                            1.0F
                    ),
                    "The visible-threat fixture must apply a later real hit"
            );
            helper.assertTrue(
                    RetoldFactionTargetMemory.isOwnedByAny(
                            wolfVictim,
                            wolfAttacker,
                            RetoldTargetSource.RETALIATION
                    ),
                    "A later real hit must upgrade visible-threat ownership to retaliation"
            );

            float factionHealth = factionVictim.getHealth();
            assignAttackIntent(helper, factionAttacker, factionVictim);
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(factionAttacker)
            );
            helper.assertTrue(
                    factionVictim.getHealth() == factionHealth,
                    "Faction self-defense must begin before any health damage"
            );
            assertThreatResponseTarget(
                    helper,
                    factionVictim,
                    factionAttacker,
                    "A combat faction member must counter visible attack intent"
            );
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(factionVictim)
            );
            helper.assertTrue(
                    offensiveAlly.getTarget() == null,
                    "A preemptive counter-target must not rally the original attacker's faction"
            );

            assignAttackIntent(helper, hiddenAttacker, hiddenVictim);
            RetoldFactionAssistEvents.onEntityTickPost(
                    new EntityTickEvent.Post(hiddenAttacker)
            );
            helper.assertTrue(
                    !RetoldAiControl.isControlledAs(
                            hiddenVictim,
                            RetoldAiControlMode.FLEE
                    ),
                    "Attack intent behind a solid wall must not cause perfect knowledge"
            );
            helper.succeed();
        } finally {
            for (Mob mob : mobs) {
                RetoldAiControl.clear(mob);
                mob.discard();
            }
        }
    }

    private static void assignAttackIntent(
            GameTestHelper helper,
            PathfinderMob attacker,
            LivingEntity victim
    ) {
        helper.assertTrue(
                RetoldCombatTargets.applyAttackTarget(
                        attacker,
                        victim,
                        RetoldTargetSource.BEHAVIOR_COMBAT
                ),
                "The visible-intent fixture must assign the intended victim"
        );
    }

    private static void assertThreatResponseTarget(
            GameTestHelper helper,
            PathfinderMob defender,
            LivingEntity attacker,
            String message
    ) {
        helper.assertTrue(defender.getTarget() == attacker, message);
        helper.assertTrue(
                RetoldFactionTargetMemory.isOwnedByAny(
                        defender,
                        attacker,
                        RetoldTargetSource.THREAT_RESPONSE
                ),
                message + " with explicit visible-threat ownership"
        );
    }

    private static void tame(Wolf wolf, Player owner, boolean sitting) {
        wolf.setTame(true, true);
        wolf.setOwner(owner);
        wolf.setOrderedToSit(sitting);
    }

    private static void assertDamageApplied(
            GameTestHelper helper,
            ServerLevel level,
            LivingEntity victim,
            LivingEntity attacker
    ) {
        helper.assertTrue(
                victim.hurtServer(
                        level,
                        level.damageSources().mobAttack(attacker),
                        1.0F
                ),
                "The shared-defense fixture must apply real health damage"
        );
    }

    private static void assertAssistTarget(
            GameTestHelper helper,
            PathfinderMob ally,
            LivingEntity attacker,
            String message
    ) {
        helper.assertTrue(ally.getTarget() == attacker, message);
        helper.assertTrue(
                RetoldFactionTargetMemory.isOwnedByAny(
                        ally,
                        attacker,
                        RetoldTargetSource.FACTION_ASSIST
                ),
                message + " with explicit faction-assist ownership"
        );
    }

    private static final class InlineGameTest extends FunctionGameTestInstance {
        private final Consumer<GameTestHelper> test;

        private InlineGameTest(
                TestData<Holder<TestEnvironmentDefinition<?>>> testData,
                Consumer<GameTestHelper> test
        ) {
            super(BuiltinTestFunctions.ALWAYS_PASS, testData);
            this.test = test;
        }

        @Override
        public void run(GameTestHelper helper) {
            test.accept(helper);
        }
    }
}
