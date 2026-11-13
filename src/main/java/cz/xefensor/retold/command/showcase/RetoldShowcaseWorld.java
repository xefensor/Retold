package cz.xefensor.retold.command.showcase;

import cz.xefensor.retold.Retold;
import cz.xefensor.retold.api.world.RetoldWorldProtection;
import cz.xefensor.retold.registry.RetoldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/** One-shot developer gallery. All space and permissions are checked before the first edit. */
public final class RetoldShowcaseWorld {
    private static final Identifier SUBJECT = Identifier.fromNamespaceAndPath(Retold.MODID, "showcase");
    private static final int WIDTH = 66;
    private static final int HEIGHT = 10;
    private static final int ITEM_COLUMNS = 12;
    private static final int BLOCK_COLUMNS = 8;
    private static final int ENTITY_COLUMNS = 6;

    private RetoldShowcaseWorld() {
    }

    public static Plan plan(BlockPos origin) {
        List<Item> items = BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Retold.MODID))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
        List<Block> blocks = BuiltInRegistries.BLOCK.stream()
                .filter(block -> BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Retold.MODID))
                .sorted(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()))
                .toList();
        List<EntityType<?>> entities = BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Retold.MODID))
                .sorted(Comparator.comparing(type -> BuiltInRegistries.ENTITY_TYPE.getKey(type).toString()))
                .toList();
        int blockStart = 8 + Math.ceilDiv(items.size(), ITEM_COLUMNS) * 4 + 4;
        int entityStart = blockStart + Math.ceilDiv(blocks.size(), BLOCK_COLUMNS) * 7 + 6;
        int extras = entityStart + Math.ceilDiv(entities.size(), ENTITY_COLUMNS) * 12 + 4;
        return new Plan(origin.immutable(), items, blocks, entities, WIDTH, extras + 15,
                blockStart, entityStart, extras);
    }

    public static BuildResult build(ServerLevel level, Plan plan) {
        String rejection = validateSpace(level, plan);
        if (rejection != null) {
            return new BuildResult(false, rejection);
        }

        buildPlatform(level, plan);
        supplyChests(level, plan);
        for (int index = 0; index < plan.items().size(); index++) {
            itemExhibit(level, plan, index);
        }
        for (int index = 0; index < plan.blocks().size(); index++) {
            blockExhibit(level, plan, index);
        }
        for (int index = 0; index < plan.entities().size(); index++) {
            BlockPos pos = plan.entityPosition(index);
            put(level, pos.below(), Blocks.POLISHED_ANDESITE.defaultBlockState());
            label(level, pos.north(3).below(), "ENTITY", BuiltInRegistries.ENTITY_TYPE.getKey(plan.entities().get(index)));
            RetoldShowcaseEntities.spawn(level, plan.entities().get(index), Vec3.atBottomCenterOf(pos), plan.groupTag());
        }
        workshop(level, plan);
        return new BuildResult(true, "Created Retold test gallery: " + plan.items().size() + " items, "
                + plan.blocks().size() + " blocks, " + plan.entities().size() + " entity types, Steel armor and workstations.");
    }

    private static String validateSpace(ServerLevel level, Plan plan) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return "Create the gallery in an Overworld test world; Aender terrain can regenerate.";
        }
        BoundingBox box = plan.bounds();
        if (plan.depth() > 256 || box.minY() < level.getMinY() || box.maxY() >= level.getMaxY()) {
            return "The gallery does not fit inside the world build height or supported footprint.";
        }
        if (!level.getWorldBorder().isWithinBounds(new BlockPos(box.minX(), box.minY(), box.minZ()))
                || !level.getWorldBorder().isWithinBounds(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))) {
            return "The gallery would cross the world border.";
        }
        // Loading is bounded to this explicit command's footprint; no persistent tickets or jobs.
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
            }
        }
        for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ())) {
            if (!level.getBlockState(pos).isAir()) {
                return "Gallery space is occupied at " + pos.toShortString() + ". Choose an empty location.";
            }
            if (!RetoldWorldProtection.canWorldModify(level, pos, SUBJECT)) {
                return "World protection denied the gallery at " + pos.toShortString() + ".";
            }
        }
        for (int chunkX = box.minX() >> 4; chunkX <= box.maxX() >> 4; chunkX++) {
            for (int chunkZ = box.minZ() >> 4; chunkZ <= box.maxZ() >> 4; chunkZ++) {
                // Terrain loading can finish before saved entity sections. Never treat a hidden
                // section as empty or add displays which could overlap its returning entities.
                if (!level.areEntitiesActuallyLoadedAndTicking(new ChunkPos(chunkX, chunkZ))) {
                    return "The gallery area is not fully active yet. Stay near its center and retry /retold testworld shortly.";
                }
            }
        }
        if (!level.getEntities((Entity) null, new AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1), entity -> true).isEmpty()) {
            return "Gallery space contains entities. Choose an empty location.";
        }
        // The live portal has its own permission category in addition to ordinary world edits.
        int portalIndex = plan.blocks().indexOf(RetoldBlocks.AENDER_PORTAL.get());
        if (portalIndex >= 0) {
            BlockPos center = plan.blockPosition(portalIndex);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 0, 2))) {
                if (!RetoldWorldProtection.canCreatePortal(level, pos)) {
                    return "World protection denied the gallery's Aender portal.";
                }
            }
        }
        return null;
    }

    private static void buildPlatform(ServerLevel level, Plan plan) {
        BlockPos origin = plan.origin();
        for (int x = 0; x < plan.width(); x++) {
            for (int z = 0; z < plan.depth(); z++) {
                boolean edge = x == 0 || z == 0 || x == plan.width() - 1 || z == plan.depth() - 1;
                Block floor = edge ? Blocks.POLISHED_DEEPSLATE : Blocks.SMOOTH_STONE;
                if (x % 8 == 3 && z % 8 == 3) {
                    floor = Blocks.SEA_LANTERN;
                }
                put(level, origin.offset(x, 0, z), floor.defaultBlockState());
                if (edge) {
                    put(level, origin.offset(x, 1, z), Blocks.GLASS.defaultBlockState());
                }
            }
        }
        label(level, origin.offset(plan.width() / 2, 1, 4), "RETOLD TEST WORLD", null);
        label(level, origin.offset(3, 1, 6), "ITEMS / TAKE COPIES", null);
        label(level, origin.offset(3, 1, plan.blockStartZ() - 1), "BLOCKS", null);
        label(level, origin.offset(3, 1, plan.entityStartZ() - 1), "PASSIVE ENTITIES", null);
    }

    private static void supplyChests(ServerLevel level, Plan plan) {
        for (int index = 0; index < plan.items().size(); index += 27) {
            BlockPos pos = plan.supplyPosition(index / 27);
            put(level, pos, Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                nameChest(chest, "Retold items " + (index / 27 + 1));
                for (int slot = 0; slot < 27 && index + slot < plan.items().size(); slot++) {
                    ItemStack stack = plan.items().get(index + slot).getDefaultInstance();
                    stack.setCount(stack.getMaxStackSize());
                    chest.setItem(slot, stack);
                }
            }
            label(level, pos.north(), "SUPPLIES " + (index / 27 + 1), null);
        }
    }

    private static void itemExhibit(ServerLevel level, Plan plan, int index) {
        Item item = plan.items().get(index);
        BlockPos pos = plan.origin().offset(4 + index % ITEM_COLUMNS * 5, 2, 9 + index / ITEM_COLUMNS * 4);
        put(level, pos.below(), Blocks.QUARTZ_BLOCK.defaultBlockState());
        put(level, pos, Blocks.QUARTZ_BLOCK.defaultBlockState());
        ItemFrame frame = new ItemFrame(level, pos.north(), Direction.NORTH);
        frame.setItem(item.getDefaultInstance());
        frame.setInvulnerable(true);
        frame.setNoGravity(true);
        frame.setSilent(true);
        frame.addTag(plan.groupTag());
        frame.addTag(RetoldShowcaseEntities.EXHIBIT_TAG);
        level.addFreshEntity(frame);
        label(level, pos.north(2).below(), "ITEM", BuiltInRegistries.ITEM.getKey(item));
    }

    private static void blockExhibit(ServerLevel level, Plan plan, int index) {
        Block block = plan.blocks().get(index);
        BlockPos pos = plan.blockPosition(index);
        BlockState state = block.defaultBlockState();
        put(level, pos.below(), Blocks.QUARTZ_BLOCK.defaultBlockState());
        if (block == RetoldBlocks.AENDER_PORTAL.get()) {
            portal(level, pos);
        } else {
            if (block instanceof SaplingBlock) {
                put(level, pos.below(), RetoldBlocks.AENDER_SOIL.get().defaultBlockState());
                put(level, pos.above(2), Blocks.BARRIER.defaultBlockState());
            } else if (block == RetoldBlocks.AENDER_CACTUS.get()) {
                put(level, pos.below(), RetoldBlocks.AENDER_SAND.get().defaultBlockState());
            } else if (block instanceof WallTorchBlock || block instanceof WallSignBlock) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                put(level, pos.south(), Blocks.QUARTZ_BLOCK.defaultBlockState());
            } else if (block instanceof WallHangingSignBlock) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
                put(level, pos.east(), Blocks.QUARTZ_BLOCK.defaultBlockState());
                put(level, pos.west(), Blocks.QUARTZ_BLOCK.defaultBlockState());
            } else if (block instanceof CeilingHangingSignBlock) {
                put(level, pos.above(), Blocks.QUARTZ_BLOCK.defaultBlockState());
            } else if (block instanceof ButtonBlock) {
                state = state.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR);
            }
            put(level, pos, state);
            if (block instanceof DoorBlock) {
                put(level, pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            }
        }
        label(level, pos.north(3).below(), block == RetoldBlocks.AENDER_PORTAL.get() ? "LIVE PORTAL" : "BLOCK",
                BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void portal(ServerLevel level, BlockPos center) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    put(level, center.offset(x, 0, z), RetoldBlocks.DEV_AENDER_PORTAL_FRAME.get().defaultBlockState());
                }
            }
        }
    }

    private static void workshop(ServerLevel level, Plan plan) {
        BlockPos base = plan.origin().offset(4, 1, plan.extrasZ());
        label(level, base.north(2), "WORKSHOP / ARMOR", null);
        List<Block> stations = List.of(Blocks.CRAFTING_TABLE, Blocks.CAMPFIRE, Blocks.SMOKER,
                Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.ENCHANTING_TABLE, Blocks.ANVIL);
        for (int index = 0; index < stations.size(); index++) {
            Block block = stations.get(index);
            BlockState state = block.defaultBlockState();
            if (block == Blocks.CAMPFIRE) {
                state = state.setValue(BlockStateProperties.LIT, false);
            }
            put(level, base.offset(index * 5, 0, 2), state);
            label(level, base.offset(index * 5, 0, 1), "STATION", BuiltInRegistries.BLOCK.getKey(block));
        }
        RetoldShowcaseEntities.spawnSteelArmor(level, Vec3.atBottomCenterOf(base.offset(40, 0, 3)), false, plan.groupTag());
        RetoldShowcaseEntities.spawnSteelArmor(level, Vec3.atBottomCenterOf(base.offset(48, 0, 3)), true, plan.groupTag());
        BlockPos chestPos = base.offset(0, 0, 8);
        put(level, chestPos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            nameChest(chest, "Ritual and workshop supplies");
            List<Item> supplies = List.of(Items.DRAGON_EGG, Items.HEART_OF_THE_SEA, Items.HEAVY_CORE,
                    RetoldBlocks.NETHER_REACTOR_CORE.get(), Items.LODESTONE, Items.TOTEM_OF_UNDYING,
                    Items.NETHER_STAR, Items.LAPIS_LAZULI, Items.BOOK, Items.EMERALD, Items.FLINT,
                    Items.FLINT_AND_STEEL, Items.CLAY_BALL, Items.BRICK, Items.IRON_INGOT);
            for (int index = 0; index < supplies.size(); index++) {
                chest.setItem(index, supplies.get(index).getDefaultInstance());
            }
        }
        label(level, chestPos.north(), "RITUAL / WORKSHOP", null);
    }

    private static void nameChest(ChestBlockEntity chest, String name) {
        ItemStack namedChest = new ItemStack(Items.CHEST);
        namedChest.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        chest.applyComponentsFromItemStack(namedChest);
        chest.setChanged();
    }

    private static void label(ServerLevel level, BlockPos pos, String title, Identifier id) {
        put(level, pos, Blocks.OAK_SIGN.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
            SignText text = new SignText().setMessage(0, Component.literal(title));
            if (id != null) {
                text = text.setMessage(1, Component.literal(id.getNamespace() + ":"));
                String path = id.getPath();
                text = text.setMessage(2, Component.literal(path.substring(0, Math.min(15, path.length()))));
                if (path.length() > 15) {
                    text = text.setMessage(3, Component.literal(path.substring(15)));
                }
            }
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setWaxed(true);
        }
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    public record BuildResult(boolean created, String message) {
    }

    public record Plan(BlockPos origin, List<Item> items, List<Block> blocks, List<EntityType<?>> entities,
                       int width, int depth, int blockStartZ, int entityStartZ, int extrasZ) {
        public BoundingBox bounds() {
            return new BoundingBox(origin.getX(), origin.getY(), origin.getZ(),
                    origin.getX() + width - 1, origin.getY() + HEIGHT - 1, origin.getZ() + depth - 1);
        }

        public String groupTag() {
            return "retold.showcase." + origin.getX() + "." + origin.getY() + "." + origin.getZ();
        }

        public BlockPos blockPosition(int index) {
            return origin.offset(5 + index % BLOCK_COLUMNS * 8, 2, blockStartZ + 3 + index / BLOCK_COLUMNS * 7);
        }

        public BlockPos entityPosition(int index) {
            return origin.offset(6 + index % ENTITY_COLUMNS * 10, 2, entityStartZ + 4 + index / ENTITY_COLUMNS * 12);
        }

        public BlockPos supplyPosition(int index) {
            return origin.offset(3 + index * 3, 1, 3);
        }
    }
}
