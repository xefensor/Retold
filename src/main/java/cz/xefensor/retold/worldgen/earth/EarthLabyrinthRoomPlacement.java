package cz.xefensor.retold.worldgen.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.loot.LootTable;

/** Vanilla redstone/loot containers; no ticking dungeon owner, lights, or forced mob spawns. */
final class EarthLabyrinthRoomPlacement {
    static final ResourceKey<LootTable> TREASURE = lootTable("chests/earth_labyrinth");
    static final ResourceKey<LootTable> ARROWS = lootTable("chests/earth_labyrinth_dispenser");

    private EarthLabyrinthRoomPlacement() {
    }

    static void place(WorldGenLevel level, BoundingBox chunkBounds, EarthLabyrinthRooms.Room room, BlockPos floor) {
        Direction back = room.backX() > 0 ? Direction.EAST : room.backX() < 0 ? Direction.WEST
                : room.backZ() > 0 ? Direction.SOUTH : Direction.NORTH;
        BlockPos trigger = floor.relative(back).above();
        if (room.kind() == EarthLabyrinthRooms.Kind.TREASURE) {
            placeContainer(level, chunkBounds, trigger,
                    Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, back.getOpposite()),
                    TREASURE, room.lootSeed());
            return;
        }
        // A plate immediately in front of the dispenser powers it directly. The center remains walkable.
        BlockPos dispenser = floor.relative(back, 2).above();
        placeContainer(level, chunkBounds, dispenser,
                Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, back.getOpposite()),
                ARROWS, room.lootSeed());
        if (chunkBounds.isInside(trigger) && !level.isOutsideBuildHeight(trigger)) {
            level.setBlock(trigger.below(), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(trigger, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 2);
        }
    }

    private static void placeContainer(WorldGenLevel level, BoundingBox bounds, BlockPos pos, BlockState state,
                                       ResourceKey<LootTable> table, long seed) {
        if (!bounds.isInside(pos) || level.isOutsideBuildHeight(pos) || level.getBlockEntity(pos) != null) {
            return;
        }
        level.setBlock(pos, state, 2);
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container) {
            container.setLootTable(table, seed);
        }
    }

    private static ResourceKey<LootTable> lootTable(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("retold", path));
    }
}
