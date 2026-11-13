package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.BuiltinTestFunctions;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

final class RetoldEarthRoomGameTests {
    private RetoldEarthRoomGameTests() {
    }

    static void register(RegisterGameTestsEvent event, TestData<Holder<TestEnvironmentDefinition<?>>> data) {
        event.registerTest(Identifier.fromNamespaceAndPath("retold", "earth_rooms_keep_loot_and_fire_across_chunk_border"),
                new FunctionGameTestInstance(BuiltinTestFunctions.ALWAYS_PASS, data) {
                    @Override
                    public void run(GameTestHelper helper) {
                        roomContents(helper);
                    }
                });
    }

    private static void roomContents(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(8, 4, 8));
        BlockPos floor = new BlockPos((anchor.getX() & ~15) + 14, anchor.getY(), anchor.getZ());
        ChunkPos ticket = new ChunkPos(floor.getX() >> 4, floor.getZ() >> 4);
        level.getChunkSource().addTicketWithRadius(TicketType.FORCED, ticket, 2);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                level.setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
            }
        }
        BoundingBox left = new BoundingBox(floor.getX() - 14, level.getMinY(), floor.getZ() - 4,
                floor.getX() + 1, level.getMaxY(), floor.getZ() + 4);
        BoundingBox right = new BoundingBox(floor.getX() + 2, level.getMinY(), floor.getZ() - 4,
                floor.getX() + 17, level.getMaxY(), floor.getZ() + 4);
        var trap = new EarthLabyrinthRooms.Room(new EarthLabyrinthLayout.Cell(0, 0, 0),
                1, 0, EarthLabyrinthRooms.Kind.ARROW_TRAP, 17L);
        BlockPos dispenserPos = floor.east(2).above();
        EarthLabyrinthRoomPlacement.place(level, left, trap, floor);
        helper.assertTrue(level.getBlockEntity(dispenserPos) == null, "Placement must not cross its chunk boundary");
        EarthLabyrinthRoomPlacement.place(level, right, trap, floor);
        helper.assertTrue(level.getBlockEntity(dispenserPos) instanceof DispenserBlockEntity,
                "The adjacent chunk must complete the dispenser");
        DispenserBlockEntity dispenser = (DispenserBlockEntity) level.getBlockEntity(dispenserPos);
        helper.assertValueEqual(dispenser.getLootTable(), EarthLabyrinthRoomPlacement.ARROWS, "Trap loot table");
        dispenser.unpackLootTable(null);
        int arrows = arrowCount(dispenser);
        helper.assertTrue(arrows >= 8 && arrows <= 16, "The trap must have finite live ammunition");

        var treasure = new EarthLabyrinthRooms.Room(new EarthLabyrinthLayout.Cell(1, 0, 0),
                0, 1, EarthLabyrinthRooms.Kind.TREASURE, 31L);
        BlockPos treasureFloor = floor.west(2);
        EarthLabyrinthRoomPlacement.place(level, left, treasure, treasureFloor);
        ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(treasureFloor.south().above());
        helper.assertValueEqual(chest.getLootTable(), EarthLabyrinthRoomPlacement.TREASURE, "Treasure loot table");
        chest.unpackLootTable(null);
        helper.assertTrue(!chest.isEmpty(), "Treasure must generate real loot");
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            helper.assertFalse(chest.getItem(slot).is(Items.LODESTONE), "Side treasure must not award the boss artifact");
        }
        chest.clearContent();
        chest.setItem(0, new ItemStack(Items.STICK, 3));
        EarthLabyrinthRoomPlacement.place(level, left, treasure, treasureFloor);
        helper.assertTrue(chest.getLootTable() == null && chest.getItem(0).getCount() == 3,
                "Repeated chunk placement must not refill or overwrite a container");

        var victim = EntityTypes.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(victim != null, "Trap victim must spawn");
        victim.setNoAi(true);
        victim.setPos(Vec3.atBottomCenterOf(floor.east().above()));
        level.addFreshEntity(victim);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(arrowCount(dispenser) < arrows,
                        "Standing on the pressure plate must fire real dispenser ammunition"))
                .thenExecute(() -> {
                    victim.discard();
                    level.getChunkSource().removeTicketWithRadius(TicketType.FORCED, ticket, 2);
                })
                .thenSucceed();
    }

    private static int arrowCount(DispenserBlockEntity dispenser) {
        int count = 0;
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
            ItemStack stack = dispenser.getItem(slot);
            if (stack.is(Items.ARROW)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
