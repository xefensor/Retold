package cz.xefensor.retold.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import java.util.List;
import java.util.function.Consumer;

public final class RetoldRuinedPortalGameTests {
    private static final Identifier EMPTY_STRUCTURE =
            Identifier.withDefaultNamespace("empty");
    private static final Identifier PORTAL_TEMPLATE =
            Identifier.withDefaultNamespace("ruined_portal/portal_1");

    private RetoldRuinedPortalGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData =
                new TestData<>(environment, EMPTY_STRUCTURE, 40, 0, true);

        event.registerTest(
                id("ruined_portal_templates_omit_chests"),
                new InlineGameTest(
                        testData,
                        RetoldRuinedPortalGameTests::templatesOmitChests
                )
        );
    }

    private static void templatesOmitChests(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        StructureTemplate template = level.getStructureManager()
                .getOrCreate(PORTAL_TEMPLATE);
        BlockPos pivot = new BlockPos(
                template.getSize().getX() / 2,
                0,
                template.getSize().getZ() / 2
        );
        RuinedPortalPiece piece = new RuinedPortalPiece(
                level.registryAccess(),
                level.getStructureManager(),
                helper.absolutePos(BlockPos.ZERO),
                RuinedPortalPiece.VerticalPlacement.ON_LAND_SURFACE,
                new RuinedPortalPiece.Properties(
                        false,
                        0.0F,
                        false,
                        false,
                        false,
                        false
                ),
                PORTAL_TEMPLATE,
                template,
                Rotation.NONE,
                Mirror.NONE,
                pivot
        );
        BlockPos processingOrigin = helper.absolutePos(new BlockPos(1, 2, 1));
        List<StructureTemplate.StructureBlockInfo> processed =
                StructureTemplate.processBlockInfos(
                        level,
                        processingOrigin,
                        processingOrigin,
                        piece.placeSettings(),
                        List.of(
                                new StructureTemplate.StructureBlockInfo(
                                        BlockPos.ZERO,
                                        Blocks.CHEST.defaultBlockState(),
                                        null
                                ),
                                new StructureTemplate.StructureBlockInfo(
                                        BlockPos.ZERO.east(),
                                        Blocks.BARREL.defaultBlockState(),
                                        null
                                )
                        ),
                        template
                );

        helper.assertValueEqual(
                processed.size(),
                1,
                "Ruined portal placement must omit only the template chest"
        );
        helper.assertTrue(
                processed.getFirst().state().is(Blocks.BARREL),
                "Ruined portal chest removal must not discard other containers"
        );
        helper.succeed();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("retold", path);
    }

    private static final class InlineGameTest
            extends FunctionGameTestInstance {
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
