package cz.xefensor.retold.worldgen.portal;

import cz.xefensor.retold.registry.RetoldTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/** Material-aware vanilla palette used by permanent Overworld portal drain. */
public final class RetoldNetherPortalDrainTransforms {
    private static final Map<Block, Block> CORAL_DEATH_TARGETS = Map.ofEntries(
            Map.entry(Blocks.TUBE_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK),
            Map.entry(Blocks.BRAIN_CORAL_BLOCK, Blocks.DEAD_BRAIN_CORAL_BLOCK),
            Map.entry(Blocks.BUBBLE_CORAL_BLOCK, Blocks.DEAD_BUBBLE_CORAL_BLOCK),
            Map.entry(Blocks.FIRE_CORAL_BLOCK, Blocks.DEAD_FIRE_CORAL_BLOCK),
            Map.entry(Blocks.HORN_CORAL_BLOCK, Blocks.DEAD_HORN_CORAL_BLOCK),
            Map.entry(Blocks.TUBE_CORAL, Blocks.DEAD_TUBE_CORAL),
            Map.entry(Blocks.BRAIN_CORAL, Blocks.DEAD_BRAIN_CORAL),
            Map.entry(Blocks.BUBBLE_CORAL, Blocks.DEAD_BUBBLE_CORAL),
            Map.entry(Blocks.FIRE_CORAL, Blocks.DEAD_FIRE_CORAL),
            Map.entry(Blocks.HORN_CORAL, Blocks.DEAD_HORN_CORAL),
            Map.entry(Blocks.TUBE_CORAL_FAN, Blocks.DEAD_TUBE_CORAL_FAN),
            Map.entry(Blocks.BRAIN_CORAL_FAN, Blocks.DEAD_BRAIN_CORAL_FAN),
            Map.entry(Blocks.BUBBLE_CORAL_FAN, Blocks.DEAD_BUBBLE_CORAL_FAN),
            Map.entry(Blocks.FIRE_CORAL_FAN, Blocks.DEAD_FIRE_CORAL_FAN),
            Map.entry(Blocks.HORN_CORAL_FAN, Blocks.DEAD_HORN_CORAL_FAN),
            Map.entry(
                    Blocks.TUBE_CORAL_WALL_FAN,
                    Blocks.DEAD_TUBE_CORAL_WALL_FAN
            ),
            Map.entry(
                    Blocks.BRAIN_CORAL_WALL_FAN,
                    Blocks.DEAD_BRAIN_CORAL_WALL_FAN
            ),
            Map.entry(
                    Blocks.BUBBLE_CORAL_WALL_FAN,
                    Blocks.DEAD_BUBBLE_CORAL_WALL_FAN
            ),
            Map.entry(
                    Blocks.FIRE_CORAL_WALL_FAN,
                    Blocks.DEAD_FIRE_CORAL_WALL_FAN
            ),
            Map.entry(
                    Blocks.HORN_CORAL_WALL_FAN,
                    Blocks.DEAD_HORN_CORAL_WALL_FAN
            )
    );

    private RetoldNetherPortalDrainTransforms() {
    }

    public static @Nullable BlockState resolveDrain(
            ServerLevel level,
            BlockPos pos,
            BlockState source
    ) {
        if (source.isAir()
                || source.is(RetoldTags.NETHER_PORTAL_DRAIN_IMMUNE)
                || source.hasBlockEntity()) {
            return null;
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_STABLE)) {
            return null;
        }

        BlockState death = resolveDeath(level, pos, source);
        if (death != null) {
            return death;
        }

