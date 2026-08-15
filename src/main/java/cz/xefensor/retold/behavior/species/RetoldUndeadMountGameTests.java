package cz.xefensor.retold.behavior.species;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.behavior.control.RetoldAiControl;
import cz.xefensor.retold.behavior.control.RetoldAiControlMode;
import cz.xefensor.retold.behavior.control.RetoldAiControlOwner;
import cz.xefensor.retold.behavior.profiles.RetoldMobRules;
import cz.xefensor.retold.combat.RetoldCombatTargets;
import cz.xefensor.retold.combat.RetoldFactionTargetMemory;
import cz.xefensor.retold.combat.RetoldTargetSource;
import cz.xefensor.retold.faction.RetoldFaction;
import cz.xefensor.retold.faction.RetoldFactionMembers;
import cz.xefensor.retold.faction.RetoldFactionRelations;

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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.equine.ZombieHorse;
import net.minecraft.world.entity.animal.camel.CamelHusk;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;

import java.util.function.Consumer;

public final class RetoldUndeadMountGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");

    private RetoldUndeadMountGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                id("isolated_undead_mount_behavior"),
                new TestEnvironmentDefinition.AllOf()
        );

        registerTest(
                event,
                environment,
                "wild_undead_mounts_are_hostile_until_claimed",
                RetoldUndeadMountGameTests::wildMountsAreHostileUntilClaimed
        );
        registerTest(
                event,
                environment,
                "claimed_undead_mounts_defend_themselves_and_owners",
                RetoldUndeadMountGameTests::claimedMountsDefendThemselvesAndOwners
        );
    }

    private static void wildMountsAreHostileUntilClaimed(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        SkeletonHorse skeletonHorse = helper.spawn(EntityTypes.SKELETON_HORSE, 3, 3, 5);
        ZombieHorse zombieHorse = helper.spawn(EntityTypes.ZOMBIE_HORSE, 5, 3, 7);
        CamelHusk camelHusk = helper.spawn(EntityTypes.CAMEL_HUSK, 6, 3, 5);
        Zombie undeadPeer = helper.spawn(EntityTypes.ZOMBIE, 4, 3, 5);
        Cow target = helper.spawn(EntityTypes.COW, 7, 3, 5);
        long gameTime = level.getGameTime();

        try {
            skeletonHorse.setTamed(true);
            zombieHorse.setTamed(false);

            assertWildUndeadMount(helper, skeletonHorse, "Skeleton Horse");
            assertWildUndeadMount(helper, zombieHorse, "Zombie Horse");
            assertWildUndeadMount(helper, camelHusk, "Camel Husk");
            helper.assertTrue(
                    !RetoldFactionRelations.shouldAttack(camelHusk, undeadPeer)
                            && !RetoldFactionRelations.shouldAttack(undeadPeer, camelHusk),
                    "Wild undead mounts and other Undead must tolerate each other"
            );

            float healthBeforeAttack = target.getHealth();
            RetoldUndeadMountEvents.tick(level, camelHusk, gameTime);
            assertCombat(
                    helper,
                    camelHusk,
                    target,
                    RetoldTargetSource.FACTION_COMBAT,
                    "A riderless wild Camel Husk must acquire a nearby living enemy"
            );
            RetoldUndeadMountEvents.tick(level, camelHusk, gameTime + 1L);
            helper.assertTrue(
                    target.getHealth() < healthBeforeAttack,
                    "A wild Camel Husk must deal real melee damage with the vanilla default attribute"
            );

            var owner = helper.makeMockPlayer(GameType.SURVIVAL);
            RetoldUndeadMountEvents.onEntityMount(new EntityMountEvent(
                    owner,
                    camelHusk,
                    level,
                    true
            ));
            helper.assertTrue(
                    camelHusk.getOwner() == owner
                            && RetoldFactionMembers.getFaction(camelHusk) == null
                            && camelHusk.getTarget() == null,
                    "A player mounting a riderless Camel Husk must claim it and end wild hostility"
            );

            RetoldUndeadMountEvents.onEntityMount(new EntityMountEvent(
                    owner,
                    skeletonHorse,
                    level,
                    true
            ));
            helper.assertTrue(
                    skeletonHorse.getOwner() == owner
                            && RetoldFactionMembers.getFaction(skeletonHorse) == null,
                    "A player mounting an ownerless trap-tamed Skeleton Horse must claim it"
            );

            RetoldUndeadMountEvents.onEntityMount(new EntityMountEvent(
                    owner,
                    zombieHorse,
                    level,
                    true
            ));
            helper.assertTrue(
                    zombieHorse.getOwnerReference() == null
                            && RetoldFactionMembers.getFaction(zombieHorse) == RetoldFaction.UNDEAD,
                    "Mounting must not bypass the Zombie Horse's vanilla bucking/taming process"
            );

            helper.assertTrue(
                    zombieHorse.tameWithName(owner)
                            && zombieHorse.getOwner() == owner
                            && RetoldFactionMembers.getFaction(zombieHorse) == null,
                    "Vanilla Zombie Horse taming must establish the same owner boundary"
            );
            owner.discard();
            helper.succeed();
        } finally {
            cleanup(skeletonHorse, zombieHorse, camelHusk, undeadPeer, target);
        }
    }

    private static void claimedMountsDefendThemselvesAndOwners(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ZombieHorse mount = helper.spawn(EntityTypes.ZOMBIE_HORSE, 5, 3, 5);
        Zombie ownerThreat = helper.spawn(EntityTypes.ZOMBIE, 6, 3, 5);
        Zombie selfThreat = helper.spawn(EntityTypes.ZOMBIE, 6, 3, 6);
        Cow innocent = helper.spawn(EntityTypes.COW, 7, 3, 7);
        var owner = helper.makeMockPlayer(GameType.SURVIVAL);
        long gameTime = level.getGameTime();

        try {
            mount.setOwner(owner);
            mount.setTamed(true);
            owner.snapTo(mount.getX(), mount.getY(), mount.getZ(), 0.0F, 0.0F);
            owner.setLastHurtByMob(ownerThreat);

            float ownerThreatHealth = ownerThreat.getHealth();
            RetoldUndeadMountEvents.tick(level, mount, gameTime);
            assertCombat(
                    helper,
                    mount,
                    ownerThreat,
                    RetoldTargetSource.OWNER_DEFENSE,
                    "A claimed undead mount must defend its owner"
            );
            RetoldUndeadMountEvents.tick(level, mount, gameTime + 1L);
            helper.assertTrue(
                    ownerThreat.getHealth() < ownerThreatHealth,
                    "Owner defense must use the mount's real melee damage path"
            );

            ownerThreat.discard();
            RetoldUndeadMountEvents.tick(level, mount, gameTime + 2L);
            helper.assertTrue(
                    mount.getTarget() == null
                            && !RetoldAiControl.isControlledBy(
                            mount,
                            RetoldAiControlOwner.UNDEAD_MOUNT
                    ),
                    "Owner-defense target and control must clear with the threat"
            );

            float mountHealth = mount.getHealth();
            helper.assertTrue(
                    mount.hurtServer(
                            level,
                            level.damageSources().mobAttack(selfThreat),
                            1.0F
                    ),
                    "The claimed undead mount must receive real damage"
            );
            helper.assertTrue(
                    mount.getHealth() < mountHealth,
                    "Self-defense must begin only after successful health damage"
            );
            assertCombat(
                    helper,
                    mount,
                    selfThreat,
                    RetoldTargetSource.RETALIATION,
                    "A claimed undead mount must defend itself without rejoining the Undead faction"
            );

            RetoldCombatTargets.clearTargetReferencesAndAggression(
                    mount,
                    selfThreat,
                    true
            );
            RetoldAiControl.clear(mount);
            selfThreat.discard();
            RetoldUndeadMountEvents.tick(level, mount, gameTime + 3L);
            helper.assertTrue(
                    mount.getTarget() != innocent,
                    "A claimed undead mount must not independently hunt unrelated livestock"
            );
            helper.succeed();
        } finally {
            owner.discard();
            cleanup(mount, ownerThreat, selfThreat, innocent);
        }
    }

    private static void assertWildUndeadMount(
            GameTestHelper helper,
            AbstractHorse mount,
            String name
    ) {
        helper.assertTrue(
                RetoldMobRules.isUndeadMount(mount)
                        && mount.getOwnerReference() == null
                        && RetoldFactionMembers.getFaction(mount) == RetoldFaction.UNDEAD
                        && mount.getAttribute(Attributes.ATTACK_DAMAGE) != null,
                name + " must load the undead-mount profile, hostility, and attack attribute"
        );
    }

    private static void assertCombat(
            GameTestHelper helper,
            AbstractHorse mount,
            LivingEntity target,
            RetoldTargetSource source,
            String message
    ) {
        helper.assertTrue(
                mount.getTarget() == target
                        && RetoldFactionTargetMemory.isOwnedByAny(mount, target, source)
                        && RetoldAiControl.isControlledAsBy(
                        mount,
                        RetoldAiControlMode.ATTACK,
                        RetoldAiControlOwner.UNDEAD_MOUNT
                ),
                message
        );
    }

    private static void cleanup(PathfinderMob... mobs) {
        for (PathfinderMob mob : mobs) {
            RetoldAiControl.clear(mob);

            if (!mob.isRemoved()) {
                mob.discard();
            }
        }
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String path,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(
                id(path),
                new InlineGameTest(
                        new TestData<>(environment, EMPTY_STRUCTURE, 120, 0, true),
                        test
                )
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Retold.MODID, path);
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
