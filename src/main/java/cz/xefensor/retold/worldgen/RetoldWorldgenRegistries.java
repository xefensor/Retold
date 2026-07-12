package cz.xefensor.retold.worldgen;

import com.mojang.serialization.MapCodec;
import cz.xefensor.retold.Retold;
import cz.xefensor.retold.worldgen.air.AirTemplePiece;
import cz.xefensor.retold.worldgen.air.AirTempleStructure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RetoldWorldgenRegistries {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Retold.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(BuiltInRegistries.STRUCTURE_TYPE, Retold.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, Retold.MODID);

    public static final DeferredHolder<
            MapCodec<? extends DensityFunction>,
            MapCodec<RetoldCentralEndIslandMaskDensityFunction>
            > CENTRAL_END_ISLAND_MASK =
            DENSITY_FUNCTION_TYPES.register(
                    "central_end_island_mask",
                    () -> RetoldCentralEndIslandMaskDensityFunction.CODEC
            );

    public static final DeferredHolder<
            StructureType<?>,
            StructureType<AirTempleStructure>
            > AIR_TEMPLE_STRUCTURE =
            STRUCTURE_TYPES.register(
                    "air_temple",
                    () -> () -> AirTempleStructure.CODEC
            );

    public static final DeferredHolder<
            StructurePieceType,
            StructurePieceType
            > AIR_TEMPLE_PIECE =
            STRUCTURE_PIECE_TYPES.register(
                    "air_temple",
                    () -> (StructurePieceType.ContextlessType) AirTemplePiece::new
            );

    private RetoldWorldgenRegistries() {
    }

    public static void register(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECE_TYPES.register(modEventBus);
    }
}
