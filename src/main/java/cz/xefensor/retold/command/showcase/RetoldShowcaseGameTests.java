package cz.xefensor.retold.command.showcase;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import cz.xefensor.retold.event.RetoldCommandEvents;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class RetoldShowcaseGameTests {
    private static final Identifier EMPTY_STRUCTURE = Identifier.withDefaultNamespace("empty");

    private RetoldShowcaseGameTests() {
    }

    public static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment
    ) {
        register(event, environment, "showcase_contains_live_registry_and_stable_exhibits", 120,
                RetoldShowcaseGameTests::containsLiveRegistryAndStableExhibits);
        register(event, environment, "showcase_rejects_occupied_and_protected_volume_without_edits", 120,
                RetoldShowcaseGameTests::rejectsOccupiedAndProtectedVolumeWithoutEdits);
    }

    private static void containsLiveRegistryAndStableExhibits(GameTestHelper helper) {
        RetoldShowcaseWorld.Plan plan = RetoldShowcaseWorld.plan(helper.absolutePos(new BlockPos(4, 64, 4)));
        TestArea area = new TestArea(helper, plan);
        helper.startSequence()
                .thenWaitUntil(() -> area.assertReady(helper))
                .thenExecute(() -> buildAndObserve(helper, plan, area));
    }

    private static void buildAndObserve(
            GameTestHelper helper,
            RetoldShowcaseWorld.Plan plan,
            TestArea area
    ) {
        ServerLevel level = helper.getLevel();
        RetoldShowcaseWorld.BuildResult result = RetoldShowcaseWorld.build(level, plan);
        helper.assertTrue(result.created(), "The empty loaded sky volume must accept a showcase: " + result.message());
        assertCatalog(helper, plan);

        List<Entity> exhibits = area.entities().stream()
                .filter(entity -> entity.entityTags().contains(RetoldShowcaseEntities.EXHIBIT_TAG))
                .toList();
        Map<Entity, Vec3> positions = new HashMap<>();
        Map<Entity, Integer> ticks = new HashMap<>();
        for (Entity entity : exhibits) {
            positions.put(entity, entity.position());
            ticks.put(entity, entity.tickCount);
            helper.assertTrue(entity.isNoGravity(), "Exhibits must not fall when their tick is suppressed");
            EntityTickEvent.Pre exhibitTick = new EntityTickEvent.Pre(entity);
            RetoldCommandEvents.onShowcaseEntityTickPre(exhibitTick);
            helper.assertTrue(exhibitTick.isCanceled(), "Every tagged exhibit must suppress custom entity ticks");
            if (isRetold(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())) || entity instanceof ArmorStand) {
                assertSavedFreezePolicy(helper, entity, plan.groupTag());
            }
            if (entity instanceof Mob mob) {
                MobDespawnEvent despawn = new MobDespawnEvent(mob, level);
                RetoldCommandEvents.onShowcaseMobDespawn(despawn);
                helper.assertTrue(despawn.getResult() == MobDespawnEvent.Result.DENY,
                        "Tagged bosses must be protected from pre-tick despawning, including Peaceful");
            }
        }

        Mob ordinaryMob = EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(ordinaryMob != null, "An ordinary control mob must be creatable");
        ordinaryMob.snapTo(Vec3.atBottomCenterOf(plan.origin().offset(2, 1, 2)), 0.0F, 0.0F);
        ordinaryMob.setNoAi(true);
        level.addFreshEntity(ordinaryMob);
        EntityTickEvent.Pre controlTick = new EntityTickEvent.Pre(ordinaryMob);
        RetoldCommandEvents.onShowcaseEntityTickPre(controlTick);
        helper.assertFalse(controlTick.isCanceled(), "The showcase freeze must leave ordinary mobs ticking");
        MobDespawnEvent controlDespawn = new MobDespawnEvent(ordinaryMob, level);
        RetoldCommandEvents.onShowcaseMobDespawn(controlDespawn);
        helper.assertTrue(controlDespawn.getResult() == MobDespawnEvent.Result.DEFAULT,
                "The showcase must leave ordinary mob despawning unchanged");

        helper.runAfterDelay(30, () -> {
            helper.assertTrue(ordinaryMob.tickCount > 0, "The fixture must advance real loaded entity ticks");
            for (Entity entity : exhibits) {
                helper.assertFalse(entity.isRemoved(), "Every exhibit must remain present after real server ticks");
                helper.assertValueEqual(entity.position(), positions.get(entity),
                        "Boss flight, gravity, and custom encounter ticks must not move an exhibit");
                // ServerLevel advances tickCount before the cancellable tick hook.
                helper.assertTrue(entity.tickCount > ticks.get(entity),
                        "Every exhibit chunk must advance real server ticks during the observation");
            }
            assertCatalog(helper, plan);
            ordinaryMob.discard();
            helper.succeed();
        });
    }

    private static void assertSavedFreezePolicy(GameTestHelper helper, Entity entity, String groupTag) {
        ServerLevel level = helper.getLevel();
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(output);
        Entity restored = entity.getType().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
        helper.assertTrue(restored != null, "A saved exhibit type must be restorable");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
        helper.assertTrue(restored.entityTags().contains(groupTag)
                        && restored.entityTags().contains(RetoldShowcaseEntities.EXHIBIT_TAG),
                "Exhibit identity and freezing must survive vanilla entity serialization");
        helper.assertTrue(restored.isNoGravity(), "Restored exhibits must retain their gravity policy");
        EntityTickEvent.Pre restoredTick = new EntityTickEvent.Pre(restored);
        RetoldCommandEvents.onShowcaseEntityTickPre(restoredTick);
        helper.assertTrue(restoredTick.isCanceled(), "Restored exhibit tags must activate the real freeze hook");
    }

    private static void assertCatalog(GameTestHelper helper, RetoldShowcaseWorld.Plan plan) {
        ServerLevel level = helper.getLevel();
        Set<Identifier> blockIds = new HashSet<>();
        Set<Identifier> itemIds = new HashSet<>();
        int namedAccessibleChests = 0;
        int labels = 0;
        for (BlockPos pos : positions(plan.bounds())) {
            BlockState state = level.getBlockState(pos);
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (isRetold(blockId)) {
                blockIds.add(blockId);
                helper.assertTrue(state.canSurvive(level, pos),
                        "The block exhibit needs valid support: " + blockId + " at " + pos);
            }
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                if (chest.getCustomName() != null && !chest.getCustomName().getString().isBlank()) {
                    helper.assertTrue(state.getMenuProvider(level, pos) != null,
                            "Supply chests must remain accessible from the gallery");
                    namedAccessibleChests++;
                }
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    ItemStack stack = chest.getItem(slot);
                    if (!stack.isEmpty() && isRetold(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                        itemIds.add(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                    }
                }
            }
            if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                for (int row = 0; row < 4; row++) {
                    if (!sign.getFrontText().getMessage(row, false).getString().isBlank()) {
                        labels++;
                        break;
                    }
                }
            }
        }

        Set<Identifier> expectedBlocks = new HashSet<>();
        BuiltInRegistries.BLOCK.keySet().stream().filter(RetoldShowcaseGameTests::isRetold).forEach(expectedBlocks::add);
        Set<Identifier> expectedItems = new HashSet<>();
        BuiltInRegistries.ITEM.keySet().stream().filter(RetoldShowcaseGameTests::isRetold).forEach(expectedItems::add);
        helper.assertValueEqual(blockIds, expectedBlocks, "Every currently registered Retold block must have a supported exhibit");
        helper.assertValueEqual(itemIds, expectedItems, "Supply chests must contain every currently registered Retold item");
        helper.assertTrue(namedAccessibleChests > 0, "The catalog needs named, usable supply chests");
        helper.assertTrue(labels >= expectedBlocks.size(), "Block exhibits must have readable sign labels");

        Set<Identifier> entityIds = new HashSet<>();
        int adultArmor = 0;
        int smallArmor = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, AABB.of(plan.bounds()),
                candidate -> candidate.entityTags().contains(plan.groupTag()))) {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (isRetold(entityId)) {
                entityIds.add(entityId);
                helper.assertTrue(entity.hasCustomName(), "Entity labels must identify the registry entry");
                helper.assertTrue(entity.entityTags().contains(RetoldShowcaseEntities.EXHIBIT_TAG),
                        "All Retold entities must use the isolated frozen-exhibit policy");
            }
            if (entity instanceof ArmorStand stand) {
                helper.assertTrue(stand.getItemBySlot(EquipmentSlot.HEAD).is(RetoldBlocks.STEEL_HELMET.get())
                                && stand.getItemBySlot(EquipmentSlot.CHEST).is(RetoldBlocks.STEEL_CHESTPLATE.get())
                                && stand.getItemBySlot(EquipmentSlot.LEGS).is(RetoldBlocks.STEEL_LEGGINGS.get())
                                && stand.getItemBySlot(EquipmentSlot.FEET).is(RetoldBlocks.STEEL_BOOTS.get()),
                        "Both armor exhibits must wear the full Steel set");
                if (stand.isBaby()) {
                    smallArmor++;
                } else {
                    adultArmor++;
                }
            }
        }
        Set<Identifier> expectedEntities = new HashSet<>();
        BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(RetoldShowcaseGameTests::isRetold).forEach(expectedEntities::add);
        helper.assertValueEqual(entityIds, expectedEntities, "Every currently registered Retold entity must be exhibited");
        helper.assertTrue(adultArmor == 1 && smallArmor == 1, "Steel needs one adult and one baby armor exhibit");
    }

    private static void rejectsOccupiedAndProtectedVolumeWithoutEdits(GameTestHelper helper) {
        RetoldShowcaseWorld.Plan plan = RetoldShowcaseWorld.plan(helper.absolutePos(new BlockPos(4, 64, 4)));
        TestArea area = new TestArea(helper, plan);
        helper.startSequence()
                .thenWaitUntil(() -> area.assertReady(helper))
                .thenExecute(() -> assertPlacementDenials(helper, plan, area))
                .thenExecute(() -> assertUnreadyRejection(helper, plan.origin().offset(1024, 0, 1024)))
                .thenSucceed();
    }

    private static void assertPlacementDenials(
            GameTestHelper helper,
            RetoldShowcaseWorld.Plan plan,
            TestArea area
    ) {
        ServerLevel level = helper.getLevel();
        BlockPos occupied = new BlockPos(plan.bounds().maxX(), plan.bounds().maxY(), plan.bounds().maxZ());
        level.setBlock(occupied, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
        RetoldShowcaseWorld.BuildResult occupiedResult = RetoldShowcaseWorld.build(level, plan);
        helper.assertFalse(occupiedResult.created(), "One occupied cell must reject the whole planned volume");
        helper.assertTrue(occupiedResult.message().contains("space is occupied"),
                "The loaded fixture must reach the occupied-block check");
        assertUnchangedExcept(helper, plan, occupied);
        helper.assertTrue(area.entities().isEmpty(), "Occupied-volume rejection must not leave any entity exhibits");
        level.setBlock(occupied, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);

        RetoldWorldProtection.Registration protection = RetoldWorldProtection.register(
                Identifier.fromNamespaceAndPath(Retold.MODID, "showcase_test_protection"),
                context -> context.level() != level || !context.pos().equals(occupied)
        );
        try {
            RetoldShowcaseWorld.BuildResult protectedResult = RetoldShowcaseWorld.build(level, plan);
            helper.assertFalse(protectedResult.created(), "One protected cell must reject the whole planned volume");
            helper.assertTrue(protectedResult.message().contains("protection denied"),
                    "The loaded fixture must reach the world-protection check");
            assertUnchangedExcept(helper, plan, null);
            helper.assertTrue(area.entities().isEmpty(), "Protection rejection must not leave any entity exhibits");
        } finally {
            protection.close();
        }

        Mob cow = EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(cow != null, "An occupied-volume control mob must be creatable");
        cow.snapTo(Vec3.atBottomCenterOf(plan.origin().offset(2, 1, 2)), 0.0F, 0.0F);
        cow.setNoAi(true);
        cow.setNoGravity(true);
        helper.assertTrue(level.addFreshEntity(cow), "The ordinary Cow must join the loaded fixture");
        RetoldShowcaseWorld.BuildResult entityResult = RetoldShowcaseWorld.build(level, plan);
        helper.assertFalse(entityResult.created(), "An existing ordinary entity must reject the planned volume");
        helper.assertTrue(entityResult.message().contains("contains entities"),
                "The loaded fixture must reach the occupied-entity check");
        assertUnchangedExcept(helper, plan, null);
        helper.assertFalse(cow.isRemoved(), "Entity-occupied rejection must preserve the original Cow");
        List<Entity> remaining = area.entities();
        helper.assertTrue(remaining.size() == 1 && remaining.contains(cow),
                "Entity-occupied rejection must leave only the original Cow and no partial exhibits");
        cow.discard();
    }

    private static void assertUnreadyRejection(GameTestHelper helper, BlockPos origin) {
        RetoldShowcaseWorld.Plan plan = RetoldShowcaseWorld.plan(origin);
        TestArea area = new TestArea(helper, plan, false);
        boolean unready = false;
        for (int x = plan.bounds().minX() >> 4; x <= plan.bounds().maxX() >> 4; x++) {
            for (int z = plan.bounds().minZ() >> 4; z <= plan.bounds().maxZ() >> 4; z++) {
                unready |= !helper.getLevel().areEntitiesActuallyLoadedAndTicking(new ChunkPos(x, z));
            }
        }
        helper.assertTrue(unready, "The remote fixture must have real unticketed entity sections");
        RetoldShowcaseWorld.BuildResult result = RetoldShowcaseWorld.build(helper.getLevel(), plan);
        helper.assertFalse(result.created(), "A volume with unpublished or non-ticking entities must be rejected");
        helper.assertTrue(result.message().contains("retry /retold testworld"),
                "Unready sections must explain how to retry after the area loads");
        assertUnchangedExcept(helper, plan, null);
        helper.assertTrue(area.entities().isEmpty(), "Unready-volume rejection must not create partial exhibits");
    }

    private static void assertUnchangedExcept(
            GameTestHelper helper,
            RetoldShowcaseWorld.Plan plan,
            BlockPos occupied
    ) {
        for (BlockPos pos : positions(plan.bounds())) {
            boolean isMarker = pos.equals(occupied);
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(isMarker ? Blocks.GOLD_BLOCK : Blocks.AIR),
                    "Rejected construction must leave every planned cell unchanged at " + pos);
        }
    }

    private static Iterable<BlockPos> positions(BoundingBox bounds) {
        return BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    private static boolean isRetold(Identifier identifier) {
        return Retold.MODID.equals(identifier.getNamespace());
    }

    private static void register(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            int ticks,
            Consumer<GameTestHelper> test
    ) {
        event.registerTest(Identifier.fromNamespaceAndPath(Retold.MODID, name),
                new InlineGameTest(new TestData<>(environment, EMPTY_STRUCTURE, ticks, 0, true), test));
    }

    /** The catalog is larger than an empty fixture, so release its chunks and edits on either outcome. */
    private static final class TestArea implements GameTestListener {
        private final ServerLevel level;
        private final RetoldShowcaseWorld.Plan plan;
        private final List<ChunkPos> addedForcedChunks = new ArrayList<>();
        private boolean ownsVolume;
        private boolean cleaned;

        private TestArea(GameTestHelper helper, RetoldShowcaseWorld.Plan plan) {
            this(helper, plan, true);
        }

        private TestArea(GameTestHelper helper, RetoldShowcaseWorld.Plan plan, boolean forceChunks) {
            this.level = helper.getLevel();
            this.plan = plan;
            helper.testInfo.addListener(this);
            if (forceChunks) {
                for (int x = plan.bounds().minX() >> 4; x <= plan.bounds().maxX() >> 4; x++) {
                    for (int z = plan.bounds().minZ() >> 4; z <= plan.bounds().maxZ() >> 4; z++) {
                        if (!level.getForceLoadedChunks().contains(ChunkPos.pack(x, z))) {
                            level.setChunkForced(x, z, true);
                            addedForcedChunks.add(new ChunkPos(x, z));
                        }
                    }
                }
            }
            for (BlockPos pos : positions(plan.bounds())) {
                helper.assertTrue(level.getBlockState(pos).isAir(), "The high test volume must start empty at " + pos);
            }
            ownsVolume = true;
        }

        private List<Entity> entities() {
            return level.getEntitiesOfClass(Entity.class, AABB.of(plan.bounds()), entity -> true);
        }

        private void assertReady(GameTestHelper helper) {
            for (int x = plan.bounds().minX() >> 4; x <= plan.bounds().maxX() >> 4; x++) {
                for (int z = plan.bounds().minZ() >> 4; z <= plan.bounds().maxZ() >> 4; z++) {
                    ChunkPos chunk = new ChunkPos(x, z);
                    // setChunkForced loads terrain synchronously; entity-section visibility follows later.
                    helper.assertTrue(level.areEntitiesActuallyLoadedAndTicking(chunk),
                            "The fixture must publish entity visibility and ticking before building chunk " + chunk);
                }
            }
        }

        private void cleanup() {
            if (cleaned) {
                return;
            }
            cleaned = true;
            if (ownsVolume) {
                entities().forEach(Entity::discard);
                for (BlockPos pos : positions(plan.bounds())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                // Container removal can create fresh item drops after the original entities were removed.
                entities().forEach(Entity::discard);
            }
            for (ChunkPos chunk : addedForcedChunks) {
                level.setChunkForced(chunk.x(), chunk.z(), false);
            }
        }

        @Override
        public void testStructureLoaded(GameTestInfo testInfo) {
        }

        @Override
        public void testPassed(GameTestInfo testInfo, GameTestRunner runner) {
            cleanup();
        }

        @Override
        public void testFailed(GameTestInfo testInfo, GameTestRunner runner) {
            cleanup();
        }

        @Override
        public void testAddedForRerun(GameTestInfo original, GameTestInfo copy, GameTestRunner runner) {
        }
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
