package cz.xefensor.retold.event;

import cz.xefensor.retold.Retold;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.function.Consumer;

public final class RetoldVexGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final float DAMAGE_EPSILON = 0.001F;

    private RetoldVexGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(
                        environment,
                        EMPTY_STRUCTURE,
                        40,
                        0,
                        true
                );

        event.registerTest(
                id("vex_direct_hits_deal_half_damage"),
                new InlineGameTest(
                        testData,
                        RetoldVexGameTests::vexDirectHitsDealHalfDamage
                )
        );
    }

    private static void vexDirectHitsDealHalfDamage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var vex = helper.spawn(EntityTypes.VEX, 1, 2, 1);
        var cow = helper.spawn(EntityTypes.COW, 2, 2, 1);
        ItemStack weapon = vex.getWeaponItem();
        DamageSource source = weapon.getDamageSource(vex);
        float rawDamage = (float) vex.getAttributeValue(Attributes.ATTACK_DAMAGE);

        rawDamage += weapon.getItem().getAttackDamageBonus(
                cow,
                rawDamage,
                source
        );

        float healthBefore = cow.getHealth();

        try {
            helper.assertTrue(
                    vex.doHurtTarget(level, cow),
                    "A direct Vex strike must connect with its target"
            );

            float actualDamage = healthBefore - cow.getHealth();
            float expectedDamage = rawDamage * 0.5F;

            helper.assertTrue(
                    Math.abs(actualDamage - expectedDamage) <= DAMAGE_EPSILON,
                    "A direct Vex strike must deal exactly half of its unmodified damage"
            );
            helper.succeed();
        } finally {
            vex.discard();
            cow.discard();
        }
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