        return null;
    }

    private static @Nullable BlockState resolveDeath(
            ServerLevel level,
            BlockPos pos,
            BlockState source
    ) {
        Block deadCoral = CORAL_DEATH_TARGETS.get(source.getBlock());
        if (deadCoral != null) {
            return copySharedProperties(source, deadCoral);
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_MELTS)
                || source.is(RetoldTags.NETHER_PORTAL_DRAIN_VEGETATION)) {
            return Blocks.AIR.defaultBlockState();
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_TO_DEAD_BUSH)) {
            if (source.getBlock() instanceof DoublePlantBlock) {
                return Blocks.AIR.defaultBlockState();
            }

            BlockState deadBush = Blocks.DEAD_BUSH.defaultBlockState();
            return deadBush.canSurvive(level, pos)
                    ? deadBush
                    : Blocks.AIR.defaultBlockState();
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_OUTER_GROUND)
                && !source.is(Blocks.COARSE_DIRT)) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }

        return null;
    }

    public static @Nullable BlockState resolveCorruption(
            ServerLevel level,
            BlockPos pos,
            BlockState source
    ) {
        if (source.isAir()
                || source.is(RetoldTags.NETHER_PORTAL_DRAIN_IMMUNE)
                || source.hasBlockEntity()) {
            return null;
        }

        boolean evaporatedWater = false;
        if (source.getFluidState().is(FluidTags.WATER)) {
            if (!source.hasProperty(BlockStateProperties.WATERLOGGED)) {
                return Blocks.AIR.defaultBlockState();
            }

            source = source.setValue(BlockStateProperties.WATERLOGGED, false);
            evaporatedWater = true;
        }

        if (!source.getFluidState().isEmpty()) {
            return null;
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_STABLE)) {
            return evaporatedWater ? source : null;
        }

        BlockState death = resolveDeath(level, pos, source);
        if (death != null) {
            return death;
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_TO_CRIMSON_WOOD)) {
            return copySharedProperties(source, crimsonWoodTarget(source));
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_TO_BLACKSTONE)) {
            return copySharedProperties(source, blackstoneTarget(source));
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_TO_GRAVEL)) {
            return Blocks.GRAVEL.defaultBlockState();
        }

        if (source.is(RetoldTags.NETHER_PORTAL_DRAIN_TO_NETHERRACK)) {
            return Blocks.NETHERRACK.defaultBlockState();
        }

        return evaporatedWater ? source : null;
    }

    private static Block crimsonWoodTarget(BlockState source) {
        Block block = source.getBlock();

        if (block instanceof RotatedPillarBlock) {
            String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
            boolean stripped = path.startsWith("stripped_");
            boolean barkOnEverySide = path.contains("wood")
                    || path.contains("hyphae");

            if (barkOnEverySide) {
                return stripped
                        ? Blocks.STRIPPED_CRIMSON_HYPHAE
                        : Blocks.CRIMSON_HYPHAE;
            }

            return stripped
                    ? Blocks.STRIPPED_CRIMSON_STEM
                    : Blocks.CRIMSON_STEM;
        }

        if (block instanceof StairBlock) {
            return Blocks.CRIMSON_STAIRS;
        }
        if (block instanceof SlabBlock) {
            return Blocks.CRIMSON_SLAB;
        }
        if (block instanceof FenceBlock) {
            return Blocks.CRIMSON_FENCE;
        }
        if (block instanceof FenceGateBlock) {
            return Blocks.CRIMSON_FENCE_GATE;
        }
        if (block instanceof DoorBlock) {
            return Blocks.CRIMSON_DOOR;
        }
        if (block instanceof TrapDoorBlock) {
            return Blocks.CRIMSON_TRAPDOOR;
        }
        if (block instanceof ButtonBlock) {
            return Blocks.CRIMSON_BUTTON;
        }
        if (block instanceof PressurePlateBlock) {
            return Blocks.CRIMSON_PRESSURE_PLATE;
        }

        return Blocks.CRIMSON_PLANKS;
    }

    private static Block blackstoneTarget(BlockState source) {
        Block block = source.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        boolean sandstone = path.contains("sandstone");
        boolean brickLike = path.contains("brick") || path.contains("tile");

        if (block instanceof StairBlock) {
            return brickLike
                    ? Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS
                    : Blocks.BLACKSTONE_STAIRS;
        }
        if (block instanceof SlabBlock) {
            return brickLike
                    ? Blocks.POLISHED_BLACKSTONE_BRICK_SLAB
                    : Blocks.BLACKSTONE_SLAB;
        }
        if (block instanceof WallBlock) {
            return brickLike
                    ? Blocks.POLISHED_BLACKSTONE_BRICK_WALL
                    : Blocks.BLACKSTONE_WALL;
        }
        if (block instanceof ButtonBlock) {
            return Blocks.POLISHED_BLACKSTONE_BUTTON;
        }
        if (block instanceof PressurePlateBlock) {
            return Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE;
        }
        if (path.contains("cracked")) {
            return Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS;
        }
        if (path.contains("chiseled") && !sandstone) {
            return Blocks.CHISELED_POLISHED_BLACKSTONE;
        }
        if (brickLike) {
            return Blocks.POLISHED_BLACKSTONE_BRICKS;
        }

        return Blocks.BLACKSTONE;
    }

    private static BlockState copySharedProperties(
            BlockState source,
            Block targetBlock
    ) {
        BlockState target = targetBlock.defaultBlockState();

        for (Property<?> property : source.getProperties()) {
            if (target.hasProperty(property)) {
                target = copyProperty(source, target, property);
            }
        }

        return target;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState source,
            BlockState target,
            Property<T> property
    ) {
        return target.setValue(property, source.getValue(property));
    }
}
